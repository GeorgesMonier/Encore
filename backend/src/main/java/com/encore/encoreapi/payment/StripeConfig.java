package com.encore.encoreapi.payment;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${payments.demo-mode:true}")
    private boolean demoMode;

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isBlank()) {
            Stripe.apiKey = stripeApiKey;
        } else if (!demoMode) {
            throw new IllegalStateException("STRIPE_API_KEY es obligatoria si los pagos demo están desactivados");
        }
    }
}