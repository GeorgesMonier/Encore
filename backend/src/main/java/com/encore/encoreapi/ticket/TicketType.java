package com.encore.encoreapi.ticket;

import com.encore.encoreapi.event.Event;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "ticket_types")
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String name; // "General", "VIP", etc.

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int availableQuantity;

    @Version
    private Long version; // para el bloqueo optimista

    public TicketType() {}

    public TicketType(Event event, String name, BigDecimal price, int totalQuantity) {
        this.event = event;
        this.name = name;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getSoldQuantity() { return totalQuantity - availableQuantity; }
    public boolean isDemoTicket() { return name.toLowerCase(Locale.ROOT).contains("(demo)"); }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    public Long getVersion() { return version; }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
}