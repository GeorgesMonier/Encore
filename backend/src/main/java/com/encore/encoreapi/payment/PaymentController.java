package com.encore.encoreapi.payment;

import com.encore.encoreapi.ticket.Order;
import com.encore.encoreapi.ticket.OrderRepository;
import com.encore.encoreapi.ticket.OrderService;
import com.encore.encoreapi.user.User;
import com.encore.encoreapi.user.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final boolean demoMode;

    public PaymentController(PaymentService paymentService,
                             OrderRepository orderRepository,
                             OrderService orderService,
                             UserRepository userRepository,
                             @Value("${payments.demo-mode:true}") boolean demoMode) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.demoMode = demoMode;
    }

    @GetMapping("/mode")
    public Map<String, Boolean> getPaymentMode() {
        return Map.of("demoMode", demoMode);
    }

    @PostMapping("/create-intent/{orderId}")
    public ResponseEntity<?> createPaymentIntent(@PathVariable UUID orderId) {
        if (demoMode) {
            return ResponseEntity.status(409).body("Los pagos de Stripe están desactivados en modo demostración");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));
        User user = authenticatedUser();
        if (!order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("No tienes permiso para pagar esta orden");
        }

        try {
            PaymentIntent intent = paymentService.createPaymentIntent(order, user);
            return ResponseEntity.ok(Map.of("clientSecret", intent.getClientSecret()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body("Error al crear el pago con Stripe");
        }
    }

    @PostMapping("/demo-confirm/{orderId}")
    public ResponseEntity<?> confirmDemoPayment(@PathVariable UUID orderId) {
        if (!demoMode) {
            return ResponseEntity.status(404).body("Los pagos de demostración están desactivados");
        }
        return ResponseEntity.ok(orderService.confirmDemoPayment(authenticatedUser().getId(), orderId));
    }

    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<?> confirmPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        if (demoMode) {
            return ResponseEntity.status(409).body("Stripe está desactivado en modo demostración");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));
        User user = authenticatedUser();
        if (!order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("No tienes permiso para confirmar esta orden");
        }

        try {
            paymentService.verifyPaymentIntent(order, user, request.paymentIntentId());
            return ResponseEntity.ok(orderService.markPaid(user.getId(), orderId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body("No se pudo verificar el pago con Stripe");
        }
    }

    @GetMapping("/payment-method")
    public ResponseEntity<?> getPaymentMethod() {
        try {
            return ResponseEntity.ok(paymentService.getDefaultPaymentMethod(authenticatedUser()));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body("No se pudo consultar el método de pago en Stripe");
        }
    }

    @PostMapping("/setup-intent")
    public ResponseEntity<?> createPaymentMethodSetup() {
        try {
            SetupIntent setupIntent = paymentService.createPaymentMethodSetup(authenticatedUser());
            return ResponseEntity.ok(Map.of("clientSecret", setupIntent.getClientSecret()));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body("No se pudo iniciar la verificación del método de pago en Stripe");
        }
    }

    @PostMapping("/payment-method")
    public ResponseEntity<?> savePaymentMethod(
            @Valid @RequestBody ConfirmPaymentMethodRequest request) {
        try {
            return ResponseEntity.ok(
                    paymentService.saveDefaultPaymentMethod(authenticatedUser(), request.setupIntentId()));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body("No se pudo guardar el método de pago en Stripe");
        }
    }

    private User authenticatedUser() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    public record ConfirmPaymentMethodRequest(@NotBlank String setupIntentId) {}
    public record ConfirmPaymentRequest(@NotBlank String paymentIntentId) {}
}