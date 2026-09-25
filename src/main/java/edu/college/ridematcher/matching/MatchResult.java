package edu.college.ridematcher.matching;

import edu.college.ridematcher.model.Driver;

public final class MatchResult {
    private final Driver driver;
    private final int distance;
    private final String message;
    private MatchResult(Driver driver, int distance, String message) { this.driver = driver; this.distance = distance; this.message = message; }
    public static MatchResult success(Driver driver, int distance) { return new MatchResult(driver, distance, "Match successful."); }
    public static MatchResult failure(String message) { return new MatchResult(null, -1, message); }
    public boolean isMatched() { return driver != null; }
    public Driver getDriver() { return driver; }
    public int getDistance() { return distance; }
    public String getMessage() { return message; }
}
