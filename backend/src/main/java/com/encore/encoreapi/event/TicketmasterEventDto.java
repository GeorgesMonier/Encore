package com.encore.encoreapi.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketmasterEventDto {

    private String id;
    private String name;
    private String info;
    private Dates dates;
    private List<Image> images;

    @JsonProperty("_embedded")
    private Embedded embedded;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
    public Dates getDates() { return dates; }
    public void setDates(Dates dates) { this.dates = dates; }
    public List<Image> getImages() { return images; }
    public void setImages(List<Image> images) { this.images = images; }
    public Embedded get_embedded() { return embedded; }
    public void set_embedded(Embedded embedded) { this.embedded = embedded; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dates {
        private Start start;
        public Start getStart() { return start; }
        public void setStart(Start start) { this.start = start; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Start {
            private String localDate;
            private String localTime;
            public String getLocalDate() { return localDate; }
            public void setLocalDate(String localDate) { this.localDate = localDate; }
            public String getLocalTime() { return localTime; }
            public void setLocalTime(String localTime) { this.localTime = localTime; }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        private String url;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        private List<Venue> venues;
        private List<Attraction> attractions;
        public List<Venue> getVenues() { return venues; }
        public void setVenues(List<Venue> venues) { this.venues = venues; }
        public List<Attraction> getAttractions() { return attractions; }
        public void setAttractions(List<Attraction> attractions) { this.attractions = attractions; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Venue {
        private String name;
        private City city;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public City getCity() { return city; }
        public void setCity(City city) { this.city = city; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class City {
            private String name;
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attraction {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}