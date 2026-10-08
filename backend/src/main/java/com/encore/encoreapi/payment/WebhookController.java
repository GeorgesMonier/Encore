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
                String orderIdStr = paymentIntent.getMetadata() == null
                        ? null
                        : paymentIntent.getMetadata().get("orderId");
                if (orderIdStr != null) {
                    Order order = parseOrderId(orderIdStr);
                    if (order != null
                            && order.getStatus() == OrderStatus.PENDING
                            && !order.isExpired()
                            && paymentIntent.getAmount() != null
                            && paymentIntent.getAmount().equals(order.getTotalAmount()
                                    .movePointRight(2).longValueExact())
                            && "eur".equals(paymentIntent.getCurrency())
                            && order.getUser().getStripeCustomerId() != null
                            && order.getUser().getStripeCustomerId().equals(paymentIntent.getCustomer())) {
                        order.setStatus(OrderStatus.PAID);
                        orderRepository.save(order);
                    }
                }
            }
        }

        return ResponseEntity.ok("Recibido");
    }

    private Order parseOrderId(String orderId) {
        try {
            return orderRepository.findById(UUID.fromString(orderId)).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}