package edu.college.ridematcher.model;

import java.util.Objects;

/** A zone in the simulated city graph. */
public final class Location {
    private final String zone;

    public Location(String zone) {
        if (zone == null || zone.trim().isEmpty()) throw new IllegalArgumentException("Zone cannot be blank.");
        this.zone = zone.trim().toUpperCase();
    }

    public String getZone() { return zone; }
    @Override public String toString() { return zone; }
    @Override public boolean equals(Object other) { return other instanceof Location && zone.equals(((Location) other).zone); }
    @Override public int hashCode() { return Objects.hash(zone); }
}

