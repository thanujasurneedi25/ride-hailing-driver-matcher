package edu.college.ridematcher.matching;

import edu.college.ridematcher.model.*;

/** The rider-driver relation: available driver, exact requested type, and enough seats. */
public final class CompatibilityRelation {
    public String rejectionReason(RideRequest request, Driver driver) {
        if (!driver.isAvailable()) return "driver unavailable";
        if (driver.getVehicle().getType() != request.getRequiredVehicleType()) return "incompatible vehicle (requires " + request.getRequiredVehicleType() + ")";
        if (driver.getVehicle().getCapacity() < request.getPassengerCount()) return "vehicle capacity is insufficient";
        return null;
    }
    public boolean isCompatible(RideRequest request, Driver driver) { return rejectionReason(request, driver) == null; }
}
