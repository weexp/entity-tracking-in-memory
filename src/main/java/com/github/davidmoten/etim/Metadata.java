package com.github.davidmoten.etim;

import java.util.Map;

public final class Metadata {

    private final String entityType;
    private final double lat;
    private final double lon;
    private final long time;
    private final Map<String, TimestampedString> properties;

    public Metadata(String type, double lat, double lon, long time,
            Map<String, TimestampedString> properties) {
        this.entityType = type;
        this.lat = lat;
        this.lon = lon;
        this.time = time;
        this.properties = properties;
    }
    
    public String type() {
        return entityType;
    }

    public double lat() {
        return lat;
    }

    public double lon() {
        return lon;
    }

    public long time() {
        return time;
    }

    public Map<String, TimestampedString> properties() {
        return properties;
    }

}
