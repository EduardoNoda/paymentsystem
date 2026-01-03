package br.com.paymentsystem.demo.application.payment.command;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
class JpaPaymentLockManager implements PaymentLockManager {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean tryAcquire(String key) {
        int updated = em.createNativeQuery("""
            update payments
               set lease_expires_at = now() + interval '30 seconds'
             where idempotency_key = :key
               and lease_expires_at is null
        """)
                .setParameter("key", key)
                .executeUpdate();

        return updated == 1;
    }
}
