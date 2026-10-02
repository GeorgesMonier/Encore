package com.encore.encoreapi.payment;

import com.encore.encoreapi.ticket.Order;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {

    public PaymentIntent createPaymentIntent(Order order) throws StripeException {
        // Stripe trabaja en la unidad mínima de la moneda (céntimos para EUR)
        long amountInCents = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("eur")
                .putMetadata("orderId", order.getId().toString())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        return PaymentIntent.create(params);
    }
}