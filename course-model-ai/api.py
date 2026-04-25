"""
api.py
------
Flask REST API that exposes the trained model to your Spring Boot app.

ENDPOINTS:
    POST /predict   — score a course before it is published
    POST /retrain   — retrain the model with latest data from DB
    GET  /health    — check if the API is running
    GET  /report    — see the latest training metrics

RUN THIS:
    python api.py

YOUR SPRING BOOT APP CALLS:
    POST http://localhost:5000/predict
    with JSON body: { "title": "...", "description": "..." }
"""

import os
import json
import pickle
import numpy as np
from flask import Flask, request, jsonify
from datetime import datetime
import plagiarism

app = Flask(__name__)


MODEL_DIR = os.path.join(os.path.dirname(__file__), "model")

# ── Load model files at startup ───────────────────────────────────────────────
def load_model():
    """Load classifier, feature builder, and label encoder from disk."""
    paths = {
        "classifier":     os.path.join(MODEL_DIR, "classifier.pkl"),
        "feature_builder":os.path.join(MODEL_DIR, "feature_builder.pkl"),
        "label_encoder":  os.path.join(MODEL_DIR, "label_encoder.pkl"),
    }

    missing = [k for k, v in paths.items() if not os.path.exists(v)]
    if missing:
        return None, None, None, f"Missing model files: {missing}. Run train.py first."

    with open(paths["classifier"],      "rb") as f: clf     = pickle.load(f)
    with open(paths["feature_builder"], "rb") as f: builder = pickle.load(f)
    with open(paths["label_encoder"],   "rb") as f: le      = pickle.load(f)

    return clf, builder, le, None

clf, feature_builder, label_encoder, load_error = load_model()


# ── Helper: build the feedback bullets ───────────────────────────────────────
def build_feedback(title: str, description: str,
                   comment_count: int, report_count: int) -> list[dict]:
    """
    Returns a list of feedback bullets explaining the score.
    These are rule-based signals that complement the model output.
    In a more advanced version you would use SHAP values here.

    Each bullet has:
        type    — 'positive' | 'neutral' | 'negative'
        message — plain English explanation
    """
    bullets = []

    title_words = len(title.strip().split()) if title.strip() else 0
    desc_words  = len(description.strip().split()) if description.strip() else 0

    # Title checks
    if title_words >= 5:
        bullets.append({"type": "positive", "message": "Title is descriptive and clear"})
    elif title_words >= 3:
        bullets.append({"type": "neutral",  "message": "Title could be more specific"})
    else:
        bullets.append({"type": "negative", "message": "Title is too short — be more descriptive"})

    # Description length checks
    if desc_words >= 40:
        bullets.append({"type": "positive", "message": "Description is detailed and thorough"})
    elif desc_words >= 15:
        bullets.append({"type": "neutral",  "message": "Description is okay but could have more detail"})
    else:
        bullets.append({"type": "negative", "message": "Description is too short — explain what learners will gain"})

    # Content depth signals
    if desc_words >= 20 and title_words >= 4:
        bullets.append({"type": "positive", "message": "Good overall content structure"})

    # Report warning
    if report_count > 0:
        bullets.append({
            "type": "negative",
            "message": f"This course has been reported {report_count} time(s) — review the content"
        })

    # Engagement signal
    if comment_count >= 3:
        bullets.append({"type": "positive", "message": "Community is actively engaging with this course"})

    return bullets


# ── POST /predict ─────────────────────────────────────────────────────────────
@app.route("/predict", methods=["POST"])
def predict():
    """
    Score a course's quality before or after publishing.

    REQUEST BODY (JSON):
    {
        "title":         "Complete React Hooks Guide",
        "description":   "Learn useState useEffect and custom hooks with examples",
        "comment_count": 0,    -- optional, default 0
        "report_count":  0,    -- optional, default 0
        "up_votes":      0,    -- optional, default 0
        "down_votes":    0     -- optional, default 0
    }

    RESPONSE (JSON):
    {
        "score":          82,
        "label":          "high_quality",
        "label_display":  "High quality",
        "probabilities": {
            "high_quality": 0.82,
            "average":      0.15,
            "low_quality":  0.03
        },
        "feedback": [
            { "type": "positive", "message": "Title is descriptive and clear" },
            { "type": "negative", "message": "Description is too short" }
        ],
        "model_available": true
    }
    """
    if clf is None:
        # Model not trained yet — return a rule-based fallback score
        return _fallback_predict(request.get_json())

    data = request.get_json()
    if not data:
        return jsonify({"error": "Request body must be JSON"}), 400

    title         = data.get("title",         "")
    description   = data.get("description",   "")
    comment_count = int(data.get("comment_count", 0))
    report_count  = int(data.get("report_count",  0))
    up_votes      = int(data.get("up_votes",      0))
    down_votes    = int(data.get("down_votes",    0))

    # Build features and predict
    X = feature_builder.transform_single(
        title=title,
        description=description,
        comment_count=comment_count,
        report_count=report_count,
        up_votes=up_votes,
        down_votes=down_votes,
    )

    proba       = clf.predict_proba(X)[0]          # probabilities for each class
    pred_index  = int(np.argmax(proba))            # index of highest probability
    pred_label  = label_encoder.classes_[pred_index]

    # Map class probabilities to a dict
    prob_dict = {
        label_encoder.classes_[i]: round(float(proba[i]), 4)
        for i in range(len(label_encoder.classes_))
    }

    # Base score from probabilities
    hq_prob = prob_dict.get("high_quality", 0.0)
    lq_prob = prob_dict.get("low_quality",  0.0)
    base_score = (hq_prob - lq_prob * 0.5 + 0.5) * 100
    
    # Apply heuristic modifiers for stability and larger gaps
    title_words = len(title.strip().split())
    desc_words  = len(description.strip().split())
    
    score_modifier = 0
    if title_words >= 5:   score_modifier += 15
    elif title_words < 3:  score_modifier -= 20
    
    if desc_words >= 40:   score_modifier += 20
    elif desc_words >= 15: score_modifier += 5
    elif desc_words < 5:   score_modifier -= 25
    
    score = int(round(base_score + score_modifier))
    score = max(0, min(100, score))   # clamp to [0, 100]

    label_display_map = {
        "high_quality": "High quality",
        "average":      "Average",
        "low_quality":  "Low quality",
    }

    return jsonify({
        "score":           score,
        "label":           pred_label,
        "label_display":   label_display_map.get(pred_label, pred_label),
        "probabilities":   prob_dict,
        "feedback":        build_feedback(title, description, comment_count, report_count),
        "model_available": True,
    })

