package tests;

import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.event.EventRepository;
import com.encore.encoreapi.payment.PaymentMode;
import com.encore.encoreapi.ticket.DemoTicketSeeder;
import com.encore.encoreapi.ticket.TicketType;
import com.encore.encoreapi.ticket.TicketTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoTicketSeederTest {

    @Mock private EventRepository eventRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;

    @Test
    void createsClearlyNamedSampleInventoryOnlyInDemoMode() {
        Event event = new Event("event-1", "Concert", null, null, null, null, null, null);
        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(ticketTypeRepository.findByEventId(null)).thenReturn(List.of());

        DemoTicketSeeder seeder = new DemoTicketSeeder(eventRepository, ticketTypeRepository, new PaymentMode("true", ""));
        seeder.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<TicketType> ticketTypes = ArgumentCaptor.forClass(TicketType.class);
        verify(ticketTypeRepository, times(2)).save(ticketTypes.capture());
        assertEquals(List.of("General (demo)", "VIP (demo)"),
                ticketTypes.getAllValues().stream().map(TicketType::getName).toList());
    }

    @Test
    void doesNotCreateFictitiousEventsWhenCatalogIsEmpty() {
        when(eventRepository.findAll()).thenReturn(List.of());

        DemoTicketSeeder seeder = new DemoTicketSeeder(eventRepository, ticketTypeRepository, new PaymentMode("true", ""));
        seeder.run(new DefaultApplicationArguments(new String[0]));

        verify(eventRepository, never()).saveAll(any());
        verifyNoInteractions(ticketTypeRepository);
    }

    @Test
    void createsNoSampleInventoryWhenDemoModeIsDisabled() {
        DemoTicketSeeder seeder = new DemoTicketSeeder(eventRepository, ticketTypeRepository, new PaymentMode("false", "sk_test_key"));

        seeder.run(new DefaultApplicationArguments(new String[0]));

        verifyNoInteractions(eventRepository, ticketTypeRepository);
    }
}
