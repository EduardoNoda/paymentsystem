package br.com.paymentsystem.demo.domain.audit;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "payment_audit")
public class PaymentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_origin", nullable = false)
    private ActionOrigin actionOrigin;

    @Column(name = "description")
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected PaymentAudit() {}

}
