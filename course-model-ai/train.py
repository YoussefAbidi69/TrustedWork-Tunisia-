"""
train.py
--------
Trains the course quality classifier and saves everything needed
for inference to the /model directory.

RUN THIS:
    python train.py              # uses real DB
    python train.py --seed       # uses seed data (no DB needed)

WHAT IT SAVES:
    model/classifier.pkl         -- the trained sklearn classifier
    model/feature_builder.pkl    -- the FeatureBuilder instance
    model/label_encoder.pkl      -- maps class names to integers
    model/training_report.json   -- accuracy, f1, confusion matrix
"""

import os
import json
import pickle
import argparse
import numpy as np
import pandas as pd
from datetime import datetime, UTC

from sklearn.linear_model    import LogisticRegression
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.preprocessing   import LabelEncoder
from sklearn.metrics         import (
    classification_report, confusion_matrix, accuracy_score
)

from data_loader import load_training_data, load_seed_data
from features    import FeatureBuilder

MODEL_DIR = os.path.join(os.path.dirname(__file__), "model")
os.makedirs(MODEL_DIR, exist_ok=True)


def train(use_seed: bool = False):
    print("=" * 60)
    print("  Course Quality Classifier — Training")
    print("=" * 60)

    # ── 1. Load data ───────────────────────────────────────────────────────────
    df = load_seed_data() if use_seed else load_training_data()

    if len(df) < 10:
        print(
            f"[train] WARNING: Only {len(df)} training examples. "
            "The model will not generalise well. "
            "Publish more courses and collect votes before retraining."
        )

    # ── 2. Build features ──────────────────────────────────────────────────────
    builder = FeatureBuilder()
    builder.fit(df)
    X = builder.transform(df)

    # ── 3. Encode labels ───────────────────────────────────────────────────────
    le = LabelEncoder()
    y = le.fit_transform(df["label"])
    # Classes will be sorted alphabetically:
    # 0 = average, 1 = high_quality, 2 = low_quality
    print(f"[train] Classes: {list(le.classes_)}")

    # ── 4. Train / test split ──────────────────────────────────────────────────
    # If we have very few samples, skip the split and train on everything
    if len(df) >= 20:
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42, stratify=y
        )
        has_test_set = True
    else:
        print("[train] Small dataset — training on full data, no test split")
        X_train, y_train = X, y
        X_test,  y_test  = X, y
        has_test_set = False

    # ── 5. Train classifier ────────────────────────────────────────────────────
    # Logistic Regression works well here because:
    # - It is fast to train and predict
    # - It outputs calibrated probabilities (important for the score display)
    # - It is interpretable
    # max_iter=1000 because embedding features can take more iterations to converge
    print("[train] Training Logistic Regression classifier...")
    clf = LogisticRegression(
        max_iter=1000,
        class_weight="balanced",  # handles imbalanced classes automatically
        C=1.0,
        random_state=42,
    )
    clf.fit(X_train, y_train)

    # ── 6. Evaluate ────────────────────────────────────────────────────────────
    y_pred = clf.predict(X_test)
    acc    = accuracy_score(y_test, y_pred)
    report = classification_report(
        y_test, y_pred,
        target_names=le.classes_,
        output_dict=True,
    )
    cm = confusion_matrix(y_test, y_pred).tolist()

    print(f"\n[train] Accuracy: {acc:.3f}")
    print(f"[train] Classification report:")
    print(classification_report(y_test, y_pred, target_names=le.classes_))
    print(f"[train] Confusion matrix:\n{np.array(cm)}")

    # Cross-validation score (only if enough data)
    if len(df) >= 20:
        cv_scores = cross_val_score(clf, X, y, cv=3, scoring="accuracy")
        print(f"[train] 3-fold CV accuracy: {cv_scores.mean():.3f} ± {cv_scores.std():.3f}")
    else:
        cv_scores = np.array([acc])

    # ── 7. Save everything ─────────────────────────────────────────────────────
    def save(obj, filename):
        path = os.path.join(MODEL_DIR, filename)
        with open(path, "wb") as f:
            pickle.dump(obj, f)
        print(f"[train] Saved {filename}")

    save(clf,     "classifier.pkl")
    save(builder, "feature_builder.pkl")
    save(le,      "label_encoder.pkl")

    # Save a training report so you can track progress over time
    training_report = {
        "trained_at":        datetime.now(UTC).isoformat(),
        "num_samples":       len(df),
        "used_seed_data":    use_seed,
        "classes":           list(le.classes_),
        "accuracy":          round(acc, 4),
        "cv_accuracy_mean":  round(float(cv_scores.mean()), 4),
        "cv_accuracy_std":   round(float(cv_scores.std()), 4),
        "classification_report": report,
        "confusion_matrix":  cm,
        "label_distribution": df["label"].value_counts().to_dict(),
    }

    report_path = os.path.join(MODEL_DIR, "training_report.json")
    with open(report_path, "w") as f:
        json.dump(training_report, f, indent=2)
    print(f"[train] Saved training_report.json")

    print("\n[train] Training complete.")
    print(f"[train] Model files in: {MODEL_DIR}")
    return clf, builder, le, training_report


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--seed",
        action="store_true",
        help="Use seed data instead of the real database (for development)"
    )
    args = parser.parse_args()
    train(use_seed=args.seed)
