package com.encore.encoreapi.event;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import com.encore.encoreapi.ticket.DemoTicketSeeder;

@Service
public class EventSyncService {

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
        System.out.println("Eventos recibidos de Ticketmaster: " + events.size());
        int savedCount = 0;

        for (TicketmasterEventDto dto : events) {
            if (eventRepository.existsByExternalId(dto.getId())) {
                eventRepository.findByExternalId(dto.getId()).ifPresent(demoTicketSeeder::ensureDemoTickets);
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
                    imageUrl,
                    dto.getInfo()
            );

            Event savedEvent = eventRepository.save(event);
            demoTicketSeeder.ensureDemoTickets(savedEvent);
            savedCount++;
        }

        return savedCount;
    }

}