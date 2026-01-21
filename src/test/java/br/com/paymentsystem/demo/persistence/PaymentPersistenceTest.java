package br.com.paymentsystem.demo.persistence;

import br.com.paymentsystem.demo.AbstractPostgresTest;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class PaymentPersistenceTest extends AbstractPostgresTest {

    @Autowired
    private EntityManager em;


    @Test
    void should_insert_payment() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Payment payment = Payment.create("idem-123",
                new BigDecimal("100.00"),
                "BRL",
                """
                        {
                            "client-snapshot" : "snapshot"
                        }
                        """
                );

        em.persist(payment);
        em.flush(); // força o insert no banco

        assertNotNull(payment.getId());
    }

    @Test
    void should_fail_on_invalid_status() {
        assertThrows(PersistenceException.class, () -> {
            em.createNativeQuery("""
                        INSERT INTO payment (
                            idempotency_key, amount, currency, client_snapshot,
                            status, created_at, updated_at
                        )
                        VALUES (
                            'idem-invalid',
                            50.00,
                            'BRL',
                            '{"clientId":"123"}',
                            'HACKED',
                            now(),
                            now()
                        )
                    """).executeUpdate();
        });
    }

    @Test
    void should_fail_on_illegal_status_transition() {
        // cria pagamento válido
        Payment payment = Payment.create("idem-transitor",
                new BigDecimal(80.00),
                "BRL",
        """
                    {
                        "client-snapshot" : "snapshot"
                    }
                    """
        );

        em.persist(payment);
        em.flush();

        em.createNativeQuery("SET LOCAL app.action_origin = 'API'").executeUpdate();

        // tenta pular estado
        assertThrows(PersistenceException.class, () -> {
            em.createNativeQuery("""
                                UPDATE payment
                                SET status = 'APPROVED'
                                WHERE id = :id
                            """)
                    .setParameter("id", payment.getId())
                    .executeUpdate();
        });
    }

    @Test
    void java_should_not_block_invalid_state_change() {
        Payment payment = Payment.create("idem-java",
                new BigDecimal("30.00"),
                "BRL",
                """
                    {
                        "client-snapshot" : "snapshot"
                    }
                    """
        );

        em.persist(payment);
        em.flush();

        // Java permite
        PaymentStatus status = PaymentStatus.APPROVED;


        // Banco decide
        assertThrows(PersistenceException.class, () -> em.flush());
    }

    @Test
    void should_fail_on_duplicate_idempotency_key() {
        Payment firstPayment = Payment.create("idem-duplicated",
                new BigDecimal("50.00"),
                "BRL",
                """
                    {
                        "client-snapshot" : "snapshot"
                    }
                    """
        );

        em.persist(firstPayment);
        em.flush();

        Payment duplicatePayment = Payment.create("idem-duplicated",
                new BigDecimal(50.00),
                "BRL",
                """
                    {
                        "client-snapshot" : "snapshot"
                    }
                    """
        );

        em.persist(duplicatePayment);
        assertThrows(PersistenceException.class, () -> em.flush());
    }
}