package com.encore.encoreapi.ticket;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderExpirationService {

    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public OrderExpirationService(OrderRepository orderRepository, TicketTypeRepository ticketTypeRepository) {
        this.orderRepository = orderRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @Scheduled(fixedRate = 60000) // 60 sec
    public void expireOldOrders() {
        List<Order> pendingOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .filter(Order::isExpired)
                .toList();

        for (Order order : pendingOrders) {
            for (OrderItem item : order.getItems()) {
                TicketType ticketType = item.getTicketType();
                ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() + item.getQuantity());
                ticketTypeRepository.save(ticketType);
            }

            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            System.out.println("Orden expirada y stock liberado: " + order.getId());
        }
    }
}