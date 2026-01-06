package br.com.paymentsystem.demo.domain.payment;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Set;

public class PaymentLeasePolicy {

    private final Set<PaymentStatus> FINAL_STATE = EnumSet.of(
        PaymentStatus.APPROVED,
        PaymentStatus.RECUSED,
        PaymentStatus.FAIL,
        PaymentStatus.CANCEL_ADMIN
    );

    public boolean canAttemptProcessing(
            Payment payment,
            OffsetDateTime now,
            ActionOrigin origin
    ) {

        if(FINAL_STATE.contains(payment.getStatus())){
            return false;
        }

        if(payment.getStatus() == PaymentStatus.RECEIVED) {
            return true;
        }

        return isLeaseExpired(payment, now);
    }

    public Duration leaseDurationFor(ActionOrigin origin) {
        return switch (origin){
            case API -> Duration.ofSeconds(30);
            case JOB -> Duration.ofMinutes(5);
            case ADMIN -> Duration.ZERO;
        };
    }

    public OffsetDateTime calculateLeaseUntil (
            OffsetDateTime now,
            ActionOrigin origin
    ) {
        return now.plus(leaseDurationFor(origin));
    }

    public boolean isLeaseExpired(Payment payment, OffsetDateTime now) {
        OffsetDateTime leaseUntil = payment.getLeaseExpiresAt();
        return leaseUntil == null || leaseUntil.isBefore(now);
    }

    public boolean isLeaseState(PaymentStatus status) {
        return FINAL_STATE.contains(status);
    }
}