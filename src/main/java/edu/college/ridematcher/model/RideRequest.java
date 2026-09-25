package edu.college.ridematcher.model;

public final class RideRequest {
    private final Rider rider;
    private final Location pickup;
    private final Location destination;
    private final VehicleType requiredVehicleType;
    private final int passengerCount;

    public RideRequest(Rider rider, Location pickup, Location destination, VehicleType requiredVehicleType, int passengerCount) {
        if (rider == null || pickup == null || destination == null || requiredVehicleType == null) throw new IllegalArgumentException("Ride request fields are required.");
        if (passengerCount < 1) throw new IllegalArgumentException("Passenger count must be positive.");
        this.rider = rider; this.pickup = pickup; this.destination = destination; this.requiredVehicleType = requiredVehicleType; this.passengerCount = passengerCount;
    }
    public Rider getRider() { return rider; }
    public Location getPickup() { return pickup; }
    public Location getDestination() { return destination; }
    public VehicleType getRequiredVehicleType() { return requiredVehicleType; }
    public int getPassengerCount() { return passengerCount; }
}
