package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.infrastructure.persistence.PostgresActionOriginContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class PaymentLeasePolicyTest {

    private PaymentLeasePolicy policy;

    private PostgresActionOriginContext postgresActionOriginContext;

    @BeforeEach
    void setup() {
        policy = new PaymentLeasePolicy(postgresActionOriginContext);
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
        });
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


    }


    /* ======================================================
       Utilitários
       ====================================================== */
    private Payment basePayment() {
        Payment p = Payment.create(
                "idem-test",
                null,
                "BRL",
                """
                        {
                            "client-snapshot" : "snapshot"
                        }
                        """
        );
        return p;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
