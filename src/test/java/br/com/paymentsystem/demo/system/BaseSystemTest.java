package br.com.paymentsystem.demo.system;

import br.com.paymentsystem.demo.application.payment.port.PaymentGateway;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Testcontainers
public abstract class BaseSystemTest {

    @MockitoBean
    PaymentGateway paymentGateway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16")
                    .withDatabaseName("payment")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void overrideProps (DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.clean-on-validation-error", () -> true);
        registry.add("spring.flyway.clean-disabled", () -> false);
    }

    @Autowired
    private Flyway flyway;

    @BeforeEach
    public void clearData() {
        flyway.clean();
        flyway.migrate();
    }

    @AfterEach
    public void colectData(){
        jdbcTemplate.execute("UPDATE payment\n" +
                "SET lease_expires_at = now() + interval '30 seconds'\n" +
                "WHERE idempotency_key = 'X'\n" +
                "  AND (lease_expires_at IS NULL OR lease_expires_at < now());");
        jdbcTemplate.execute("SELECT lease_expires_at FROM payment;");
    }

    protected void runConcurrently(Runnable r1, Runnable r2) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Runnable wrapped1 = () -> {
            ready.countDown();
            await(start);
            r1.run();
        };

        Runnable wrapped2 = () -> {
            ready.countDown();
            await(start);
            r2.run();
        };

        try {
            executor.submit(wrapped1);
            executor.submit(wrapped2);

            // espera os dois estarem prontos
            ready.await();

            // solta os dois AO MESMO TEMPO
            start.countDown();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
