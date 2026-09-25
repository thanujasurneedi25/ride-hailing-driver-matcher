"""Predict a fare from data/trip_input.csv using the trained pipeline."""
from pathlib import Path
import math
import sys

import joblib
import pandas as pd

BASE_DIR = Path(__file__).resolve().parent
INPUT_FILE = BASE_DIR / "data" / "trip_input.csv"
OUTPUT_FILE = BASE_DIR / "data" / "fare_output.csv"
MODEL_FILE = BASE_DIR / "models" / "fare_model.joblib"
NUMERIC_FEATURES = ["distance_km", "duration_min", "passengers"]
CATEGORICAL_FEATURES = ["demand_level", "vehicle_type"]
FEATURES = NUMERIC_FEATURES + CATEGORICAL_FEATURES
ALLOWED_DEMAND = {"LOW", "NORMAL", "HIGH"}
ALLOWED_VEHICLES = {"AUTO", "BIKE", "CAR", "SUV"}


def main():
    if not MODEL_FILE.is_file():
        raise FileNotFoundError("Trained model is missing (models/fare_model.joblib). Run train_model.py first.")
    if not INPUT_FILE.is_file():
        raise FileNotFoundError("Trip input is missing: " + str(INPUT_FILE))
    data = pd.read_csv(INPUT_FILE)
    required = ["trip_id"] + FEATURES
    missing = [column for column in required if column not in data.columns]
    if missing:
        raise ValueError("Trip input is missing columns: " + ", ".join(missing))
    if data.empty:
        raise ValueError("Trip input contains no rides.")
    if data[required].isnull().any().any():
        raise ValueError("Trip input contains blank values.")
    for column in NUMERIC_FEATURES:
        data[column] = pd.to_numeric(data[column], errors="raise")
        if not data[column].map(math.isfinite).all():
            raise ValueError("{} must contain finite numbers.".format(column))
        if (data[column] < 0).any():
            raise ValueError("{} cannot be negative.".format(column))
    if (data["passengers"] < 1).any() or (data["passengers"] % 1 != 0).any():
        raise ValueError("passengers must be a positive whole number.")
    data["demand_level"] = data["demand_level"].astype(str).str.upper()
    data["vehicle_type"] = data["vehicle_type"].astype(str).str.upper()
    if not set(data["demand_level"]).issubset(ALLOWED_DEMAND):
        raise ValueError("demand_level must be LOW, NORMAL, or HIGH.")
    if not set(data["vehicle_type"]).issubset(ALLOWED_VEHICLES):
        raise ValueError("vehicle_type must be AUTO, BIKE, CAR, or SUV.")

    model = joblib.load(MODEL_FILE)
    raw_predictions = model.predict(data[FEATURES])
    if not all(math.isfinite(float(value)) for value in raw_predictions):
        raise ValueError("Model returned a non-finite fare.")
    # Fares are displayed as whole currency units in this teaching prototype.
    predicted = [int(round(max(0.0, float(value)))) for value in raw_predictions]
    result = pd.DataFrame({"trip_id": data["trip_id"].astype(str), "predicted_fare": predicted})
    result.to_csv(OUTPUT_FILE, index=False)
    print("Predicted fare(s) written to: " + str(OUTPUT_FILE))
    for row in result.itertuples(index=False):
        print("{} -> {}".format(row.trip_id, row.predicted_fare))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError) as error:
        print("Prediction error: {}".format(error), file=sys.stderr)
        sys.exit(1)




