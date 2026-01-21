package br.com.paymentsystem.demo.system;

import br.com.paymentsystem.demo.application.payment.command.CreatePaymentCommand;
import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.usecase.CreatePaymentUseCase;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.infrastructure.transaction.ActionOriginTransaction;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PaymentGatewayTestConfig.class)
class PaymentConcurrencySystemTest extends BaseSystemTest {

    @Autowired
    private CreatePaymentUseCase createPaymentUseCase;

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ActionOriginTransaction actionOriginTransaction;

    @Autowired
    private EntityManager entityManager;

    @Test
    void only_one_execution_should_acquire_lease() throws Exception {

        // arrange
        String idempotencyKey = "idem-concurrent-001";

        actionOriginTransaction.run(ActionOrigin.API, () ->
                createPaymentUseCase.execute(
                        new CreatePaymentCommand(
                                idempotencyKey,
                                new BigDecimal("500"),
                                "BRL",
                                """
                                {
                                  "clientId": "123",
                                  "paymentMethod": "CREDIT-CARD"
                                }
                                """
                        )
                )
        );

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        CyclicBarrier startBarrier = new CyclicBarrier(threads);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        List<Throwable> errors = new Vector<>();

        Runnable task = () -> {
            try {
                startBarrier.await();

                actionOriginTransaction.run(ActionOrigin.API, () ->
                        processPaymentUseCase.execute(idempotencyKey)
                );

            } catch (Throwable t) {
                errors.add(t);
            } finally {
                finishLatch.countDown();
            }
        };

        executor.submit(task);
        executor.submit(task);

        finishLatch.await();
        executor.shutdown();

        // assert
        Payment payment = paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow();

        assertThat(errors)
                .hasSize(1);

        assertThat(errors)
                .as("only one execution is allowed to fail due to lease contention")
                .hasSize(1);
    }
}

