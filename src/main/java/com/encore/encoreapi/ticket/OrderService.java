package com.encore.encoreapi.ticket;

import com.encore.encoreapi.user.User;
import com.encore.encoreapi.user.UserRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        TicketTypeRepository ticketTypeRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order createOrder(UUID userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Order order = new Order(user, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
            }

            TicketType ticketType = ticketTypeRepository.findById(itemReq.getTicketTypeId())
                    .orElseThrow(() -> new IllegalArgumentException("Tipo de ticket no encontrado"));

            if (ticketType.getAvailableQuantity() < itemReq.getQuantity()) {
                throw new IllegalArgumentException(
                        "No hay suficientes entradas disponibles para: " + ticketType.getName());
            }
            ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() - itemReq.getQuantity());
            try {
                ticketTypeRepository.saveAndFlush(ticketType);
            } catch (ObjectOptimisticLockingFailureException e) {
                throw new IllegalArgumentException(
                        "Las entradas para " + ticketType.getName() + " se agotaron mientras comprabas. Inténtalo de nuevo.");
            }

            BigDecimal subtotal = ticketType.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(subtotal);

            OrderItem orderItem = new OrderItem(ticketType, itemReq.getQuantity(), ticketType.getPrice());
            order.addItem(orderItem);
        }


        order.setTotalAmount(total);
        return orderRepository.save(order);
    }
    @Transactional
    public Order cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Solo se pueden cancelar órdenes PENDING o PAID");
        }

        for (OrderItem item : order.getItems()) {
            TicketType ticketType = item.getTicketType();
            ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() + item.getQuantity());
            ticketTypeRepository.save(ticketType);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }
}