# ── POST /check_plagiarism ────────────────────────────────────────────────────
@app.route("/check_plagiarism", methods=["POST"])
def check_plagiarism():
    """
    Checks if the given course content is too similar to any existing published course.
    """
    data = request.get_json()
    if not data:
        return jsonify({"error": "Request body must be JSON"}), 400
        
    try:
        result = plagiarism.check_similarity(data)
        return jsonify(result)
    except Exception as e:
        print(f"[api] ERROR checking plagiarism: {e}")
        return jsonify({"error": str(e)}), 500

# ── POST /update_index ────────────────────────────────────────────────────────
@app.route("/update_index", methods=["POST"])
def update_index():
    """Rebuilds the plagiarism index from the database."""
    try:
        plagiarism.build_index()
        return jsonify({"success": True})
    except Exception as e:
        print(f"[api] ERROR updating plagiarism index: {e}")
        return jsonify({"success": False, "error": str(e)}), 500


# ── POST /retrain ─────────────────────────────────────────────────────────────
@app.route("/retrain", methods=["POST"])
def retrain():
    """
    Retrain the model with the latest data from the database.
    Call this from a scheduled job (e.g. every night at midnight).

    REQUEST BODY (JSON):
    {
        "use_seed": false   -- set true to use seed data (dev only)
    }

    RESPONSE (JSON):
    {
        "success":    true,
        "accuracy":   0.87,
        "num_samples": 42,
        "trained_at": "2024-01-15T22:00:00"
    }
    """
    global clf, feature_builder, label_encoder

    data     = request.get_json() or {}
    use_seed = bool(data.get("use_seed", False))

    try:
        from train import train
        new_clf, new_builder, new_le, report = train(use_seed=use_seed)

        # Hot-swap the model without restarting the server
        clf             = new_clf
        feature_builder = new_builder
        label_encoder   = new_le

        return jsonify({
            "success":     True,
            "accuracy":    report["accuracy"],
            "num_samples": report["num_samples"],
            "trained_at":  report["trained_at"],
        })

    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500


# ── GET /health ───────────────────────────────────────────────────────────────
@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status":          "ok",
        "model_loaded":    clf is not None,
        "load_error":      load_error,
        "timestamp":       datetime.utcnow().isoformat(),
    })


# ── GET /report ───────────────────────────────────────────────────────────────
@app.route("/report", methods=["GET"])
def report():
    """Returns the latest training metrics."""
    path = os.path.join(MODEL_DIR, "training_report.json")
    if not os.path.exists(path):
        return jsonify({"error": "No training report found. Run train.py first."}), 404
    with open(path) as f:
        return jsonify(json.load(f))


# ── Fallback when model is not trained yet ────────────────────────────────────
def _fallback_predict(data: dict) -> tuple:
    """
    Rule-based scoring used when the model hasn't been trained yet.
    This ensures the API is useful from day one even with no training data.
    """
    if not data:
        return jsonify({"error": "Request body must be JSON"}), 400

    title       = data.get("title",       "")
    description = data.get("description", "")

    title_words = len(title.strip().split())
    desc_words  = len(description.strip().split())

    score = 50  # start at neutral
    if title_words >= 5:   score += 15
    elif title_words < 3:  score -= 20
    if desc_words >= 40:   score += 20
    elif desc_words >= 15: score += 5
    elif desc_words < 5:   score -= 25
    score = max(0, min(100, score))

    if score >= 65:
        label = "high_quality"
    elif score >= 35:
        label = "average"
    else:
        label = "low_quality"

    return jsonify({
        "score":           score,
        "label":           label,
        "label_display":   label.replace("_", " ").title(),
        "probabilities":   {},
        "feedback":        build_feedback(title, description, 0, 0),
        "model_available": False,
        "note":            "Using rule-based fallback — train the model for better results",
    })


if __name__ == "__main__":
    print("[api] Starting Course Quality API on http://localhost:5000")
    if load_error:
        print(f"[api] WARNING: {load_error}")
        print("[api] Run: python train.py --seed   to train on seed data")
        print("[api] The /predict endpoint will use a rule-based fallback until then")
        
    try:
        plagiarism.build_index()
    except Exception as e:
        print(f"[api] WARNING: Failed to build plagiarism index: {e}")
        
    app.run(host="0.0.0.0", port=5000, debug=False)
