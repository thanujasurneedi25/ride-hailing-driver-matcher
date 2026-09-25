package edu.college.ridematcher.demo;

import edu.college.ridematcher.graph.CityGraph;
import edu.college.ridematcher.model.Driver;
import edu.college.ridematcher.model.Location;
import edu.college.ridematcher.model.Vehicle;
import edu.college.ridematcher.model.VehicleType;
import java.util.Arrays;
import java.util.List;

/** Shared simulated city and fleet used by both the console and web demonstrations. */
public final class DemoData {
    private DemoData() { }

    public static CityGraph createCityGraph() {
        CityGraph graph = new CityGraph();
        String[][] edges = {{"A","B"},{"A","C"},{"B","D"},{"B","E"},{"C","F"},{"D","G"},{"E","G"},{"F","G"},{"G","H"}};
        for (String[] edge : edges) graph.connect(new Location(edge[0]), new Location(edge[1]));
        return graph;
    }

    public static List<Driver> createDrivers() {
        return Arrays.asList(
            driver("D1", "Asha Rao", "B", true, VehicleType.CAR, 4, 4),
            driver("D2", "Ravi Kumar", "D", true, VehicleType.CAR, 8, 4),
            driver("D3", "Mina Shah", "C", false, VehicleType.CAR, 12, 4),
            driver("D4", "Kiran Das", "E", true, VehicleType.SUV, 2, 6),
            driver("D5", "Dev Nair", "F", true, VehicleType.CAR, 6, 4),
            driver("D6", "Nila Sen", "B", true, VehicleType.BIKE, 5, 1));
    }

    private static Driver driver(String id, String name, String zone, boolean available, VehicleType type, int wait, int capacity) {
        return new Driver(id, name, new Location(zone), available, new Vehicle(type, capacity), wait);
    }
}
