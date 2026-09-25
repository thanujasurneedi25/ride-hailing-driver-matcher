package edu.college.ridematcher.graph;

import edu.college.ridematcher.model.Location;
import java.util.*;

/** Undirected city graph stored as an adjacency list. BFS distance is a hop count in this academic prototype, not driving time; production routing needs a weighted road network. */
public final class CityGraph {
    private final Map<Location, List<Location>> adjacencyList = new LinkedHashMap<>();

    public void addZone(Location zone) { adjacencyList.computeIfAbsent(zone, ignored -> new ArrayList<>()); }
    public void connect(Location first, Location second) {
        if (first == null || second == null) throw new IllegalArgumentException("Zones are required.");
        addZone(first); addZone(second);
        if (!adjacencyList.get(first).contains(second)) adjacencyList.get(first).add(second);
        if (!adjacencyList.get(second).contains(first)) adjacencyList.get(second).add(first);
    }

    /** Breadth-first search visits nearer zones first and returns minimum edge counts. */
    public Map<Location, Integer> breadthFirstDistances(Location start) {
        if (!adjacencyList.containsKey(start)) return Collections.emptyMap();
        Map<Location, Integer> distances = new LinkedHashMap<>();
        Queue<Location> queue = new ArrayDeque<>();
        distances.put(start, 0); queue.add(start);
        while (!queue.isEmpty()) {
            Location current = queue.remove();
            for (Location neighbor : adjacencyList.get(current)) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, distances.get(current) + 1);
                    queue.add(neighbor);
                }
            }
        }
        return distances;
    }
}
