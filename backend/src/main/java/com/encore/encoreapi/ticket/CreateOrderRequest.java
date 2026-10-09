package com.encore.encoreapi.ticket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateOrderRequest {

    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public static class OrderItemRequest {
        @NotNull
        private java.util.UUID ticketTypeId;
        @Min(1)
        private int quantity;

        public java.util.UUID getTicketTypeId() { return ticketTypeId; }
        public void setTicketTypeId(java.util.UUID ticketTypeId) { this.ticketTypeId = ticketTypeId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}