package tests;

import com.encore.encoreapi.event.*;
import com.encore.encoreapi.ticket.DemoTicketSeeder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSyncServiceTest {

    @Mock private TicketmasterClient ticketmasterClient;
    @Mock private EventRepository eventRepository;
    @Mock private DemoTicketSeeder demoTicketSeeder;

    @Test
    void preservesTheStartTimeProvidedByTicketmaster() {
        TicketmasterEventDto dto = event("21:30:00");
        when(ticketmasterClient.searchEvents("Madrid", "ES")).thenReturn(List.of(dto));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventSyncService service = new EventSyncService(ticketmasterClient, eventRepository, demoTicketSeeder);
        assertEquals(1, service.syncEvents("Madrid", "ES"));

        ArgumentCaptor<Event> savedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(savedEvent.capture());
        assertEquals("21:30:00", savedEvent.getValue().getEventTime());
    }

    @Test
    void leavesStartTimeAbsentWhenTicketmasterDoesNotProvideOne() {
        TicketmasterEventDto dto = event(null);
        when(ticketmasterClient.searchEvents("Madrid", "ES")).thenReturn(List.of(dto));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventSyncService service = new EventSyncService(ticketmasterClient, eventRepository, demoTicketSeeder);
        service.syncEvents("Madrid", "ES");

        ArgumentCaptor<Event> savedEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(savedEvent.capture());
        assertNull(savedEvent.getValue().getEventTime());
    }

    private TicketmasterEventDto event(String localTime) {
        TicketmasterEventDto dto = new TicketmasterEventDto();
        dto.setId("ticketmaster-event");
        dto.setName("Concierto");

        TicketmasterEventDto.Dates.Start start = new TicketmasterEventDto.Dates.Start();
        start.setLocalDate("2027-05-20");
        start.setLocalTime(localTime);
        TicketmasterEventDto.Dates dates = new TicketmasterEventDto.Dates();
        dates.setStart(start);
        dto.setDates(dates);
        return dto;
    }
}
