package tests;

import com.encore.encoreapi.event.Event;
import com.encore.encoreapi.event.EventRepository;
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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoTicketSeederTest {

    @Mock private EventRepository eventRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;

    @Test
    void createsClearlyNamedSampleInventoryOnlyInDemoMode() {
        Event event = new Event("event-1", "Concert", null, null, null, null, null, null);
        when(eventRepository.count()).thenReturn(1L);
        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(ticketTypeRepository.findByEventId(null)).thenReturn(List.of());

        DemoTicketSeeder seeder = new DemoTicketSeeder(eventRepository, ticketTypeRepository, true);
        seeder.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<TicketType> ticketTypes = ArgumentCaptor.forClass(TicketType.class);
        verify(ticketTypeRepository, times(2)).save(ticketTypes.capture());
        assertEquals(List.of("General (demo)", "VIP (demo)"),
                ticketTypes.getAllValues().stream().map(TicketType::getName).toList());
    }

    @Test
    void seedsSampleEventsAndInventoryWhenDemoDatabaseIsEmpty() {
        List<Event> persistedEvents = new ArrayList<>();
        when(eventRepository.count()).thenReturn(0L);
        when(eventRepository.findAll()).thenAnswer(invocation -> persistedEvents);
        when(eventRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<Event>>any()))
                .thenAnswer(invocation -> {
                    Iterable<Event> events = invocation.getArgument(0);
                    events.forEach(persistedEvents::add);
                    return persistedEvents;
                });
        when(ticketTypeRepository.findByEventId(null)).thenReturn(List.of());

        DemoTicketSeeder seeder = new DemoTicketSeeder(eventRepository, ticketTypeRepository, true);
        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(3, persistedEvents.size());
        verify(ticketTypeRepository, times(6)).save(any(TicketType.class));
    }

    @Test
    void createsNoSampleInventoryWhenDemoModeIsDisabled() {
        DemoTicketSeeder seeder = new DemoTicketSeeder(eventRepository, ticketTypeRepository, false);

        seeder.run(new DefaultApplicationArguments(new String[0]));

        verifyNoInteractions(eventRepository, ticketTypeRepository);
    }
}
