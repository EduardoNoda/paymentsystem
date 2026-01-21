package br.com.paymentsystem.demo.domain.payment;

import br.com.paymentsystem.demo.infrastructure.dto.GatewayData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String key);

    @Modifying
    @Query(value = """
                UPDATE payment 
                   SET lease_expires_at = current_timestamp + (:leaseSeconds || ' seconds')::interval,
                       status = 'PROCESSING'
                 WHERE idempotency_key = :key
                   AND (lease_expires_at IS NULL OR lease_expires_at < CURRENT_TIMESTAMP)
                   AND status IN (
                       'RECEIVED',
                       'TO_ANALYZE'
                   )
            """, nativeQuery = true)
    int tryAcquireLease(@Param("key") String paymentId, @Param("leaseSeconds") Long leaseSeconds);

    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.status = 'PROCESSING'
            AND p.leaseExpiresAt IS NOT NULL
            AND p.leaseExpiresAt < current_timestamp
            """)
    List<Payment> findProcessingWithExpiredLease();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE payment
               SET status = 'TO_ANALYZE',
                   lease_expires_at = NULL,
                   updated_at = now()
             WHERE idempotency_key = :key
               AND status = 'PROCESSING'
               AND lease_expires_at < current_timestamp
            """, nativeQuery = true)
    int forceToAnalyze(@Param("key") String key);

    @Query("""
             SELECT\s
                 p.idempotencyKey as idempotencyKey,\s
                 p.amount as amount,\s
                 p.currency as currency
             FROM Payment p
             WHERE p.idempotencyKey = :key
            \s""")
    GatewayData loadGatewayData(@Param("key") String key);

    @Modifying(clearAutomatically = true)
    @Query(value = """
                UPDATE payment
                   SET status = :status
                 WHERE idempotency_key = :key
                   AND status = 'PROCESSING'
            """, nativeQuery = true)
    int finalizePayment(@Param("key") String key, @Param("status") String status);

    @Modifying(clearAutomatically = true)
    @Query(value = """
                UPDATE payment
                   SET status = 'FAIL'
                 WHERE idempotency_key = :key
                   AND status = 'PROCESSING'
            """, nativeQuery = true)
    void failPayment(@Param("key") String key);

    @Modifying(clearAutomatically = true)
    @Query(value = """
                UPDATE payment
                   SET status = 'CANCEL_ADMIN'
                 WHERE idempotency_key = :key
            """, nativeQuery = true)
    void cancelPayment(@Param("key") String key);

    @Modifying(clearAutomatically = true)
    @Query(value = """
                UPDATE payment
                   SET status = 'PROCESSING'
                 WHERE idempotencyKey = :key
                   AND status = 'TO_ANALYZE'
            """, nativeQuery = true)
    void returnToProcessing(@Param("key") String key);

    @Modifying
    @Query(value = """
                UPDATE payment
                   SET lease_expires_at = current_timestamp - interval '1 seconds'
                 WHERE idempotency_key = :key
            """, nativeQuery = true)
    void forceExpireLease(
            @Param("key") String key
    );
    @Query("""
    SELECT p.idempotencyKey
    FROM Payment p
    WHERE p.status = 'TO_ANALYZE'
""")
    List<String> findPaymentsToRetry();

}