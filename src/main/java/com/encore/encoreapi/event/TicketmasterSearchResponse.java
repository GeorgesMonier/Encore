package com.encore.encoreapi.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketmasterSearchResponse {

    @JsonProperty("_embedded")
    private Embedded embedded;

    public Embedded get_embedded() { return embedded; }
    public void set_embedded(Embedded embedded) { this.embedded = embedded; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        private List<TicketmasterEventDto> events;
        public List<TicketmasterEventDto> getEvents() { return events; }
        public void setEvents(List<TicketmasterEventDto> events) { this.events = events; }
    }
}