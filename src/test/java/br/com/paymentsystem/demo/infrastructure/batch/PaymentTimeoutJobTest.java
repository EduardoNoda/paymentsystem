package br.com.paymentsystem.demo.infrastructure.batch;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentTimeoutJobTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    PaymentLeasePolicy leasePolicy;

    PaymentTimeoutJob job;

    @BeforeEach
    void setup() {
        job = new PaymentTimeoutJob(paymentRepository, leasePolicy);
    }

    /* ======================================================
       TESTE 1 — Não faz nada se não houver expirados
       ====================================================== */
    @Test
    void should_do_nothing_when_no_processing_payments_are_expired() {

        when(paymentRepository.findProcessingWithExpiredLease(any()))
                .thenReturn(List.of());

        job.execute();

        verify(paymentRepository).findProcessingWithExpiredLease(any());
        verifyNoMoreInteractions(paymentRepository);
        verifyNoInteractions(leasePolicy);
    }

    /* ======================================================
       TESTE 2 — Move PROCESSING → TO_ANALYZE
       ====================================================== */
    @Test
    void should_move_processing_to_to_analyze_when_lease_expired() {

        Payment payment = baseProcessingPaymentExpired();

        when(paymentRepository.findProcessingWithExpiredLease(any()))
                .thenReturn(List.of(payment));

        when(leasePolicy.calculateLeaseUntil(any(), eq(ActionOrigin.JOB)))
                .thenReturn(now().plusMinutes(5));

        job.execute();

        assertEquals(PaymentStatus.TO_ANALYZE, payment.getStatus());
    }

    /* ======================================================
       TESTE 3 — Define lease de 5 minutos
       ====================================================== */
    @Test
    void should_assign_job_lease_duration() {

        Payment payment = baseProcessingPaymentExpired();

        OffsetDateTime expectedLease = now().plusMinutes(5);

        when(paymentRepository.findProcessingWithExpiredLease(any()))
                .thenReturn(List.of(payment));

        when(leasePolicy.calculateLeaseUntil(any(), eq(ActionOrigin.JOB)))
                .thenReturn(expectedLease);

        job.execute();

        assertEquals(expectedLease, payment.getLeaseExpiresAt());
    }

    /* ======================================================
       TESTE 4 — Persiste pagamento atualizado
       ====================================================== */
    @Test
    void should_persist_updated_payment() {

        Payment payment = baseProcessingPaymentExpired();

        when(paymentRepository.findProcessingWithExpiredLease(any()))
                .thenReturn(List.of(payment));

        when(leasePolicy.calculateLeaseUntil(any(), eq(ActionOrigin.JOB)))
                .thenReturn(now().plusMinutes(5));

        job.execute();

        verify(paymentRepository).save(payment);
    }

    /* ======================================================
       TESTE 5 — Usa ActionOrigin.JOB
       ====================================================== */
    @Test
    void should_use_job_action_origin() {

        Payment payment = baseProcessingPaymentExpired();

        when(paymentRepository.findProcessingWithExpiredLease(any()))
                .thenReturn(List.of(payment));

        job.execute();

        verify(leasePolicy).calculateLeaseUntil(any(), eq(ActionOrigin.JOB));
    }

    /* ======================================================
       Utilitários
       ====================================================== */
    private Payment baseProcessingPaymentExpired() {

        Payment p = Payment.create(
                "idem-job",
                null,
                "BRL"
        );
        p.setStatus(PaymentStatus.PROCESSING);
        p.setLeaseExpiresAt(now().minusSeconds(10));
        return p;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
