package com.encore.encoreapi.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final StripeWebhookService stripeWebhookService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public WebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook rejected because its signature is invalid");
            return ResponseEntity.badRequest().body("Firma inválida");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (paymentIntent == null) {
                log.error("Verified Stripe webhook contained an unreadable PaymentIntent");
                return ResponseEntity.badRequest().body("No se pudo leer el PaymentIntent");
            }
            stripeWebhookService.confirmSuccessfulPayment(paymentIntent);
        }

        return ResponseEntity.ok("Recibido");
    }
}