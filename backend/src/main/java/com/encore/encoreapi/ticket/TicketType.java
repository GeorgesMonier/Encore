package com.encore.encoreapi.ticket;

import com.encore.encoreapi.event.Event;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "ticket_types")
@Check(constraints = "total_quantity >= 0 and available_quantity >= 0 and sold_quantity >= 0 and reserved_quantity >= 0 and total_quantity = available_quantity + sold_quantity + reserved_quantity and category in ('NORMAL', 'VIP') and currency ~ '^[A-Z]{3}$'")
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TicketCategory category;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Column(nullable = false)
    private int soldQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Version
    private Long version; // para el bloqueo optimista

    public TicketType() {}

    public TicketType(Event event, String name, BigDecimal price, int totalQuantity) {
        this(event, name, TicketCategory.fromLegacyName(name), "EUR", price, totalQuantity);
    }

    public TicketType(Event event, String name, TicketCategory category, String currency,
                      BigDecimal price, int totalQuantity) {
        this.event = event;
        this.name = name;
        this.category = category;
        this.currency = normalizeCurrency(currency);
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
    }

    public static String normalizeCurrency(String currency) {
        String normalized = currency == null ? "EUR" : currency.trim();
        if (normalized.isEmpty()) {
            return "EUR";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public String getName() { return name; }
    public TicketCategory getCategory() { return category; }
    public String getCurrency() { return currency; }
    public BigDecimal getPrice() { return price; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getSoldQuantity() { return soldQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public boolean isDemoTicket() { return name.toLowerCase(java.util.Locale.ROOT).contains("(demo)"); }
    public void setAvailableQuantity(int availableQuantity) {
        if (availableQuantity < 0 || availableQuantity + reservedQuantity > totalQuantity) {
            throw new IllegalArgumentException("La disponibilidad debe respetar la capacidad comprometida");
        }
        this.availableQuantity = availableQuantity;
        this.soldQuantity = totalQuantity - availableQuantity - reservedQuantity;
    }
    public void setTotalQuantity(int totalQuantity) {
        if (totalQuantity < soldQuantity + reservedQuantity) {
            throw new IllegalArgumentException("La capacidad no puede ser menor que las entradas vendidas o reservadas");
        }
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity - soldQuantity - reservedQuantity;
    }
    public Long getVersion() { return version; }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setCurrency(String currency) { this.currency = normalizeCurrency(currency); }

    public void reserve(int quantity) {
        if (quantity < 1 || availableQuantity < quantity) {
            throw new IllegalArgumentException("No hay suficientes entradas disponibles para: " + name);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void confirmReservation(int quantity) {
        if (quantity < 1 || reservedQuantity < quantity) {
            throw new IllegalStateException("La reserva de inventario no coincide con la orden");
        }
        reservedQuantity -= quantity;
        soldQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        if (quantity < 1 || reservedQuantity < quantity) {
            throw new IllegalStateException("La reserva de inventario no coincide con la orden");
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    public void releaseSold(int quantity) {
        if (quantity < 1 || soldQuantity < quantity) {
            throw new IllegalStateException("Las entradas vendidas no coinciden con la orden");
        }
        soldQuantity -= quantity;
        availableQuantity += quantity;
    }
}