package com.encore.encoreapi.payment;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);
    @Value("${stripe.api.key}")
    private String stripeApiKey;

    private final PaymentMode paymentMode;

    public StripeConfig(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
    }

    @PostConstruct
    public void init() {
        boolean demoMode = paymentMode.isDemoMode();
        if (stripeApiKey != null && !stripeApiKey.isBlank()) {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe payment integration initialized");
        } else if (!demoMode) {
            throw new IllegalStateException("STRIPE_API_KEY es obligatoria si los pagos demo están desactivados");
        } else {
            log.info("Stripe payment integration is disabled; demo payments are enabled");
        }
    }
}