package br.com.paymentsystem.demo.system;

import br.com.paymentsystem.demo.application.payment.port.GatewayResult;
import br.com.paymentsystem.demo.application.payment.port.PaymentDataAnalyzer;
import br.com.paymentsystem.demo.application.payment.port.PaymentGateway;
import br.com.paymentsystem.demo.domain.payment.PaymentStatus;
import br.com.paymentsystem.demo.infrastructure.dto.GatewayData;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class PaymentGatewayTestConfig {

    /**
     * Simula um gateway que NÃO responde.
     * Ou seja: processamento iniciado, mas nunca concluído.
     */
    @Bean
    public PaymentGateway paymentGateway() {
        PaymentGateway gateway = Mockito.mock(PaymentGateway.class);

        Mockito.when(gateway.process(Mockito.any()))
                .thenReturn(GatewayResult.PENDING);

        return gateway;
    }

    /**
     * Analyzer não pode decidir nada se o gateway não respondeu.
     * Ele apenas mantém o pagamento em PROCESSING.
     */
    @Bean
    public PaymentDataAnalyzer paymentDataAnalyzer() {
        PaymentDataAnalyzer analyzer = Mockito.mock(PaymentDataAnalyzer.class);

        Mockito.when(analyzer.dataAnalyzer(Mockito.any(GatewayData.class)))
                .thenReturn(PaymentStatus.PROCESSING);

        return analyzer;
    }
}
