package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;
import br.com.paymentsystem.demo.application.payment.port.GatewayResult;
import br.com.paymentsystem.demo.application.payment.port.PaymentDataAnalyzer;
import br.com.paymentsystem.demo.application.payment.usecase.ProcessPaymentUseCase;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.exception.GatewayCommunicationException;
import br.com.paymentsystem.demo.exception.PaymentAlreadyBeingProcessException;
import br.com.paymentsystem.demo.infrastructure.dto.PaymentGatewayRequest;
import br.com.paymentsystem.demo.infrastructure.gateway.FakePaymentGateway;
import jakarta.persistence.EntityManager;
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
    PaymentDataAnalyzer paymentDataAnalyzer;

    @Mock
    FakePaymentGateway fakePaymentGateway;

    @Mock
    PaymentLeasePolicy paymentLeasePolicy;

    ProcessPaymentUseCase useCase;

    ActionOriginContext actionOriginContext;


    @BeforeEach
    void setup() {
        useCase = new ProcessPaymentUseCase(
                actionOriginContext,
                paymentRepository,
                fakePaymentGateway,
                paymentDataAnalyzer,
                paymentLeasePolicy
        );
    }

    /* ======================================================
       TESTE 1 — Só processa pagamentos em RECEIVED
       ====================================================== */
    @Test
    void should_not_process_when_lease_policy_denies_attempt() {

        Payment payment = Payment.create(
                "idem-blocked",
                new BigDecimal("100.00"),
                "BRL",
                """
                        {
                            "client-snapshot" : "snapshot"
                        }
                        """
        );

        when(paymentRepository.findByIdempotencyKey("idem-blocked"))
                .thenReturn(Optional.of(payment));


        assertThrows(
                PaymentAlreadyBeingProcessException.class,
                () -> useCase.execute("idem-blocked")
        );

        verifyNoInteractions(fakePaymentGateway);
    }


    /* ======================================================
       TESTE 2 — Gateway ERROR aborta processamento
       ====================================================== */
    @Test
    void should_throw_gateway_exception_when_gateway_errors() {

        Payment payment = Payment.create(
                "idem-error",
                new BigDecimal("50.00"),
                "BRL",
                """
                        {
                            "client-snapshot" : "snapshot"
                        }
                        """
        );

        when(paymentRepository.findByIdempotencyKey("idem-error"))
                .thenReturn(Optional.of(payment));


        when(paymentRepository.tryAcquireLease(any(), any()))
                .thenReturn(1);

        when(fakePaymentGateway.process(any(PaymentGatewayRequest.class)))
                .thenReturn(GatewayResult.ERROR);

        assertThrows(
                GatewayCommunicationException.class,
                () -> useCase.execute("idem-error")
        );

        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
    }


    /* ======================================================
       TESTE 3 — Gateway APPROVED não decide estado final
       ====================================================== */
    @Test
    void should_delegate_final_status_decision_to_analyzer() {

        Payment payment = Payment.create(
                "idem-approved",
                new BigDecimal("100.00"),
                "BRL",
                """
                        {
                            "client-snapshot" : "snapshot"
                        }
                        """
        );

        when(paymentRepository.findByIdempotencyKey("idem-approved"))
                .thenReturn(Optional.of(payment));


        when(paymentRepository.tryAcquireLease(any(), any()))
                .thenReturn(1);

        when(fakePaymentGateway.process(any(PaymentGatewayRequest.class)))
                .thenReturn(GatewayResult.APPROVED);

        useCase.execute("idem-approved");

        assertEquals(PaymentStatus.APPROVED, payment.getStatus());


    }

}
