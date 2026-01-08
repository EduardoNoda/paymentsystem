package br.com.paymentsystem.demo.infrastructure.batch;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.domain.payment.Payment;
import br.com.paymentsystem.demo.domain.payment.PaymentLeasePolicy;
import br.com.paymentsystem.demo.domain.payment.PaymentRepository;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class PaymentTimeoutJob {

    private PaymentRepository paymentRepository;
    private PaymentLeasePolicy paymentLeasePolicy;

    public PaymentTimeoutJob(
            PaymentRepository paymentRepository,
            PaymentLeasePolicy paymentLeasePolicy
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentLeasePolicy = paymentLeasePolicy;
    }

    @Transactional
    @Scheduled(fixedDelay = 60_000)
    public void execute() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<Payment> expiredProcessingPayment = paymentRepository.findProcessingWithExpiredLease(now);

        for(Payment payment : expiredProcessingPayment) {

            payment.setStatus(PaymentStatus.TO_ANALYZE);

            OffsetDateTime leaseUntil = paymentLeasePolicy.calculateLeaseUntil(now, ActionOrigin.JOB);

            payment.setLeaseExpiresAt(leaseUntil);

            paymentRepository.save(payment);
        }

    }

}