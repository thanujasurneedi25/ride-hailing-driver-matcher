package edu.college.ridematcher.matching;

import edu.college.ridematcher.graph.CityGraph;
import edu.college.ridematcher.model.*;
import java.util.*;

/** Applies compatibility first, then deterministic BFS distance and waiting-time ordering. */
public final class MatchingEngine {
    private final CityGraph cityGraph;
    private final CompatibilityRelation compatibility;
    public MatchingEngine(CityGraph cityGraph, CompatibilityRelation compatibility) { this.cityGraph = cityGraph; this.compatibility = compatibility; }

    public MatchResult findMatch(RideRequest request, List<Driver> drivers, List<String> trace) {
        Map<Location, Integer> distances = cityGraph.breadthFirstDistances(request.getPickup());
        List<Driver> candidates = new ArrayList<>();
        for (Driver driver : drivers) {
            String reason = compatibility.rejectionReason(request, driver);
            if (reason != null) { trace.add(driver.getId() + " -> Rejected: " + reason); continue; }
            Integer distance = distances.get(driver.getLocation());
            if (distance == null) { trace.add(driver.getId() + " -> Rejected: zone is unreachable from pickup"); continue; }
            trace.add(driver.getId() + " -> distance " + distance);
            candidates.add(driver);
        }
        candidates.sort(Comparator.comparingInt((Driver d) -> distances.get(d.getLocation())).thenComparingInt(Driver::getWaitingMinutes).thenComparing(Driver::getId));
        if (candidates.isEmpty()) return MatchResult.failure("No compatible available driver was found.");
        Driver selected = candidates.get(0);
        return MatchResult.success(selected, distances.get(selected.getLocation()));
    }
}
