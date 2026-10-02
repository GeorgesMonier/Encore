package com.encore.encoreapi.ticket;

import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.event.EventRepository;
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

    public TicketTypeController(TicketTypeRepository ticketTypeRepository, EventRepository eventRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketType>> getByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ticketTypeRepository.findByEventId(eventId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateTicketTypeRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        TicketType ticketType = new TicketType(event, request.getName(), request.getPrice(), request.getQuantity());
        ticketTypeRepository.save(ticketType);

        return ResponseEntity.status(HttpStatus.CREATED).body(ticketType);
    }

    public static class CreateTicketTypeRequest {
        private UUID eventId;
        private String name;
        private BigDecimal price;
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
    public ResponseEntity<TicketType> update(@PathVariable UUID id, @RequestBody UpdateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ticket no encontrado"));

        if (request.getName() != null) {
            ticketType.setName(request.getName());
        }
        if (request.getPrice() != null) {
            ticketType.setPrice(request.getPrice());
        }
        if (request.getAvailableQuantity() != null) {
            ticketType.setAvailableQuantity(request.getAvailableQuantity());
        }

        ticketTypeRepository.save(ticketType);
        return ResponseEntity.ok(ticketType);
    }

    public static class UpdateTicketTypeRequest {
        private String name;
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