package br.com.paymentsystem.demo.domain.payment;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.infrastructure.persistence.PostgresActionOriginContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

@Component
public class PaymentLeasePolicy {

    private final PostgresActionOriginContext postgresActionOriginContext;

    public PaymentLeasePolicy (PostgresActionOriginContext postgresActionOriginContext) {
        this.postgresActionOriginContext = postgresActionOriginContext;
    }

    private final Set<PaymentStatus> FINAL_STATE = EnumSet.of(
        PaymentStatus.APPROVED,
        PaymentStatus.RECUSED,
        PaymentStatus.FAIL,
        PaymentStatus.CANCEL_ADMIN
    );

    public Duration leaseDurationFor(ActionOrigin origin) {
        return switch (origin){
            case API -> Duration.ofSeconds(30);
            case JOB -> Duration.ofMinutes(5);
            case ADMIN -> Duration.ZERO;
        };
    }

}