package com.encore.encoreapi.ticket;

import com.encore.encoreapi.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_user_idempotency_key", columnNames = {"user_id", "idempotency_key"})
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(unique = true)
    private String stripePaymentIntentId;

    @Column
    private UUID idempotencyKey;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public Order(User user, BigDecimal totalAmount) {
        this.user = user;
        this.status = OrderStatus.PENDING;
        this.totalAmount = totalAmount;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = normalizeCurrency(currency); }
    public static String normalizeCurrency(String currency) {
        String normalized = currency == null ? "EUR" : currency.trim();
        if (normalized.isEmpty()) {
            return "EUR";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getStripePaymentIntentId() { return stripePaymentIntentId; }
    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public List<OrderItem> getItems() { return items; }
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public boolean isExpired() {
        return !LocalDateTime.now().isBefore(expiresAt);
    }
}