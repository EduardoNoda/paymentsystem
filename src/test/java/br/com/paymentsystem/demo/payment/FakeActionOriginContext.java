package br.com.paymentsystem.demo.payment;

import br.com.paymentsystem.demo.application.payment.port.ActionOrigin;
import br.com.paymentsystem.demo.application.payment.port.ActionOriginContext;

class FakeActionOriginContext implements ActionOriginContext {

    private ActionOrigin lastOrigin;

    @Override
    public void set(ActionOrigin origin) {
        this.lastOrigin = origin;
    }

    ActionOrigin getLastOrigin() {
        return lastOrigin;
    }
}

