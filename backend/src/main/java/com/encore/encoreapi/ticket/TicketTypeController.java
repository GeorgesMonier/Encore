package com.encore.encoreapi.ticket;

import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.event.EventRepository;
import com.encore.encoreapi.payment.PaymentMode;
import com.encore.encoreapi.payment.StripeCurrency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    private final TicketPurchaseLimits purchaseLimits;

    public TicketTypeController(TicketTypeRepository ticketTypeRepository, EventRepository eventRepository,
                                PaymentMode paymentMode, TicketPurchaseLimits purchaseLimits) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.eventRepository = eventRepository;
        this.paymentMode = paymentMode;
        this.purchaseLimits = purchaseLimits;
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketTypeResponse>> getByEvent(@PathVariable UUID eventId) {
        List<TicketType> ticketTypes = ticketTypeRepository.findByEventId(eventId);
        if (!paymentMode.isDemoMode()) {
            ticketTypes = ticketTypes.stream().filter(ticketType -> !ticketType.isDemoTicket()).toList();
        }
        return ResponseEntity.ok(ticketTypes.stream()
                .map(ticketType -> TicketTypeResponse.from(ticketType, purchaseLimits))
                .toList());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateTicketTypeRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        TicketCategory category = request.getCategory() == null
                ? TicketCategory.fromLegacyName(request.getName())
                : request.getCategory();
        String currency = request.getCurrency() == null ? "EUR" : TicketType.normalizeCurrency(request.getCurrency());
        StripeCurrency.toMinorUnits(request.getPrice(), currency);
        TicketType ticketType = new TicketType(
                event, request.getName(), category, currency, request.getPrice(), request.getQuantity());
        ticketTypeRepository.save(ticketType);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketTypeResponse.from(ticketType, purchaseLimits));
    }

    public static class CreateTicketTypeRequest {
        @NotNull
        private UUID eventId;
        @NotBlank
        private String name;
        private TicketCategory category;
        @Pattern(regexp = "[A-Z]{3}")
        private String currency;
        @NotNull
        @DecimalMin("0.00")
        private BigDecimal price;
        @Min(1)
        private int quantity;

        public UUID getEventId() { return eventId; }
        public void setEventId(UUID eventId) { this.eventId = eventId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public TicketCategory getCategory() { return category; }
        public void setCategory(TicketCategory category) { this.category = category; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody UpdateTicketTypeRequest request) {
        if (request.getName() != null && request.getName().isBlank()) {
            return ResponseEntity.badRequest().body("El nombre del tipo de entrada no puede estar vacío");
        }
        TicketType ticketType = ticketTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de ticket no encontrado"));

        if (request.getName() != null) {
            ticketType.setName(request.getName());
        }
        String currency = request.getCurrency() == null ? ticketType.getCurrency() : TicketType.normalizeCurrency(request.getCurrency());
        BigDecimal price = request.getPrice() == null ? ticketType.getPrice() : request.getPrice();
        StripeCurrency.toMinorUnits(price, currency);
        if (request.getPrice() != null) {
            ticketType.setPrice(request.getPrice());
        }
        ticketType.setCurrency(currency);
        if (request.getTotalQuantity() != null) {
            try {
                ticketType.setTotalQuantity(request.getTotalQuantity());
            } catch (IllegalArgumentException exception) {
                return ResponseEntity.badRequest().body(exception.getMessage());
            }
        }
        if (request.getAvailableQuantity() != null) {
            try {
                ticketType.setAvailableQuantity(request.getAvailableQuantity());
            } catch (IllegalArgumentException exception) {
                return ResponseEntity.badRequest().body(exception.getMessage());
            }
        }

        ticketTypeRepository.save(ticketType);
        return ResponseEntity.ok(TicketTypeResponse.from(ticketType, purchaseLimits));
    }

    public static class UpdateTicketTypeRequest {
        private String name;
        @DecimalMin("0.00")
        private BigDecimal price;
        @Pattern(regexp = "[A-Z]{3}")
        private String currency;
        @Min(1)
        private Integer totalQuantity;
        @Min(0)
        private Integer availableQuantity;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public Integer getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
        public Integer getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    }
}