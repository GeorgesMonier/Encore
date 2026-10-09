package tests;

import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.ticket.Order;
import com.encore.encoreapi.ticket.TicketCategory;
import com.encore.encoreapi.ticket.TicketType;
import com.encore.encoreapi.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketTypeInventoryTest {

    @Test
    void tracksReservationSaleAndReleaseWithoutChangingCapacity() {
        TicketType ticketType = new TicketType(
                new Event("event-1", "Concert", null, null, null, null, null, null),
                "Entrada general", TicketCategory.NORMAL, "EUR", new BigDecimal("30.00"), 20);

        ticketType.reserve(4);
        assertEquals(16, ticketType.getAvailableQuantity());
        assertEquals(4, ticketType.getReservedQuantity());
        assertEquals(0, ticketType.getSoldQuantity());

        ticketType.confirmReservation(3);
        assertEquals(16, ticketType.getAvailableQuantity());
        assertEquals(1, ticketType.getReservedQuantity());
        assertEquals(3, ticketType.getSoldQuantity());

        ticketType.releaseReservation(1);
        ticketType.releaseSold(1);
        assertEquals(18, ticketType.getAvailableQuantity());
        assertEquals(0, ticketType.getReservedQuantity());
        assertEquals(2, ticketType.getSoldQuantity());
        assertEquals(20, ticketType.getTotalQuantity());
    }

    @Test
    void refusesReservationsBeyondAvailabilityAndCapacityReductionsBelowCommitments() {
        TicketType ticketType = new TicketType(
                new Event("event-1", "Concert", null, null, null, null, null, null),
                "VIP", TicketCategory.VIP, "EUR", new BigDecimal("100.00"), 5);
        ticketType.reserve(3);

        assertThrows(IllegalArgumentException.class, () -> ticketType.reserve(3));
        assertThrows(IllegalArgumentException.class, () -> ticketType.setTotalQuantity(2));
        assertEquals(3, ticketType.getReservedQuantity());
        assertEquals(2, ticketType.getAvailableQuantity());
    }

    @Test
    void normalizesWhitespaceAndCaseForLegacyCurrencyCodes() {
        TicketType ticketType = new TicketType(
                new Event("event-1", "Concert", null, null, null, null, null, null),
                "General", TicketCategory.NORMAL, " eur ", new BigDecimal("30.00"), 10);
        assertEquals("EUR", ticketType.getCurrency());

        Order order = new Order(new User("buyer@test.com", "hash", "Buyer"), new BigDecimal("30.00"));
        order.setCurrency(" usd ");
        assertEquals("USD", order.getCurrency());
        assertEquals(TicketCategory.VIP, TicketCategory.fromLegacyName("  vip gold  "));
    }
}
