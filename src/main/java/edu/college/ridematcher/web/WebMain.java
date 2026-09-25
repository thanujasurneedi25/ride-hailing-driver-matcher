package edu.college.ridematcher.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.college.ridematcher.fare.FarePredictionService;
import edu.college.ridematcher.graph.CityGraph;
import edu.college.ridematcher.matching.CompatibilityRelation;
import edu.college.ridematcher.matching.MatchResult;
import edu.college.ridematcher.matching.MatchingEngine;
import edu.college.ridematcher.model.*;
import edu.college.ridematcher.service.RideService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Small localhost presentation server. Matching and fare logic stay in the existing Java/Python services. */
public final class WebMain {
    private static final Set<String> DEMO_ZONES = new HashSet<String>(Arrays.asList("A", "B", "C", "D", "E"));
    private final CityGraph graph = createCityGraph();
    private final RideService rideService = new RideService(new MatchingEngine(graph, new CompatibilityRelation()), createDrivers());
    private final AtomicInteger tripCounter = new AtomicInteger(1);
    private final String pythonCommand;

    private WebMain(String pythonCommand) { this.pythonCommand = pythonCommand; }

    public static void main(String[] args) throws IOException {
        String python = System.getProperty("python.command", System.getenv("PYTHON_COMMAND"));
        if (python == null || python.trim().isEmpty()) python = "python";
        int port = Integer.getInteger("ride.port", 8080);
        final WebMain app = new WebMain(python);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", app::servePage);
        server.createContext("/api/match", app::matchRide);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Ride-Hailing Driver Matcher dashboard running at http://127.0.0.1:" + port);
        System.out.println("Python executable: " + python);
        System.out.println("Press Ctrl+C to stop the dashboard.");
    }

