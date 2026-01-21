package br.com.paymentsystem.demo.infrastructure.batch;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.infrastructure.transaction.ActionOriginTransaction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentTimeoutJob {

    private final PaymentRepository paymentRepository;
    private final PaymentLeasePolicy paymentLeasePolicy;
    private final ActionOriginContext actionOriginContext;
    private final ActionOriginTransaction actionOriginTransaction;

    public PaymentTimeoutJob(
            PaymentRepository paymentRepository,
            PaymentLeasePolicy paymentLeasePolicy,
            ActionOriginContext actionOriginContext,
            ActionOriginTransaction actionOriginTransaction
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentLeasePolicy = paymentLeasePolicy;
        this.actionOriginContext = actionOriginContext;
        this.actionOriginTransaction = actionOriginTransaction;
    }

    @Scheduled(fixedDelay = 60_000)
    public void execute () {
        actionOriginContext.set(ActionOrigin.JOB);
        runJob();
    }

    void runJob() {


        List<Payment> expiredProcessingPayment =
                paymentRepository.findProcessingWithExpiredLease();

        for (Payment payment : expiredProcessingPayment) {

            int updateRows = paymentRepository.forceToAnalyze(
                    payment.getIdempotencyKey()
            );
        }
    }


}