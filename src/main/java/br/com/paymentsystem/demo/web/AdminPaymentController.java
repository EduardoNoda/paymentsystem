package br.com.paymentsystem.demo.web;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.usecase.AnalyzePaymentUseCase;
import br.com.paymentsystem.demo.application.payment.usecase.CancelPaymentAdminUseCase;
import br.com.paymentsystem.demo.infrastructure.dto.AnalyzePaymentRequest;
import br.com.paymentsystem.demo.infrastructure.transaction.ActionOriginTransaction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/controller")
public class AdminPaymentController {

    private AnalyzePaymentUseCase analyzePaymentUseCase;
    private CancelPaymentAdminUseCase cancelPaymentAdminUseCase;
    private ActionOriginTransaction actionOriginTransaction;

    @PostMapping("/{id}/analyze")
    public ResponseEntity<Void> analyze(@PathVariable String id, @RequestBody AnalyzePaymentRequest request) {
        actionOriginTransaction.run(ActionOrigin.ADMIN, () -> analyzePaymentUseCase.execute(id, request.status()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        cancelPaymentAdminUseCase.execute(id, ActionOrigin.ADMIN);
        return ResponseEntity.ok().build();
    }
}