package br.com.paymentsystem.demo.system;

import br.com.paymentsystem.demo.application.payment.command.CreatePaymentCommand;
import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.audit.PaymentAudit;
import br.com.paymentsystem.demo.domain.audit.PaymentAuditRepository;
import br.com.paymentsystem.demo.infrastructure.batch.PaymentRetryJob;
import br.com.paymentsystem.demo.infrastructure.batch.PaymentTimeoutJob;
import br.com.paymentsystem.demo.application.payment.usecase.CreatePaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.infrastructure.transaction.ActionOriginTransaction;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PaymentGatewayTestConfig.class)
class PaymentTimeoutJobSystemTest extends BaseSystemTest {

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Autowired
    private PaymentTimeoutJob paymentTimeoutJob;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private ActionOriginTransaction actionOriginTransaction;

    @Autowired
    private PaymentAuditRepository paymentAuditRepository;

    @Autowired
    private PaymentRetryJob paymentRetryJob;

    @Autowired
    private EntityManager entityManager;

    @Test
    void should_find_only_to_analyze_payments_for_retry() {

        // given
        createPaymentAndForceStatus("p1", PaymentStatus.TO_ANALYZE);
        createPaymentAndForceStatus("p2", PaymentStatus.PROCESSING);
        createPaymentAndForceStatus("p3", PaymentStatus.APPROVED);

        // when
        List<String> retryKeys =
                paymentRepository.findPaymentsToRetry();

        // then
        assertThat(retryKeys)
                .containsExactly("p1");
    }

    @Test
    void retry_job_should_process_to_analyze_payment() {

        String key = "retry-001";

        // given
        createPaymentWithStatus(key, PaymentStatus.TO_ANALYZE);

        // when
        actionOriginTransaction.run(ActionOrigin.JOB, () ->
                paymentRetryJob.execute()
        );

        // then
        Payment payment =
                paymentRepository.findByIdempotencyKey(key).orElseThrow();

        assertThat(payment.getStatus())
                .isIn(PaymentStatus.PROCESSING);
    }

    @Test
    void retry_job_should_be_safe_on_concurrent_execution() {

        String key = "retry-concurrent";

        createPaymentWithStatus(key, PaymentStatus.TO_ANALYZE);

        Runnable job = () ->
                actionOriginTransaction.run(ActionOrigin.JOB, () ->
                        paymentRetryJob.execute()
                );

        // when: dois jobs ao mesmo tempo
        runConcurrently(
                () -> actionOriginTransaction.run(ActionOrigin.JOB, paymentTimeoutJob::execute),
                () -> actionOriginTransaction.run(ActionOrigin.JOB, paymentTimeoutJob::execute)
        );


        // then
        Payment payment =
                paymentRepository.findByIdempotencyKey(key).orElseThrow();

        assertThat(payment.getStatus())
                .isNotEqualTo(PaymentStatus.TO_ANALYZE);
    }


    protected void createPaymentWithStatus(String key, PaymentStatus targetStatus) {

        CreatePaymentCommand command = new CreatePaymentCommand(
                key,
                new BigDecimal(200),
                "BRL",
                """
                {
                    "clientId": "123",
                    "paymentMethod": "CREDIT-CARD"
                }
                """
        );

        // RECEIVED
        actionOriginTransaction.run(ActionOrigin.API, () ->
                createPaymentUseCase.execute(command)
        );

        if (targetStatus == PaymentStatus.RECEIVED) {
            return;
        }

        // PROCESSING (API)
        actionOriginTransaction.run(ActionOrigin.API, () ->
                processPaymentUseCase.execute(key)
        );

        if (targetStatus == PaymentStatus.PROCESSING) {
            return;
        }

        if (targetStatus == PaymentStatus.TO_ANALYZE) {
            // força expiração de lease (JOB)
            actionOriginTransaction.run(ActionOrigin.JOB, () -> {
                entityManager.createNativeQuery("""
                UPDATE payment
                   SET lease_expires_at = now() - interval '10 minutes'
                 WHERE idempotency_key = :key
            """).setParameter("key", key).executeUpdate();
            });

            // executa timeout job
            actionOriginTransaction.run(ActionOrigin.JOB,
                    paymentTimeoutJob::execute
            );
        }
    }

    protected void createPaymentAndForceStatus(String key, PaymentStatus status) {

        CreatePaymentCommand command = new CreatePaymentCommand(
                key,
                new BigDecimal(200),
                "BRL",
                """
                {
                    "clientId": "123",
                    "paymentMethod": "CREDIT-CARD"
                }
                """
        );

        actionOriginTransaction.run(ActionOrigin.API,
                () -> createPaymentUseCase.execute(command)
        );

        if (status == PaymentStatus.RECEIVED) return;

        actionOriginTransaction.run(ActionOrigin.API,
                () -> processPaymentUseCase.execute(key)
        );

        if (status == PaymentStatus.PROCESSING) return;

        if (status == PaymentStatus.TO_ANALYZE) {
            actionOriginTransaction.run(ActionOrigin.JOB, () ->
                    entityManager.createNativeQuery("""
                UPDATE payment
                   SET status = 'TO_ANALYZE',
                       lease_expires_at = NULL
                 WHERE idempotency_key = :key
            """).setParameter("key", key).executeUpdate()
            );
        }
    }

    @Test
    void should_audit_status_change_with_action_origin() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "key",
                new BigDecimal(200),
                "BRL",
                """
                {
                    "clientId": "123",
                    "paymentMethod": "CREDIT-CARD"
                }
                """
        );

        // given
        actionOriginTransaction.run(ActionOrigin.API, () ->
                createPaymentUseCase.execute(command)
        );

        // when
        actionOriginTransaction.run(ActionOrigin.API, () ->
                processPaymentUseCase.execute(command.idempotencyKey())
        );

        Payment payment = paymentRepository.findByIdempotencyKey(command.idempotencyKey()).orElseThrow();

        // then
        List<PaymentAudit> audits =
                paymentAuditRepository.findByPaymentIdOrderByOccurredAtDesc(payment.getId());

        assertThat(audits).isNotEmpty();

        PaymentAudit audit = audits.get(0);

        assertThat(audit.getStatus())
                .isEqualTo(PaymentStatus.PROCESSING);

        assertThat(audit.getActionOrigin())
                .isEqualTo(ActionOrigin.API);

        assertThat(audit.getDescription())
                .contains("RECEIVED -> PROCESSING");

    }

}
