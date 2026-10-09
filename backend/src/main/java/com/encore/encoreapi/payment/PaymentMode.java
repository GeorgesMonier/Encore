package com.encore.encoreapi.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentMode {

    private final String configuredDemoMode;
    private final String stripeApiKey;

    public PaymentMode(@Value("${payments.demo-mode:}") String configuredDemoMode,
                       @Value("${stripe.api.key:}") String stripeApiKey) {
        this.configuredDemoMode = configuredDemoMode;
        this.stripeApiKey = stripeApiKey;
    }

    public boolean isDemoMode() {
        if (configuredDemoMode == null || configuredDemoMode.isBlank()) {
            return stripeApiKey == null || stripeApiKey.isBlank();
        }
        if ("true".equalsIgnoreCase(configuredDemoMode.strip())) {
            return true;
        }
        if ("false".equalsIgnoreCase(configuredDemoMode.strip())) {
            return false;
        }
        throw new IllegalStateException("PAYMENTS_DEMO_MODE debe ser true o false");
    }
}
