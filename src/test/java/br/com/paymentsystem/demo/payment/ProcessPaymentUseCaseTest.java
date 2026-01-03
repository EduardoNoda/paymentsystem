package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.application.payment.command.PaymentLockManager;
import br.com.paymentsystem.demo.application.payment.port.GatewayResult;
import br.com.paymentsystem.demo.application.payment.port.PaymentDataAnalyzer;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.exception.GatewayCommunicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentUseCaseTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    FakePaymentGateway fakePaymentGateway;

    @Mock
    PaymentDataAnalyzer paymentDataAnalyzer;

    @Mock
    PaymentLockManager paymentLockManager;

    ProcessPaymentUseCase useCase;

    @BeforeEach
    void setup() {
        useCase = new ProcessPaymentUseCase(
                paymentRepository,
                fakePaymentGateway,
                paymentLockManager,
                paymentDataAnalyzer
        );
    }

    /* ======================================================
       TESTE 1 — Só processa pagamentos em RECEIVED
       ====================================================== */
    @Test
    void should_reject_processing_if_payment_is_not_received() {

        Payment payment = Payment.create(
                "idem-not-received",
                new BigDecimal("100.00"),
                "BRL"
        );
        payment.setStatus(PaymentStatus.PROCESSING);

        when(paymentRepository.findByIdempotencyKey("idem-not-received"))
                .thenReturn(Optional.of(payment));

        GatewayCommunicationException ex = assertThrows(
                GatewayCommunicationException.class,
                () -> useCase.execute("idem-not-received")
        );

        assertTrue(ex.getMessage().contains("RECEIVED"));

        verifyNoInteractions(fakePaymentGateway);
        verify(paymentRepository, never()).save(any());
    }

    /* ======================================================
       TESTE 2 — Gateway ERROR aborta processamento
       ====================================================== */
    @Test
    void should_abort_processing_when_gateway_errors() {

        Payment payment = Payment.create(
                "idem-error",
                new BigDecimal("50.00"),
                "BRL"
        );
        payment.setStatus(PaymentStatus.RECEIVED);

        // payment encontrado
        when(paymentRepository.findByIdempotencyKey("idem-error"))
                .thenReturn(Optional.of(payment));

        // lock lógico sempre concedido
        when((paymentLockManager).tryAcquire("idem-error")).thenReturn(true);

        // lease físico concedido (updatedRows == 1)
        when(paymentRepository.tryAcquireLease(
                eq("idem-error"),
                any(),
                any()
        )).thenReturn(1);

        // gateway retorna erro técnico
        when(fakePaymentGateway.process(payment))
                .thenReturn(GatewayResult.ERROR);

        // execução deve abortar com IllegalStateException
        assertThrows(
                GatewayCommunicationException.class,
                () -> useCase.execute("idem-error")
        );

        // estado foi movido para PROCESSING antes do erro
        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());

        // lease foi atribuído
        //assertNotNull(payment.getLeaseExpiresAt());

        assertThrows(GatewayCommunicationException.class, () ->
            useCase.execute("idem-error")
        );

        // gateway foi chamado
        verify(fakePaymentGateway).process(payment);

        // nenhuma persistência final foi feita
        verify(paymentRepository, never()).save(any());
    }


    /* ======================================================
       TESTE 3 — Gateway APPROVED não decide estado final
       ====================================================== */
    @Test
    void should_call_gateway_and_keep_payment_processable() {

        Payment payment = Payment.create(
                "idem-approved",
                new BigDecimal("100.00"),
                "BRL"
        );
        payment.setStatus(PaymentStatus.RECEIVED);

        when(paymentRepository.findByIdempotencyKey("idem-approved"))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.tryAcquireLease(
                eq("idem-approved"),
                any(),
                any()
        )).thenReturn(1);

        when(fakePaymentGateway.process(payment))
                .thenReturn(GatewayResult.APPROVED);

        when(paymentDataAnalyzer.dataAnalyzer(payment))
                .thenReturn(PaymentStatus.RECEIVED);

        useCase.execute("idem-approved");

        // API NÃO decide estado final
        assertEquals(PaymentStatus.RECEIVED, payment.getStatus());

        verify(fakePaymentGateway).process(payment);
        verify(paymentRepository).findByIdempotencyKey("idem-approved");
        verify(paymentRepository).tryAcquireLease(eq("idem-approved"), any(), any());
        verify(fakePaymentGateway).process(payment);
        verify(paymentDataAnalyzer).dataAnalyzer(payment);

    }
}
