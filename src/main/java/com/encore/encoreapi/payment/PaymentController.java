package com.encore.encoreapi.payment;

import com.encore.encoreapi.ticket.Order;
import com.encore.encoreapi.ticket.OrderRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    public PaymentController(PaymentService paymentService, OrderRepository orderRepository) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/create-intent/{orderId}")
    public ResponseEntity<?> createPaymentIntent(@PathVariable UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        try {
            PaymentIntent intent = paymentService.createPaymentIntent(order);
            return ResponseEntity.ok(Map.of("clientSecret", intent.getClientSecret()));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body("Error al crear el pago: " + e.getMessage());
        }
    }
}