package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PaymentLeasePolicyTest {

    private PaymentLeasePolicy policy;

    @BeforeEach
    void setup() {
        policy = new PaymentLeasePolicy();
    }

    /* ======================================================
       Estados finais nunca podem ser processados
       ====================================================== */
    @Test
    void should_not_allow_processing_for_final_states() {

        Payment payment = basePayment();
        OffsetDateTime now = now();

        for (PaymentStatus status : new PaymentStatus[]{
                PaymentStatus.APPROVED,
                PaymentStatus.RECUSED,
                PaymentStatus.FAIL,
                PaymentStatus.CANCEL_ADMIN
        }) {
            payment.setStatus(status);

            boolean allowed = policy.canAttemptProcessing(
                    payment,
                    now,
                    ActionOrigin.API
            );

            assertFalse(allowed, "Estado final não pode ser processado: " + status);
        }
    }

    /* ======================================================
       RECEIVED sempre pode ser processado
       ====================================================== */
    @Test
    void should_allow_processing_when_status_is_received() {

        Payment payment = basePayment();
        payment.setStatus(PaymentStatus.RECEIVED);

        boolean allowed = policy.canAttemptProcessing(
                payment,
                now(),
                ActionOrigin.API
        );

        assertTrue(allowed);
    }

    /* ======================================================
       PROCESSING com lease ativo NÃO pode
       ====================================================== */
    @Test
    void should_not_allow_processing_when_processing_and_lease_active() {

        Payment payment = basePayment();
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setLeaseExpiresAt(now().plusSeconds(30));

        boolean allowed = policy.canAttemptProcessing(
                payment,
                now(),
                ActionOrigin.API
        );

        assertFalse(allowed);
    }

    /* ======================================================
       PROCESSING com lease expirado PODE
       ====================================================== */
    @Test
    void should_allow_processing_when_processing_and_lease_expired() {

        Payment payment = basePayment();
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setLeaseExpiresAt(now().minusSeconds(1));

        boolean allowed = policy.canAttemptProcessing(
                payment,
                now(),
                ActionOrigin.API
        );

        assertTrue(allowed);
    }

    /* ======================================================
       TO_ANALYZE segue a mesma regra do lease
       ====================================================== */
    @Test
    void should_allow_processing_for_to_analyze_only_if_lease_expired() {

        Payment payment = basePayment();
        payment.setStatus(PaymentStatus.TO_ANALYZE);

        payment.setLeaseExpiresAt(now().plusSeconds(10));
        assertFalse(policy.canAttemptProcessing(payment, now(), ActionOrigin.JOB));

        payment.setLeaseExpiresAt(now().minusSeconds(1));
        assertTrue(policy.canAttemptProcessing(payment, now(), ActionOrigin.JOB));
    }

    /* ======================================================
       Lease duration por origem
       ====================================================== */
    @Test
    void should_define_correct_lease_duration_per_origin() {

        assertEquals(30, policy.leaseDurationFor(ActionOrigin.API).getSeconds());
        assertEquals(300, policy.leaseDurationFor(ActionOrigin.JOB).getSeconds());
        assertEquals(0, policy.leaseDurationFor(ActionOrigin.ADMIN).getSeconds());
    }

    /* ======================================================
       Cálculo do leaseUntil
       ====================================================== */
    @Test
    void should_calculate_lease_until_correctly() {

        OffsetDateTime now = now();

        OffsetDateTime apiLease = policy.calculateLeaseUntil(now, ActionOrigin.API);
        OffsetDateTime jobLease = policy.calculateLeaseUntil(now, ActionOrigin.JOB);

        assertEquals(now.plusSeconds(30), apiLease);
        assertEquals(now.plusMinutes(5), jobLease);
    }

    /* ======================================================
       Lease expirado quando null
       ====================================================== */
    @Test
    void should_consider_lease_expired_when_null() {

        Payment payment = basePayment();
        payment.setLeaseExpiresAt(null);

        assertTrue(policy.isLeaseExpired(payment, now()));
    }

    /* ======================================================
       Utilitários
       ====================================================== */
    private Payment basePayment() {
        Payment p = Payment.create(
                "idem-test",
                null,
                "BRL"
        );
        p.setStatus(PaymentStatus.RECEIVED);
        return p;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