    private void servePage(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod()) || !"/".equals(exchange.getRequestURI().getPath())) {
            sendText(exchange, 404, "Not found.", "text/plain; charset=utf-8");
            return;
        }
        java.nio.file.Path page = Paths.get("web", "index.html");
        if (!Files.isRegularFile(page)) {
            sendText(exchange, 500, "Dashboard file is missing: " + page.toAbsolutePath(), "text/plain; charset=utf-8");
            return;
        }
        byte[] content = Files.readAllBytes(page);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
        exchange.close();
    }

    private void matchRide(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Use POST to submit a ride request.\"}");
            return;
        }
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> form = parseForm(body);
            String pickup = required(form, "pickup").toUpperCase(Locale.ROOT);
            String destination = required(form, "destination").toUpperCase(Locale.ROOT);
            String typeText = required(form, "vehicle").toUpperCase(Locale.ROOT);
            String demand = required(form, "demand").toUpperCase(Locale.ROOT);
            if (!DEMO_ZONES.contains(pickup) || !DEMO_ZONES.contains(destination)) throw new IllegalArgumentException("Choose pickup and destination zones from A to E.");
            if (pickup.equals(destination)) throw new IllegalArgumentException("Pickup and destination must be different zones.");
            if (!Arrays.asList("CAR", "SUV", "BIKE").contains(typeText)) throw new IllegalArgumentException("Choose CAR, SUV, or BIKE.");
            if (!Arrays.asList("LOW", "NORMAL", "HIGH").contains(demand)) throw new IllegalArgumentException("Choose LOW, NORMAL, or HIGH demand.");
            int passengers;
            try { passengers = Integer.parseInt(required(form, "passengers")); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("Passenger count must be a whole number."); }
            if (passengers < 1 || passengers > 8) throw new IllegalArgumentException("Passenger count must be between 1 and 8.");

            String tripId = "WEB" + tripCounter.getAndIncrement();
            RideRequest request = new RideRequest(new Rider(tripId, "Dashboard Rider"), new Location(pickup), new Location(destination), VehicleType.valueOf(typeText), passengers);
            List<String> trace = new ArrayList<String>();
            MatchResult result = rideService.requestRide(request, trace);
            if (!result.isMatched()) {
                sendJson(exchange, 200, noMatchJson(trace));
                return;
            }

            Driver selected = result.getDriver();
            Map<Location, Integer> distances = graph.breadthFirstDistances(request.getPickup());
            Integer routeHops = distances.get(request.getDestination());
            if (routeHops == null) throw new IllegalArgumentException("The selected city graph has no route between these zones.");
            double distanceKm = routeHops * 2.5;
            int durationMinutes = Math.max(5, routeHops * 8);
            Integer fare = null;
            String fareError = null;
            try { fare = Integer.valueOf(new FarePredictionService(pythonCommand, Paths.get(".").toAbsolutePath().normalize()).estimateFare(request, routeHops.intValue(), demand)); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); fareError = "Fare prediction was interrupted. Please try again."; }
            catch (IOException error) { fareError = friendlyFareError(error.getMessage()); }

            sendJson(exchange, 200, successJson(request, selected, result, routeHops.intValue(), distanceKm, durationMinutes, demand, fare, fareError, trace));
        } catch (IllegalArgumentException error) {
            sendJson(exchange, 400, errorJson(error.getMessage()));
        } catch (Exception error) {
            sendJson(exchange, 500, errorJson("The matcher could not complete this request. Check the Java server output and try again."));
            System.err.println("Dashboard request failed: " + error.getMessage());
        }
    }

    private String friendlyFareError(String message) {
        if (message == null || message.trim().isEmpty()) return "Fare prediction is unavailable. The driver match is still complete.";
        if (message.contains("fare_model.joblib")) return "The fare model is missing. Run python-ml/train_model.py, then try again.";
        if (message.contains("Could not start Python executable")) return "Python could not be started. Set -Dpython.command to your Python executable.";
        if (message.contains("Python fare prediction failed")) return "Python fare prediction failed. " + message.substring("Python fare prediction failed.".length()).trim();
        return "Fare prediction is unavailable. " + message;
    }

    private String successJson(RideRequest request, Driver driver, MatchResult result, int routeHops, double distanceKm, int durationMinutes, String demand, Integer fare, String fareError, List<String> trace) {
        StringBuilder json = new StringBuilder("{\"matched\":true");
        property(json, "driverId", driver.getId()); property(json, "driverName", driver.getName());
        property(json, "vehicle", driver.getVehicle().getType().name()); property(json, "pickup", request.getPickup().getZone());
        property(json, "destination", request.getDestination().getZone()); property(json, "driverZone", driver.getLocation().getZone());
        property(json, "demand", demand);
        json.append(",\"proximityHops\":").append(result.getDistance()).append(",\"waitingMinutes\":").append(driver.getWaitingMinutes());
        json.append(",\"routeHops\":").append(routeHops).append(",\"distanceKm\":").append(distanceKm).append(",\"durationMinutes\":").append(durationMinutes);
        if (fare == null) json.append(",\"fare\":null"); else json.append(",\"fare\":").append(fare.intValue());
        if (fareError == null) json.append(",\"fareError\":null"); else { json.append(",\"fareError\":"); quote(json, fareError); }
        json.append(",\"trace\":"); stringArray(json, trace); json.append('}');
        return json.toString();
    }

    private String noMatchJson(List<String> trace) {
        StringBuilder json = new StringBuilder("{\"matched\":false,\"message\":\"No compatible driver is currently available for this request.\",\"trace\":");
        stringArray(json, trace); return json.append('}').toString();
    }
    private String errorJson(String message) { StringBuilder json = new StringBuilder("{\"error\":"); quote(json, message == null ? "Invalid ride request." : message); return json.append('}').toString(); }
    private void property(StringBuilder json, String key, String value) { json.append(','); quote(json, key); json.append(':'); quote(json, value); }
    private void stringArray(StringBuilder json, List<String> values) { json.append('['); for (int i = 0; i < values.size(); i++) { if (i > 0) json.append(','); quote(json, values.get(i)); } json.append(']'); }
    private void quote(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '"' || character == '\\') json.append('\\').append(character);
            else if (character == '\n') json.append("\\n");
            else if (character == '\r') json.append("\\r");
            else if (character == '\t') json.append("\\t");
            else if (character >= 32) json.append(character);
        }
        json.append('"');
    }
    private void sendJson(HttpExchange exchange, int status, String json) throws IOException { sendText(exchange, status, json, "application/json; charset=utf-8"); }
    private void sendText(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes); exchange.close();
    }
    private String readBody(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024]; int count; int total = 0;
        while ((count = input.read(buffer)) != -1) { total += count; if (total > 8192) throw new IllegalArgumentException("Ride request is too large."); bytes.write(buffer, 0, count); }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }
    private Map<String, String> parseForm(String body) throws IOException {
        Map<String, String> values = new HashMap<String, String>();
        for (String item : body.split("&")) {
            String[] pair = item.split("=", 2);
            if (pair.length == 2) values.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
        }
        return values;
    }
    private String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Please complete all ride request fields.");
        return value.trim();
    }

    private static CityGraph createCityGraph() {
        CityGraph graph = new CityGraph();
        String[][] edges = {{"A","B"},{"A","C"},{"B","D"},{"B","E"},{"C","F"},{"D","G"},{"E","G"},{"F","G"},{"G","H"}};
        for (String[] edge : edges) graph.connect(new Location(edge[0]), new Location(edge[1]));
        return graph;
    }
    private static List<Driver> createDrivers() {
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
