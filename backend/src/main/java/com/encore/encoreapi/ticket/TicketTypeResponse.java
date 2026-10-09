package com.encore.encoreapi.ticket;

import java.math.BigDecimal;
import java.util.UUID;

public record TicketTypeResponse(
        UUID id,
        UUID eventId,
        String name,
        TicketCategory category,
        BigDecimal price,
        String currency,
        int totalQuantity,
        int availableQuantity,
        int reservedQuantity,
        int soldQuantity,
        int userPurchaseLimit
) {
    public static TicketTypeResponse from(TicketType ticketType, TicketPurchaseLimits limits) {
        return new TicketTypeResponse(
                ticketType.getId(),
                ticketType.getEvent().getId(),
                ticketType.getName(),
                ticketType.getCategory(),
                ticketType.getPrice(),
                ticketType.getCurrency(),
                ticketType.getTotalQuantity(),
                ticketType.getAvailableQuantity(),
                ticketType.getReservedQuantity(),
                ticketType.getSoldQuantity(),
                limits.forCategory(ticketType.getCategory())
        );
    }
}
