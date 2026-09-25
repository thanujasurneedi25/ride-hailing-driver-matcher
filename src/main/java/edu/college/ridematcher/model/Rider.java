package edu.college.ridematcher.model;

public final class Rider {
    private final String id;
    private final String name;
    public Rider(String id, String name) { this.id = require(id, "id"); this.name = require(name, "name"); }
    private static String require(String value, String label) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " cannot be blank."); return value.trim(); }
    public String getId() { return id; }
    public String getName() { return name; }
}

