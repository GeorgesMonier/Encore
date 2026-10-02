package com.encore.encoreapi.payment;

import com.encore.encoreapi.ticket.Order;
import com.encore.encoreapi.ticket.OrderRepository;
import com.encore.encoreapi.ticket.OrderStatus;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class WebhookController {

    private final OrderRepository orderRepository;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public WebhookController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Firma inválida");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (paymentIntent != null) {
                String orderIdStr = paymentIntent.getMetadata().get("orderId");
                if (orderIdStr != null) {
                    Order order = orderRepository.findById(UUID.fromString(orderIdStr)).orElse(null);
                    if (order != null) {
                        order.setStatus(OrderStatus.PAID);
                        orderRepository.save(order);
                    }
                }
            }
        }

        return ResponseEntity.ok("Recibido");
    }
}