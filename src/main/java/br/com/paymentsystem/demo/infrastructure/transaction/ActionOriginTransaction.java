package br.com.paymentsystem.demo.infrastructure.transaction;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class ActionOriginTransaction {

    private final ActionOriginContext actionOriginContext;

    public ActionOriginTransaction (ActionOriginContext actionOriginContext) {
        this.actionOriginContext = actionOriginContext;
    }

    @Transactional
    public void run(ActionOrigin origin, Runnable action) {
        actionOriginContext.set(origin);
        action.run();
    }
}
