package com.encore.encoreapi.event;

import com.encore.encoreapi.ticket.DemoTicketSeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class EventSyncService {

    private static final Logger log = LoggerFactory.getLogger(EventSyncService.class);
    private final TicketmasterClient ticketmasterClient;
    private final EventRepository eventRepository;
    private final DemoTicketSeeder demoTicketSeeder;

    public EventSyncService(TicketmasterClient ticketmasterClient, EventRepository eventRepository,
                            DemoTicketSeeder demoTicketSeeder) {
        this.ticketmasterClient = ticketmasterClient;
        this.eventRepository = eventRepository;
        this.demoTicketSeeder = demoTicketSeeder;
    }

    public int syncEvents(String city, String countryCode) {
        List<TicketmasterEventDto> events = ticketmasterClient.searchEvents(city, countryCode);
        log.info("Ticketmaster devolvió {} eventos para {}", events.size(), city);
        int savedCount = 0;

        for (TicketmasterEventDto dto : events) {
            if (eventRepository.existsByExternalId(dto.getId())) {
                eventRepository.findByExternalId(dto.getId()).ifPresent(event -> {
                    String eventTime = ticketmasterStartTime(dto);
                    if (!Objects.equals(event.getEventTime(), eventTime)) {
                        event.setEventTime(eventTime);
                        eventRepository.save(event);
                    }
                    demoTicketSeeder.ensureDemoTickets(event);
                });
                continue;
            }

            String venueName = null;
            String cityName = null;
            if (dto.get_embedded() != null && dto.get_embedded().getVenues() != null
                    && !dto.get_embedded().getVenues().isEmpty()) {
                venueName = dto.get_embedded().getVenues().get(0).getName();
                if (dto.get_embedded().getVenues().get(0).getCity() != null) {
                    cityName = dto.get_embedded().getVenues().get(0).getCity().getName();
                }
            }

            String artistName = null;
            if (dto.get_embedded() != null && dto.get_embedded().getAttractions() != null
                    && !dto.get_embedded().getAttractions().isEmpty()) {
                artistName = dto.get_embedded().getAttractions().get(0).getName();
            }

            String imageUrl = null;
            if (dto.getImages() != null && !dto.getImages().isEmpty()) {
                imageUrl = dto.getImages().get(0).getUrl();
            }

            LocalDateTime eventDate = null;
            if (dto.getDates() != null && dto.getDates().getStart() != null
                    && dto.getDates().getStart().getLocalDate() != null) {
                String dateStr = dto.getDates().getStart().getLocalDate();
                String timeStr = dto.getDates().getStart().getLocalTime() != null
                        ? dto.getDates().getStart().getLocalTime() : "00:00:00";
                eventDate = LocalDateTime.parse(dateStr + "T" + timeStr);
            }

            Event event = new Event(
                    dto.getId(),
                    dto.getName(),
                    artistName,
                    venueName,
                    cityName,
                    eventDate,
                    ticketmasterStartTime(dto),
                    imageUrl,
                    dto.getInfo()
            );

            Event savedEvent = eventRepository.save(event);
            demoTicketSeeder.ensureDemoTickets(savedEvent);
            savedCount++;
        }

        return savedCount;
    }

    private String ticketmasterStartTime(TicketmasterEventDto dto) {
        if (dto.getDates() == null || dto.getDates().getStart() == null) {
            return null;
        }
        return dto.getDates().getStart().getLocalTime();
    }

}