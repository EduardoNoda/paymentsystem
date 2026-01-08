package br.com.paymentsystem.demo.domain.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentAuditRepository extends JpaRepository<PaymentAudit,Long> {

    List<PaymentAudit> findByPaymentIdOrderByOccurredAtDesc(Long id);

}
