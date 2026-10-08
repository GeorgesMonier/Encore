package com.encore.encoreapi.ticket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateOrderRequest {

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public static class OrderItemRequest {
        private java.util.UUID ticketTypeId;
        private int quantity;

        public java.util.UUID getTicketTypeId() { return ticketTypeId; }
        public void setTicketTypeId(java.util.UUID ticketTypeId) { this.ticketTypeId = ticketTypeId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}