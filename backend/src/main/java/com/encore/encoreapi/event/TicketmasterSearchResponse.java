package com.encore.encoreapi.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketmasterSearchResponse {

    @JsonProperty("_embedded")
    private Embedded embedded;
    private Page page;

    public Embedded get_embedded() { return embedded; }
    public void set_embedded(Embedded embedded) { this.embedded = embedded; }
    public Page getPage() { return page; }
    public void setPage(Page page) { this.page = page; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Page {
        private Integer totalPages;
        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        private List<TicketmasterEventDto> events;
        public List<TicketmasterEventDto> getEvents() { return events; }
        public void setEvents(List<TicketmasterEventDto> events) { this.events = events; }
    }
}