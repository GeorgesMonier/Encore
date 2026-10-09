package com.encore.encoreapi.ticket;

import java.util.Locale;

public enum TicketCategory {
    NORMAL,
    VIP;

    public static TicketCategory fromLegacyName(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).contains("vip") ? VIP : NORMAL;
    }
}
