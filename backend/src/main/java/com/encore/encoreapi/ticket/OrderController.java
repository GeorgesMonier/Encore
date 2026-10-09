package com.encore.encoreapi.ticket;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Order order = orderService.createOrder(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
    @Transactional
    @GetMapping
    public ResponseEntity<List<OrderResponse>> myOrders() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<OrderResponse> orders = orderRepository.findByUserId(UUID.fromString(userId)).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @Transactional(readOnly = true)
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrder(@PathVariable UUID orderId) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));
        if (!order.getUser().getId().equals(UUID.fromString(userId))) {
            throw new AccessDeniedException("La orden no pertenece al usuario autenticado");
        }
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}