package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.AbstractPostgresTest;
import br.com.paymentsystem.demo.application.payment.command.CreatePaymentCommand;
import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;
import br.com.paymentsystem.demo.application.payment.usecase.CreatePaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class CreatePaymentUseCaseTest extends AbstractPostgresTest {

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    CreatePaymentUseCase useCase;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager em;

    @Test
    void should_create_payment_successfully() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "idem-001",
                new BigDecimal("100.00"),
                "BRL",
                """
                {"clientId":"123","origin":"API"}
                """
        );

        Payment payment = useCase.execute(command);
        em.flush();

        assertNotNull(payment.getId());

    }

    @Test
    void should_fail_on_duplicate_idempotency_key() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                "idem-dup",
                new BigDecimal("30.00"),
                "BRL",
                "{}"
        );

        // primeira transação
        transactionTemplate.execute(status -> {
            useCase.execute(command);
            return null;
        });

        // segunda transação (aqui o banco já conhece o primeiro registro)
        assertThrows(Exception.class, () -> {
            transactionTemplate.execute(status -> {
                useCase.execute(command);
                return null;
            });
        });
    }
}