package br.com.paymentsystem.demo.application.payment.usecase;

import br.com.paymentsystem.demo.application.payment.command.PaymentLockManager;
import br.com.paymentsystem.demo.application.payment.port.*;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.exception.GatewayCommunicationException;
import br.com.paymentsystem.demo.exception.PaymentAlreadyBeingProcessException;
import br.com.paymentsystem.demo.infrastructure.dto.PaymentGatewayRequest;
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
    private final PaymentLeasePolicy paymentLeasePolicy;

    public ProcessPaymentUseCase (
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            PaymentLockManager paymentLockManager,
            PaymentDataAnalyzer paymentDataAnalyzer,
            PaymentLeasePolicy paymentLeasePolicy
    ){
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.paymentLockManager = paymentLockManager;
        this.paymentDataAnalyzer = paymentDataAnalyzer;
        this.paymentLeasePolicy = paymentLeasePolicy;
    }

    @Transactional
    public void execute(String paymentId, ActionOrigin origin) {

        paymentLockManager.tryAcquire(paymentId);

        Payment payment = paymentRepository
                .findByIdempotencyKey(paymentId)
                .orElseThrow();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if(!paymentLeasePolicy.canAttemptProcessing(payment, now, origin)) {
            throw new PaymentAlreadyBeingProcessException(paymentId);
        }

        OffsetDateTime leaseUntil = paymentLeasePolicy.calculateLeaseUntil(now, origin);

        int updatedRows = paymentRepository.tryAcquireLease(
                paymentId,
                now,
                leaseUntil
        );

        if(updatedRows != 1){
            throw new PaymentAlreadyBeingProcessException(paymentId);
        }

        payment.setStatus(PaymentStatus.PROCESSING);

        PaymentGatewayRequest request = new PaymentGatewayRequest(
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getCurrency()
        );

        GatewayResult result = paymentGateway.process(request);

        if (result == GatewayResult.DECLINED) {
            return;
        }

        if (result == GatewayResult.ERROR) {
            throw new GatewayCommunicationException("Erro enquanto gateway processava pagamento "+ payment.getIdempotencyKey());
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