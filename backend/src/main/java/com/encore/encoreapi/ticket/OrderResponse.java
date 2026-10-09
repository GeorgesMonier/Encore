package com.encore.encoreapi.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        List<Item> items
) {
    public record Item(UUID ticketTypeId, String ticketTypeName, int quantity, BigDecimal unitPrice) {}

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(
                        i.getTicketType().getId(),
                        i.getTicketType().getName(),
                        i.getQuantity(),
                        i.getUnitPrice()))
                .toList();

        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), order.getCurrency(),
                order.getCreatedAt(), order.getExpiresAt(), items);
    }
}