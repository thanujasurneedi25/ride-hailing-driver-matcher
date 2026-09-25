package edu.college.ridematcher.fare;

import edu.college.ridematcher.model.RideRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Exchanges trip and predicted fare data with the Python scripts through CSV files. */
public final class FarePredictionService {
    private final String pythonCommand;
    private final Path projectDirectory;

    public FarePredictionService(String pythonCommand, Path projectDirectory) {
        if (pythonCommand == null || pythonCommand.trim().isEmpty()) throw new IllegalArgumentException("Python executable command is required.");
        this.pythonCommand = pythonCommand;
        this.projectDirectory = projectDirectory;
    }

    public int estimateFare(RideRequest request, int routeHops) throws IOException, InterruptedException {
        return estimateFare(request, routeHops, System.getProperty("ride.demand", "NORMAL"));
    }

    public int estimateFare(RideRequest request, int routeHops, String requestedDemand) throws IOException, InterruptedException {
        if (routeHops < 0) throw new IllegalArgumentException("Trip route is not reachable in the city graph.");
        Path pythonDirectory = projectDirectory.resolve("python-ml");
        Path dataDirectory = pythonDirectory.resolve("data");
        Path inputFile = dataDirectory.resolve("trip_input.csv");
        Path outputFile = dataDirectory.resolve("fare_output.csv");
        Path predictor = pythonDirectory.resolve("predict_fare.py");
        if (!Files.isRegularFile(predictor)) throw new IOException("Python prediction script is missing: " + predictor);
        Files.createDirectories(dataDirectory);

        // The graph hop count is only a simple classroom proxy for trip distance/duration.
        double estimatedDistanceKm = routeHops * 2.5;
        int estimatedDurationMinutes = Math.max(5, routeHops * 8);
        String demand = requestedDemand == null ? "" : requestedDemand.trim().toUpperCase();
        if (!"LOW".equals(demand) && !"NORMAL".equals(demand) && !"HIGH".equals(demand)) {
            throw new IOException("Invalid ride.demand setting. Use LOW, NORMAL, or HIGH.");
        }
        String csv = "trip_id,distance_km,duration_min,demand_level,passengers,vehicle_type\n"
                + csvValue(request.getRider().getId()) + "," + estimatedDistanceKm + "," + estimatedDurationMinutes + ","
                + demand + "," + request.getPassengerCount() + "," + request.getRequiredVehicleType().name() + "\n";
        Files.write(inputFile, csv.getBytes(StandardCharsets.UTF_8));
        Files.deleteIfExists(outputFile); // Never mistake a previous run's fare for this ride's result.

        Process process;
        try {
            process = new ProcessBuilder(pythonCommand, predictor.toString())
                    .directory(projectDirectory.toFile()).redirectErrorStream(true).start();
        } catch (IOException error) {
            throw new IOException("Could not start Python executable '" + pythonCommand + "'. Install Python or set -Dpython.command to its executable.", error);
        }
        StringBuilder pythonOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) pythonOutput.append(line).append(System.lineSeparator());
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new IOException("Python fare prediction failed. " + pythonOutput.toString().trim());
        if (!Files.isRegularFile(outputFile)) throw new IOException("Python finished without creating fare_output.csv.");
        return readPredictedFare(outputFile, request.getRider().getId());
    }

    private int readPredictedFare(Path outputFile, String expectedTripId) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(outputFile, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !"trip_id,predicted_fare".equals(header.trim())) throw new IOException("Fare output has an invalid CSV header.");
            String row = reader.readLine();
            if (row == null) throw new IOException("Fare output contains no prediction.");
            String[] fields = row.split(",", -1);
            if (fields.length != 2 || !expectedTripId.equals(fields[0])) throw new IOException("Fare output does not match the current trip.");
            try {
                double fare = Double.parseDouble(fields[1]);
                if (Double.isNaN(fare) || Double.isInfinite(fare) || fare < 0 || fare > Integer.MAX_VALUE) throw new NumberFormatException();
                return (int) Math.round(fare);
            } catch (NumberFormatException error) {
                throw new IOException("Fare output contains an invalid predicted_fare value.");
            }
        }
    }

    private String csvValue(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
}


