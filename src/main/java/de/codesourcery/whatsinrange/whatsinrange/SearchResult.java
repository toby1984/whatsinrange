package de.codesourcery.whatsinrange.whatsinrange;

import java.time.Duration;
import java.time.ZonedDateTime;

public class SearchResult 
{
    public final String origin;
    public final String destination;
    public final Duration travelTime;
    public final ZonedDateTime createdOn = ZonedDateTime.now();
    
    public SearchResult(String origin, String destination, Duration travelTime) {
        this.origin = origin;
        this.destination = destination;
        this.travelTime = travelTime;
    }

    @Override
    public String toString() {
        return "SearchResult [origin=" + origin + ", destination=" + destination + ", travelTime=" + travelTime
                + ", createdOn=" + createdOn + "]";
    }
}