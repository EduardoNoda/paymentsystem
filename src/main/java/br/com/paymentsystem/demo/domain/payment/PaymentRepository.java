package br.com.paymentsystem.demo.domain.payment;

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
    @Query("""
        update Payment p
           set p.leaseExpiresAt = :leaseUntil
         where p.idempotencyKey = :key
           and (p.leaseExpiresAt is null or p.leaseExpiresAt < :now)
    """)
    int tryAcquireLease(@Param("key")String paymentId, @Param("now") OffsetDateTime now, @Param("leaseUntil") OffsetDateTime ttl);
    @Modifying
    @Query("""
    update Payment p
       set p.status = :status
     where p.idempotencyKey = :key
""")
    int requestStatusTransaction(@Param("key") String paymentId, @Param("status") PaymentStatus nextStatus);

    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.status = 'PROCESSING'
            AND p.leaseExpiredAt IS NOT NULL
            AND p.leaseExpiredAt < :now
            """)
    List<Payment> findProcessingWithExpiredLease(@Param("now") OffsetDateTime now);
}