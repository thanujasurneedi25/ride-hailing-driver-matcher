package edu.college.ridematcher.model;

public final class Driver {
    private final String id;
    private final String name;
    private final Location location;
    private final boolean available;
    private final Vehicle vehicle;
    private final int waitingMinutes;

    public Driver(String id, String name, Location location, boolean available, Vehicle vehicle, int waitingMinutes) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Driver id and name are required.");
        if (location == null || vehicle == null) throw new IllegalArgumentException("Driver location and vehicle are required.");
        if (waitingMinutes < 0) throw new IllegalArgumentException("Waiting time cannot be negative.");
        this.id = id.trim(); this.name = name.trim(); this.location = location; this.available = available; this.vehicle = vehicle; this.waitingMinutes = waitingMinutes;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public boolean isAvailable() { return available; }
    public Vehicle getVehicle() { return vehicle; }
    public int getWaitingMinutes() { return waitingMinutes; }
}

