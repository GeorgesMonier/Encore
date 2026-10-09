package com.encore.encoreapi.ticket;

import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.event.EventRepository;
import com.encore.encoreapi.payment.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ticket-types")
public class TicketTypeController {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final PaymentMode paymentMode;

    public TicketTypeController(TicketTypeRepository ticketTypeRepository, EventRepository eventRepository,
                                PaymentMode paymentMode) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventRepository = eventRepository;
        this.paymentMode = paymentMode;
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketType>> getByEvent(@PathVariable UUID eventId) {
        List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(eventId);
        if (!paymentMode.isDemoMode()) {
            ticketTypes = ticketTypes.stream().filter(ticketType -> !ticketType.isDemoTicket()).toList();
        }
        return ResponseEntity.ok(ticketTypes);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateTicketTypeRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        TicketType ticketType = new TicketType(event, request.getName(), request.getPrice(), request.getQuantity());
        ticketTypeRepository.save(ticketType);

        return ResponseEntity.status(HttpStatus.CREATED).body(ticketType);
    }

    public static class CreateTicketTypeRequest {
        @NotNull
        private UUID eventId;
        @NotBlank
        private String name;
        @NotNull
        @DecimalMin("0.00")
        private BigDecimal price;
        @Min(1)
        private int quantity;

        public UUID getEventId() { return eventId; }
        public void setEventId(UUID eventId) { this.eventId = eventId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    @PatchMapping("/{id}")
    public ResponseEntity<TicketType> update(@PathVariable UUID id, @Valid @RequestBody UpdateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ticket no encontrado"));

        if (request.getName() != null) {
            ticketType.setName(request.getName());
        }
        if (request.getPrice() != null) {
            ticketType.setPrice(request.getPrice());
        }
        if (request.getAvailableQuantity() != null) {
            if (request.getAvailableQuantity() < 0 || request.getAvailableQuantity() > ticketType.getTotalQuantity()) {
                return ResponseEntity.badRequest().build();
            }
            ticketType.setAvailableQuantity(request.getAvailableQuantity());
        }

        ticketTypeRepository.save(ticketType);
        return ResponseEntity.ok(ticketType);
    }

    public static class UpdateTicketTypeRequest {
        @NotBlank
        private String name;
        @DecimalMin("0.00")
        private BigDecimal price;
        private Integer availableQuantity;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    }
}