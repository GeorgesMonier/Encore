package com.encore.encoreapi.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StripeRefundService {

    private static final Logger log = LoggerFactory.getLogger(StripeRefundService.class);

    public void refundPaymentIntent(PaymentIntent paymentIntent) {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntent.getId())
                .build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("encore-refund-" + paymentIntent.getId())
                .build();
        try {
            Refund.create(params, options);
            log.info("Stripe refund submitted for a duplicate or non-payable Encore order");
        } catch (StripeException exception) {
            log.error("Stripe refund request failed ({})", exception.getClass().getSimpleName());
            throw new IllegalStateException("Stripe refund could not be created");
        }
    }
}
