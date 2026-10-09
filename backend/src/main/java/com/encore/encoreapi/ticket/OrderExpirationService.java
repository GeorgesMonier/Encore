package com.encore.encoreapi.ticket;

import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderExpirationService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationService.class);
    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public OrderExpirationService(OrderRepository orderRepository, TicketTypeRepository ticketTypeRepository) {
        this.orderRepository = orderRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @Scheduled(fixedRate = 60000) // 60 sec
    @Transactional
    public void expireOldOrders() {
        List<Order> pendingOrders = orderRepository.findExpiredOrdersForUpdate(
                OrderStatus.PENDING, LocalDateTime.now());

        for (Order order : pendingOrders) {
            for (OrderItem item : order.getItems()) {
                TicketType ticketType = item.getTicketType();
                ticketType.releaseReservation(item.getQuantity());
                ticketTypeRepository.save(ticketType);
            }

            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            log.info("Expired a pending order and released its reserved ticket inventory");
        }
    }
}