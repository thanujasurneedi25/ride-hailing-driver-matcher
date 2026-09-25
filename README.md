# Ride-Hailing Driver Matcher

## Project Overview
A college demonstration that finds a compatible nearby driver in a small simulated city and estimates a fare with an explainable regression model. It includes the original Java console demonstration and a local browser dashboard as an additional presentation layer.

## Features
- Java OOP ride, driver, vehicle, and location models.
- Undirected adjacency-list city graph with BFS zone-hop proximity.
- Compatibility checks for availability, vehicle type, and passenger capacity.
- Deterministic driver selection: nearest compatible driver, then waiting-time tie-break, then ID.
- Python scikit-learn LinearRegression fare estimate over CSV handoff.
- Local responsive dashboard with request form, match trace, simulated graph, and fare details.
- The console demo and all five original matching scenarios remain available.

## Architecture

```text
User
 ↓
Visual UI (local HTML/CSS/JavaScript)
 ↓ HTTP form request
Java WebMain / RideService
 ├── OOP Models
 ├── Compatibility Relation
 ├── City Graph (adjacency list)
 ├── BFS
 └── MatchingEngine
          ↓ matched trip CSV
     FarePredictionService
          ↓ ProcessBuilder
       Python ML
          ↓
    Linear Regression
          ↓ predicted fare CSV
       Java UI response
```

`Main` remains the console entry point. `WebMain` is a separate entry point using Java 8's built-in `HttpServer`; both reuse the same models, `RideService`, `MatchingEngine`, and `FarePredictionService`; the web entry point builds the same A–H sample graph. The visual dashboard is served from `web/index.html` by the local Java process. It does not use a separate matching implementation.

## Technologies
- Java 8, Java Collections Framework, and built-in `com.sun.net.httpserver.HttpServer`
- HTML, CSS, and browser JavaScript
- Python 3.9 or newer
- pandas, scikit-learn, and joblib
- CSV files for Java/Python fare communication

## Algorithms
- **Graph representation:** `CityGraph` stores an undirected simulated city as adjacency lists. Its connections are A–B, A–C, B–D, B–E, C–F, D–G, E–G, F–G, and G–H. The dashboard SVG displays this same fixed graph.
- **BFS:** Breadth-first search starts at the rider pickup and calculates minimum edge counts to driver zones. These are graph hops, not road distance or travel time.
- **Compatibility relation:** A driver must be available, match the requested vehicle type, and have sufficient capacity.
- **Fairness tie-break:** `MatchingEngine` sorts compatible drivers by pickup proximity, then driver waiting minutes, then driver ID for deterministic results.
- **Linear regression:** Python one-hot encodes demand and vehicle categories, trains scikit-learn `LinearRegression` on distance, duration, passenger count, demand, and vehicle type, then predicts a nonnegative fare. The model is educational; its simulated data and metrics do not establish real-world accuracy.

## How to Run
Run commands from the project root in PowerShell.

### 1. Set up and train the fare model

```powershell
py -3 -m venv python-ml/.venv
.\python-ml\.venv\Scripts\Activate.ps1
python -m pip install -r python-ml/requirements.txt
python python-ml/train_model.py
```

Train once before using the dashboard. The UI's default Python command is `python`. To configure a different executable, pass its full path in `-Dpython.command` as shown below.

### 2. Compile Java

```powershell
javac -d out (Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName })
```

### 3. Start the visual dashboard

```powershell
java "-Dpython.command=$((Resolve-Path .\python-ml\.venv\Scripts\python.exe).Path)" -cp out edu.college.ridematcher.web.WebMain
```

Open [http://127.0.0.1:8080](http://127.0.0.1:8080). Stop the local server with Ctrl+C. Port can be changed with `-Dride.port=8081`. Python can also be configured with the `PYTHON_COMMAND` environment variable.

The dashboard supports pickup/destination zones A–E, CAR/SUV/BIKE, 1–8 passengers, and LOW/NORMAL/HIGH demand. BIKE is a supported academic vehicle category with a one-seat sample driver; two or more passengers therefore demonstrate the no-compatible-driver state.

### 4. Run the preserved console demonstration

```powershell
java "-Dpython.command=$((Resolve-Path .\python-ml\.venv\Scripts\python.exe).Path)" -cp out edu.college.ridematcher.Main
```

The console still prints its ride example and five Stage 1 scenarios.

## Project Limitations
- City zones, edges, driver availability, and driver locations are simulated and fixed for the demo.
- Fare training data is simulated educational data, not real ride-hailing records.
- Route distance is estimated at 2.5 km per graph hop and duration at 8 minutes per hop (minimum 5 minutes); neither is a road route or a travel-time prediction.
- The prototype is local and single-machine, with no real-time drivers, GPS, real maps, authentication, or production fare logic.

## Future Improvements
Possible future work includes real historical ride data, geospatial indexing, real-time GPS, advanced ML matching, mobile applications, and real map integration. These features are not implemented in this project.


