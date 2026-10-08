package com.encore.encoreapi.admin;

import com.encore.encoreapi.ticket.OrderRepository;
import com.encore.encoreapi.ticket.OrderStatus;
import com.encore.encoreapi.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminStatsController(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    public record Stats(long totalUsers, long totalOrders, long paidOrders, BigDecimal totalRevenue) {}

    @Transactional
    @GetMapping
    public ResponseEntity<Stats> stats() {
        var orders = orderRepository.findAll();

        long paid = orders.stream().filter(o -> o.getStatus() == OrderStatus.PAID).count();
        BigDecimal revenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID)
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(new Stats(userRepository.count(), orders.size(), paid, revenue));
    }
}