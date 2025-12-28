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

}