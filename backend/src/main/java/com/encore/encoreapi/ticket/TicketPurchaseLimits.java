package com.encore.encoreapi.ticket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TicketPurchaseLimits {

    private final int normal;
    private final int vip;

    public TicketPurchaseLimits(
            @Value("${tickets.purchase-limits.normal:10}") int normal,
            @Value("${tickets.purchase-limits.vip:5}") int vip) {
        if (normal < 1 || vip < 1) {
            throw new IllegalArgumentException("Los límites de compra de entradas deben ser mayores que cero");
        }
        this.normal = normal;
        this.vip = vip;
    }

    public int forCategory(TicketCategory category) {
        return category == TicketCategory.VIP ? vip : normal;
    }

    public int getNormal() {
        return normal;
    }

    public int getVip() {
        return vip;
    }
}
