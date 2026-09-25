# Python Fare Regression (Stage 2)

This module trains a small LinearRegression pipeline on a simulated educational dataset and predicts fares from a CSV trip input. It is not real ride-hailing data, and its evaluation scores do not demonstrate production accuracy.

## Requirements and setup

Requires Python 3.9 or newer. From the project root in PowerShell:

```powershell
py -3 -m venv python-ml/.venv
.\python-ml\.venv\Scripts\Activate.ps1
python -m pip install -r python-ml/requirements.txt
```

The only direct dependencies are pandas, scikit-learn, and joblib.

## Train the model

```powershell
python python-ml/train_model.py
```

The script reads `data/training_data.csv`, evaluates a held-out split with MAE and R², then fits the pipeline on all rows and saves `models/fare_model.joblib`. The dataset is intentionally small and simulated. Metrics are included as a demonstration of evaluation, not proof of fare accuracy.

## Predict a fare

```powershell
python python-ml/predict_fare.py
```

The predictor reads `data/trip_input.csv`, loads the trained pipeline, validates values, clamps predictions to zero or above, rounds to a whole currency unit, and writes `data/fare_output.csv` with columns `trip_id,predicted_fare`.

Example output:

```csv
trip_id,predicted_fare
R1,135
```

The input columns are `trip_id,distance_km,duration_min,demand_level,passengers,vehicle_type`. Demand must be LOW, NORMAL, or HIGH; vehicle type must be AUTO, BIKE, CAR, or SUV. Numeric features must be nonnegative, and passenger count must be at least one.

## Model explanation

LinearRegression learns coefficients for the numeric features and one-hot encoded demand and vehicle categories. It finds a linear combination that minimizes squared differences between training fares and predicted fares. This makes the baseline straightforward to explain, but real fares can depend on many factors not represented here.

The CSV handoff is used by Java's `FarePredictionService`: Java writes a trip row, invokes `predict_fare.py`, waits for completion, and reads the resulting fare. Java's Python executable is configurable with `-Dpython.command=<path>` or `PYTHON_COMMAND`.

## Limitations

All example rows are simulated. Real-world fare prediction needs a large, representative historical dataset, sensible feature definitions, validation across time and locations, and domain review. The prototype's route distance and duration are estimated from graph hops.


