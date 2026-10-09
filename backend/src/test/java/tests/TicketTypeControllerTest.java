package tests;

import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.event.EventRepository;
import com.encore.encoreapi.payment.PaymentMode;
import com.encore.encoreapi.ticket.TicketType;
import com.encore.encoreapi.ticket.TicketTypeController;
import com.encore.encoreapi.ticket.TicketTypeRepository;
import com.encore.encoreapi.ticket.TicketPurchaseLimits;
import com.encore.encoreapi.ticket.TicketCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketTypeControllerTest {

    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private EventRepository eventRepository;
    @Mock private PaymentMode paymentMode;

    @Test
    void returnsDatabaseAvailabilityAndHidesOldDemoInventoryInStripeMode() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event("concert-1", "Concert", null, null, null, null, null, null);
        TicketType general = new TicketType(event, "General", new BigDecimal("40.00"), 20);
        general.setAvailableQuantity(7);
        TicketType oldDemo = new TicketType(event, "VIP (demo)", new BigDecimal("90.00"), 10);
        when(ticketTypeRepository.findByEventId(eventId)).thenReturn(List.of(general, oldDemo));
        when(paymentMode.isDemoMode()).thenReturn(false);

        var result = new TicketTypeController(ticketTypeRepository, eventRepository, paymentMode,
                new TicketPurchaseLimits(10, 5))
                .getByEvent(eventId);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals(TicketCategory.NORMAL, result.getBody().get(0).category());
        assertEquals(13, result.getBody().get(0).soldQuantity());
        assertEquals(10, result.getBody().get(0).userPurchaseLimit());
        assertEquals("EUR", result.getBody().get(0).currency());
    }

    @Test
    void preservesDemoInventoryOnlyWhenDemoModeIsExplicitlyEnabled() {
        UUID eventId = UUID.randomUUID();
        List<TicketType> inventory = List.of(
                new TicketType(new Event("concert-1", "Concert", null, null, null, null, null, null),
                        "General (demo)", new BigDecimal("45.00"), 10));
        when(ticketTypeRepository.findByEventId(eventId)).thenReturn(inventory);
        when(paymentMode.isDemoMode()).thenReturn(true);

        var result = new TicketTypeController(ticketTypeRepository, eventRepository, paymentMode,
                new TicketPurchaseLimits(10, 5))
                .getByEvent(eventId);

        assertEquals(1, result.getBody().size());
        assertEquals(TicketCategory.NORMAL, result.getBody().get(0).category());
        assertEquals(0, result.getBody().get(0).reservedQuantity());
        assertEquals(10, result.getBody().get(0).availableQuantity());
    }
}
