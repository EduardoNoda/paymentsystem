package br.com.paymentsystem.demo.application.payment.usecase;

import br.com.paymentsystem.demo.application.payment.command.PaymentLockManager;
import br.com.paymentsystem.demo.application.payment.port.*;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.exception.GatewayCommunicationException;
import br.com.paymentsystem.demo.exception.PaymentAlreadyBeingProcessException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentLockManager paymentLockManager;
    private final PaymentDataAnalyzer paymentDataAnalyzer;

    public ProcessPaymentUseCase (
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            PaymentLockManager paymentLockManager,
            PaymentDataAnalyzer paymentDataAnalyzer
    ){
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.paymentLockManager = paymentLockManager;
        this.paymentDataAnalyzer = paymentDataAnalyzer;
    }

    @Transactional
    public void execute(String paymentId) {

        paymentLockManager.tryAcquire(paymentId);

        Payment payment = paymentRepository
                .findByIdempotencyKey(paymentId)
                .orElseThrow();

        if(payment.getStatus() != PaymentStatus.RECEIVED) {
            throw new GatewayCommunicationException(
                    "API só consegue processar pagamento no estado 'RECEIVED'"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime leaseUntil = now.plusSeconds(30);

        int updatedRows = paymentRepository.tryAcquireLease(
                paymentId,
                now,
                leaseUntil
        );

        boolean locked = updatedRows == 1;

        if(!locked) {
            throw new PaymentAlreadyBeingProcessException(paymentId);
        }

        payment.setStatus(PaymentStatus.PROCESSING);

        GatewayResult result = paymentGateway.process(payment);

        if (result == GatewayResult.DECLINED) {
            return;
        }

        if (result == GatewayResult.ERROR) {
            throw new GatewayCommunicationException("Erro enquanto gateway processava pagament "+ payment.getIdempotencyKey());
        }

        try {
            PaymentStatus finalStatus = paymentDataAnalyzer.dataAnalyzer(payment);
            payment.setStatus(finalStatus);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAIL);
            throw e;
        }
    }
}