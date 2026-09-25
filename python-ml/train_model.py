"""Train and save a small, explainable fare regression pipeline."""
from pathlib import Path
import math
import sys

import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder

BASE_DIR = Path(__file__).resolve().parent
DATA_FILE = BASE_DIR / "data" / "training_data.csv"
MODEL_FILE = BASE_DIR / "models" / "fare_model.joblib"
NUMERIC_FEATURES = ["distance_km", "duration_min", "passengers"]
CATEGORICAL_FEATURES = ["demand_level", "vehicle_type"]
FEATURES = NUMERIC_FEATURES + CATEGORICAL_FEATURES
TARGET = "fare"


def main():
    if not DATA_FILE.is_file():
        raise FileNotFoundError("Training data is missing: " + str(DATA_FILE))
    data = pd.read_csv(DATA_FILE)
    missing = [column for column in FEATURES + [TARGET] if column not in data.columns]
    if missing:
        raise ValueError("Training CSV is missing columns: " + ", ".join(missing))
    if len(data) < 4:
        raise ValueError("Training data needs at least four rows for the demonstration split.")
    if data[FEATURES + [TARGET]].isnull().any().any():
        raise ValueError("Training data contains blank feature or fare values.")
    for column in NUMERIC_FEATURES + [TARGET]:
        data[column] = pd.to_numeric(data[column], errors="raise")
        if not data[column].map(math.isfinite).all():
            raise ValueError("{} must contain finite numbers.".format(column))
        if (data[column] < 0).any():
            raise ValueError("{} cannot be negative.".format(column))
    if (data["passengers"] < 1).any() or (data["passengers"] % 1 != 0).any():
        raise ValueError("passengers must be a positive whole number.")

    preprocessor = ColumnTransformer([
        ("numbers", "passthrough", NUMERIC_FEATURES),
        ("categories", OneHotEncoder(handle_unknown="ignore"), CATEGORICAL_FEATURES),
    ])
    pipeline = Pipeline([
        ("preprocessing", preprocessor),
        ("regression", LinearRegression()),
    ])
    x_train, x_test, y_train, y_test = train_test_split(
        data[FEATURES], data[TARGET], test_size=0.25, random_state=42
    )
    pipeline.fit(x_train, y_train)
    predictions = pipeline.predict(x_test)
    print("Evaluation on a small held-out part of the simulated dataset:")
    print("MAE: {:.2f} fare units".format(mean_absolute_error(y_test, predictions)))
    print("R^2: {:.3f}".format(r2_score(y_test, predictions)))
    print("These scores do not establish real-world accuracy; the sample is tiny and simulated.")

    pipeline.fit(data[FEATURES], data[TARGET])
    MODEL_FILE.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, MODEL_FILE)
    print("Trained model saved to: " + str(MODEL_FILE))


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, KeyError) as error:
        print("Training error: {}".format(error), file=sys.stderr)
        sys.exit(1)



