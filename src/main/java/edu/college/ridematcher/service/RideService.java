package edu.college.ridematcher.service;

import edu.college.ridematcher.matching.*;
import edu.college.ridematcher.model.*;
import java.util.ArrayList;
import java.util.List;

public final class RideService {
    private final MatchingEngine matchingEngine;
    private final List<Driver> drivers;
    public RideService(MatchingEngine matchingEngine, List<Driver> drivers) { this.matchingEngine = matchingEngine; this.drivers = new ArrayList<>(drivers); }
    public MatchResult requestRide(RideRequest request, List<String> trace) { return matchingEngine.findMatch(request, drivers, trace); }
}
