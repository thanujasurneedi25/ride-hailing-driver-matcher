package edu.college.ridematcher.model;

public final class Vehicle {
    private final VehicleType type;
    private final int capacity;

    public Vehicle(VehicleType type, int capacity) {
        if (type == null) throw new IllegalArgumentException("Vehicle type is required.");
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive.");
        this.type = type;
        this.capacity = capacity;
    }
    public VehicleType getType() { return type; }
    public int getCapacity() { return capacity; }
    @Override public String toString() { return type + " (capacity " + capacity + ")"; }
}
