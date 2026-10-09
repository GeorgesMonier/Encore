package tests;

import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.event.EventRepository;
import com.encore.encoreapi.payment.PaymentMode;
import com.encore.encoreapi.ticket.TicketType;
import com.encore.encoreapi.ticket.TicketTypeController;
import com.encore.encoreapi.ticket.TicketTypeRepository;
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

        var result = new TicketTypeController(ticketTypeRepository, eventRepository, paymentMode)
                .getByEvent(eventId);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(List.of(general), result.getBody());
        assertEquals(13, result.getBody().get(0).getSoldQuantity());
    }

    @Test
    void preservesDemoInventoryOnlyWhenDemoModeIsExplicitlyEnabled() {
        UUID eventId = UUID.randomUUID();
        List<TicketType> inventory = List.of(
                new TicketType(new Event("concert-1", "Concert", null, null, null, null, null, null),
                        "General (demo)", new BigDecimal("45.00"), 10));
        when(ticketTypeRepository.findByEventId(eventId)).thenReturn(inventory);
        when(paymentMode.isDemoMode()).thenReturn(true);

        var result = new TicketTypeController(ticketTypeRepository, eventRepository, paymentMode)
                .getByEvent(eventId);

        assertEquals(inventory, result.getBody());
    }
}
