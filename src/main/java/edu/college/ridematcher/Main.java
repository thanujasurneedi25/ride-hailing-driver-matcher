package edu.college.ridematcher;

import edu.college.ridematcher.graph.CityGraph;
import edu.college.ridematcher.fare.FarePredictionService;
import java.io.IOException;
import java.nio.file.Paths;
import edu.college.ridematcher.matching.*;
import edu.college.ridematcher.model.*;
import edu.college.ridematcher.service.RideService;
import java.util.*;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        CityGraph graph = createCityGraph();
        CompatibilityRelation relation = new CompatibilityRelation();
        MatchingEngine engine = new MatchingEngine(graph, relation);
        List<Driver> sampleDrivers = Arrays.asList(
            driver("D1", "Asha", "B", true, VehicleType.CAR, 4),
            driver("D2", "Ravi", "D", true, VehicleType.CAR, 8),
            driver("D3", "Mina", "C", false, VehicleType.CAR, 12),
            driver("D4", "Kiran", "E", true, VehicleType.AUTO, 2),
            driver("D5", "Dev", "F", true, VehicleType.CAR, 6));
        RideService service = new RideService(engine, sampleDrivers);
        Rider rider = new Rider("R1", "Anu");
        RideRequest firstRequest = request(rider, "A", "H", VehicleType.CAR, 2);

        System.out.println("========================================");
        System.out.println("RIDE-HAILING DRIVER MATCHER");
        System.out.println("========================================");
        showRequest(firstRequest);
        System.out.println("Searching for compatible drivers...\n");
        List<String> trace = new ArrayList<>();
        MatchResult result = service.requestRide(firstRequest, trace);
        printResult(trace, result);
        if (result.isMatched()) estimateFare(firstRequest, graph, result.getDriver());
        System.out.println("\nSecond sample rider:");
        showRequest(request(new Rider("R2", "Bala"), "C", "G", VehicleType.AUTO, 1));
        System.out.println("\nRequested matching scenarios:");
        demonstrateScenarios(graph, relation);
    }

    private static CityGraph createCityGraph() {
        CityGraph graph = new CityGraph();
        String[][] edges = {{"A","B"},{"A","C"},{"B","D"},{"B","E"},{"C","F"},{"D","G"},{"E","G"},{"F","G"},{"G","H"}};
        for (String[] edge : edges) graph.connect(new Location(edge[0]), new Location(edge[1]));
        return graph;
    }
    private static Driver driver(String id, String name, String zone, boolean available, VehicleType type, int wait) {
        return new Driver(id, name, new Location(zone), available, new Vehicle(type, 4), wait);
    }
    private static RideRequest request(Rider rider, String pickup, String destination, VehicleType type, int passengers) {
        return new RideRequest(rider, new Location(pickup), new Location(destination), type, passengers);
    }
    private static void showRequest(RideRequest request) {
        System.out.println("Rider: " + request.getRider().getId() + " (" + request.getRider().getName() + ")");
        System.out.println("Pickup: " + request.getPickup() + "   Destination: " + request.getDestination());
        System.out.println("Vehicle required: " + request.getRequiredVehicleType() + "   Passengers: " + request.getPassengerCount());
    }
    private static void printResult(List<String> trace, MatchResult result) {
        System.out.println("BFS proximity search:");
        for (String line : trace) System.out.println(line);
        if (result.isMatched()) {
            Driver chosen = result.getDriver();
            System.out.println("\nSelected Driver:");
            System.out.println("ID: " + chosen.getId() + "\nName: " + chosen.getName());
            System.out.println("Vehicle: " + chosen.getVehicle() + "\nDistance: " + result.getDistance() + " zone/hop(s)");
        }
        System.out.println(result.getMessage());
    }
    private static void estimateFare(RideRequest request, CityGraph graph, Driver matchedDriver) {
        try {
            Integer routeHops = graph.breadthFirstDistances(request.getPickup()).get(request.getDestination());
            if (routeHops == null) {
                System.out.println("Fare estimate unavailable: destination is not reachable in the city graph.");
                return;
            }
            String python = System.getProperty("python.command", System.getenv("PYTHON_COMMAND"));
            if (python == null || python.trim().isEmpty()) python = "python";
            int fare = new FarePredictionService(python, Paths.get(".").toAbsolutePath().normalize()).estimateFare(request, routeHops);
            System.out.println("\n--- MATCH SUCCESSFUL ---");
            System.out.println("Driver: " + matchedDriver.getId() + " (" + matchedDriver.getName() + ")");
            System.out.println("Pickup: " + request.getPickup() + "   Destination: " + request.getDestination());
            System.out.println("Vehicle: " + matchedDriver.getVehicle().getType());
            System.out.println("Estimated Fare: ₹" + fare);
        } catch (IOException error) {
            System.out.println("Fare estimate unavailable: " + error.getMessage());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            System.out.println("Fare estimate cancelled while waiting for Python.");
        }
    }

    private static void demonstrateScenarios(CityGraph graph, CompatibilityRelation relation) {
        runCase("1. Compatible nearby driver exists", graph, relation,
            request(new Rider("T1", "Case One"), "A", "H", VehicleType.CAR, 2), Arrays.asList(driver("D1", "Near", "B", true, VehicleType.CAR, 1)));
        runCase("2. Nearest driver is unavailable", graph, relation,
            request(new Rider("T2", "Case Two"), "A", "H", VehicleType.CAR, 2), Arrays.asList(driver("D1", "Unavailable", "B", false, VehicleType.CAR, 1), driver("D2", "Farther", "D", true, VehicleType.CAR, 1)));
        runCase("3. Nearest driver is incompatible", graph, relation,
            request(new Rider("T3", "Case Three"), "A", "H", VehicleType.CAR, 2), Arrays.asList(driver("D1", "Wrong type", "B", true, VehicleType.AUTO, 1), driver("D2", "Farther", "D", true, VehicleType.CAR, 1)));
        runCase("4. Equal-distance drivers use waiting-time tie-break", graph, relation,
            request(new Rider("T4", "Case Four"), "A", "H", VehicleType.CAR, 2), Arrays.asList(driver("D2", "Longer wait", "B", true, VehicleType.CAR, 9), driver("D1", "Shorter wait", "C", true, VehicleType.CAR, 3)));
        runCase("5. No compatible driver exists", graph, relation,
            request(new Rider("T5", "Case Five"), "A", "H", VehicleType.CAR, 5), Arrays.asList(driver("D1", "Too small", "B", true, VehicleType.CAR, 1), driver("D2", "Unavailable", "C", false, VehicleType.CAR, 2)));
    }
    private static void runCase(String title, CityGraph graph, CompatibilityRelation relation, RideRequest request, List<Driver> drivers) {
        List<String> trace = new ArrayList<>();
        MatchResult result = new MatchingEngine(graph, relation).findMatch(request, drivers, trace);
        System.out.println("\n" + title);
        for (String line : trace) System.out.println("  " + line);
        System.out.println("  Result: " + (result.isMatched() ? result.getDriver().getId() + " selected at " + result.getDistance() + " hop(s)" : result.getMessage()));
    }
}



