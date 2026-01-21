package br.com.paymentsystem.demo.application.payment.port;

import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.infrastructure.dto.GatewayData;

public interface PaymentDataAnalyzer {

    PaymentStatus dataAnalyzer(GatewayData payment);

}