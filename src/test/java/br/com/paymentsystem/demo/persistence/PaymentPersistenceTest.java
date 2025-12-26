package br.com.paymentsystem.demo.persistence;

import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
public class PaymentPersistenceTest {
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16")
                    .withDatabaseName("payment_test")
                    .withUsername("pgsql")
                    .withPassword("pgsql");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EntityManager em;


    @Test
    void should_insert_payment() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Payment payment = new Payment();
        payment.setIdempotencyKey("idem-123");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("BRL");
        payment.setClientSnapshot("""
                    {"clientId": "123","origin":"API"}
                """);
        payment.setStatus(PaymentStatus.RECEIVED);
        payment.setCreatedAt(OffsetDateTime.now());
        payment.setUpdatedAt(OffsetDateTime.now());

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
        Payment payment = new Payment();
        payment.setIdempotencyKey("idem-transition");
        payment.setAmount(new BigDecimal("80.00"));
        payment.setCurrency("BRL");
        payment.setClientSnapshot("{}");
        payment.setStatus(PaymentStatus.RECEIVED);
        payment.setCreatedAt(OffsetDateTime.now());
        payment.setUpdatedAt(OffsetDateTime.now());

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
        Payment payment = new Payment();
        payment.setIdempotencyKey("idem-java");
        payment.setAmount(new BigDecimal("30.00"));
        payment.setCurrency("BRL");
        payment.setClientSnapshot("{}");
        payment.setStatus(PaymentStatus.RECEIVED);
        payment.setCreatedAt(OffsetDateTime.now());
        payment.setUpdatedAt(OffsetDateTime.now());

        em.persist(payment);
        em.flush();

        // Java permite
        payment.setStatus(PaymentStatus.APPROVED);

        // Banco decide
        assertThrows(PersistenceException.class, () -> em.flush());
    }
}