package br.com.paymentsystem.demo.infrastructure.persistence;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class PostgresActionOriginContext implements ActionOriginContext {

    private final EntityManager entityManager;

    public PostgresActionOriginContext (EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void set(ActionOrigin origin) {
        entityManager
                .createNativeQuery("SET LOCAL app.action_origin = '" + origin.name() + "'")
                .executeUpdate();
    }

    @Override
    public ActionOrigin get() {
        String origin = (String) entityManager
                .createNativeQuery("SELECT current_setting('app.action_origin', true)")
                .getSingleResult();

        if (origin == null) {
            throw new IllegalStateException("Action origin not set");
        }

        return ActionOrigin.valueOf(origin);
    }

}