package com.encore.encoreapi.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class TicketmasterClient {

    private static final int PAGE_SIZE = 200;
    private final RestClient restClient;

    @Value("${ticketmaster.api.key}")
    private String apiKey;

    public TicketmasterClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://app.ticketmaster.com/discovery/v2")
                .build();
    }

    public List<TicketmasterEventDto> searchEvents(String city, String countryCode) {
        List<TicketmasterEventDto> events = new ArrayList<>();
        int page = 0;
        int totalPages;

        do {
            TicketmasterSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/events.json")
                            .queryParam("apikey", apiKey)
                            .queryParam("city", city)
                            .queryParam("countryCode", countryCode)
                            .queryParam("classificationName", "music")
                            .queryParam("size", PAGE_SIZE)
                            .queryParam("page", page)
                            .build())
                    .retrieve()
                    .body(TicketmasterSearchResponse.class);

            if (response == null) {
                break;
            }
            if (response.get_embedded() != null && response.get_embedded().getEvents() != null) {
                events.addAll(response.get_embedded().getEvents());
            }

            totalPages = response.getPage() != null && response.getPage().getTotalPages() != null
                    ? response.getPage().getTotalPages() : page + 1;
            page++;
        } while (page < totalPages);

        return events;
    }
}