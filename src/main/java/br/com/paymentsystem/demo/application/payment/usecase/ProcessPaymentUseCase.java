package br.com.paymentsystem.demo.application.payment.usecase;

import br.com.paymentsystem.demo.application.payment.port.*;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.exception.PaymentAlreadyBeingProcessException;
import br.com.paymentsystem.demo.infrastructure.dto.GatewayData;
import br.com.paymentsystem.demo.infrastructure.dto.PaymentGatewayRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ProcessPaymentUseCase {

    private final ActionOriginContext actionOriginContext;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentDataAnalyzer paymentDataAnalyzer;
    private final PaymentLeasePolicy paymentLeasePolicy;

    public ProcessPaymentUseCase (
            ActionOriginContext actionOriginContext,
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            PaymentDataAnalyzer paymentDataAnalyzer,
            PaymentLeasePolicy paymentLeasePolicy
    ){
        this.actionOriginContext = actionOriginContext;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.paymentDataAnalyzer = paymentDataAnalyzer;
        this.paymentLeasePolicy = paymentLeasePolicy;
    }

    public void execute(String paymentId) {

        Duration leaseSeconds = paymentLeasePolicy.leaseDurationFor(actionOriginContext.get());

        int updatedRows = paymentRepository.tryAcquireLease(
                paymentId,
                leaseSeconds.getSeconds()
        );

        if(updatedRows != 1){
            throw new PaymentAlreadyBeingProcessException(paymentId);
        }

        GatewayData data = paymentRepository.loadGatewayData(paymentId);

        PaymentGatewayRequest request = new PaymentGatewayRequest(
                data.getIdempotencyKey(),
                data.getAmount(),
                data.getCurrency()
        );


        GatewayResult result = paymentGateway.process(request);

        if (result == GatewayResult.DECLINED) {
            paymentRepository.failPayment(paymentId);
            return;
        }

        PaymentStatus finalStatus = paymentDataAnalyzer.dataAnalyzer(data);

        int finalized = paymentRepository.finalizePayment(
                paymentId,
                finalStatus.name()
        );

        if (finalized != 1) {
            throw new IllegalStateException("Finalização concorrente detectada");
        }
    }
}