package com.encore.encoreapi.ticket;

import java.util.Locale;

public enum TicketCategory {
    NORMAL,
    VIP;

    public static TicketCategory fromLegacyName(String name) {
        if (name == null) {
            return NORMAL;
        }
        String normalizedName = name.trim();
        return normalizedName.toLowerCase(Locale.ROOT).contains("vip") ? VIP : NORMAL;
    }
}
