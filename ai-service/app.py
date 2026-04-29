"""
TrustedWork – AI Service
app.py

API Flask exposant le modèle de Churn Prediction sur le port 5001.
Consommé par le microservice Spring Boot (module06-engagement).
"""

import json
import os
import joblib
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# ─────────────────────────────────────────────
# Chargement du modèle au démarrage
# ─────────────────────────────────────────────
MODEL_PATH = "model.pkl"
STATS_PATH = "model_stats.json"

model = None
model_stats = {}

def load_model():
    global model, model_stats
    if os.path.exists(MODEL_PATH):
        model = joblib.load(MODEL_PATH)
        print(f"✅ Modèle chargé depuis {MODEL_PATH}")
    else:
        print("⚠️  model.pkl introuvable. Lance train.py d'abord.")

    if os.path.exists(STATS_PATH):
        with open(STATS_PATH, "r", encoding="utf-8") as f:
            model_stats = json.load(f)

load_model()

FEATURES = [
    "xp_points",
    "level",
    "engagement_score",
    "current_streak",
    "longest_streak",
    "days_inactive",
    "badges_count",
    "events_attended",
    "challenges_completed"
]

# ─────────────────────────────────────────────
# Endpoints
# ─────────────────────────────────────────────

@app.route("/health", methods=["GET"])
def health():
    """Status du service."""
    return jsonify({
        "status": "UP",
        "service": "TrustedWork AI Service",
        "model_loaded": model is not None,
        "version": "1.0.0"
    })


@app.route("/predict/churn", methods=["POST"])
def predict_churn():
    """
    Prédit le risque de churn d'un utilisateur.

    Body JSON attendu :
    {
        "user_id": 42,
        "xp_points": 1500,
        "level": 5,
        "engagement_score": 65.0,
        "current_streak": 3,
        "longest_streak": 12,
        "days_inactive": 2,
        "badges_count": 4,
        "events_attended": 3,
        "challenges_completed": 2
    }
    """
    if model is None:
        return jsonify({"error": "Modèle non chargé. Lance train.py d'abord."}), 503

    data = request.get_json()
    if not data:
        return jsonify({"error": "Body JSON requis"}), 400

    # Validation des features
    missing = [f for f in FEATURES if f not in data]
    if missing:
        return jsonify({"error": f"Features manquantes: {missing}"}), 400

    # Préparation du vecteur de features
    feature_vector = np.array([[data[f] for f in FEATURES]])

    # Prédiction
    prediction = int(model.predict(feature_vector)[0])
    probabilities = model.predict_proba(feature_vector)[0]
    churn_probability = round(float(probabilities[1]) * 100, 2)
    active_probability = round(float(probabilities[0]) * 100, 2)

    # Calcul du label
    top_risk_factors = []
    eng_score = data.get('engagement_score', 100)
    
    if churn_probability >= 70:
        risk_label = "HIGH"
        risk_color = "red"
        recommendation = "⚠️ Intervention urgente requise. Envoyer une notification de ré-engagement."
        if data.get('days_inactive', 0) > 7: top_risk_factors.append(f"Inactivité sévère ({data.get('days_inactive')} jours)")
        if eng_score < 30: top_risk_factors.append(f"Engagement critique ({round(eng_score, 2)})")
        if data.get('current_streak', 1) == 0: top_risk_factors.append("Série brisée (0)")
    elif churn_probability >= 40:
        risk_label = "MEDIUM"
        risk_color = "orange"
        recommendation = "📌 Surveiller l'utilisateur. Proposer un challenge ou un badge bonus."
        if data.get('days_inactive', 0) > 3: top_risk_factors.append(f"Début d'inactivité ({data.get('days_inactive')} jours)")
        if eng_score < 50: top_risk_factors.append(f"Baisse d'engagement ({round(eng_score, 2)})")
    else:
        risk_label = "LOW"
        risk_color = "green"
        recommendation = "✅ Utilisateur engagé. Continuer à encourager via gamification."
        top_risk_factors.append("Indicateurs au vert")

    if not top_risk_factors:
        top_risk_factors.append("Signaux faibles croisés (Random Forest)")

    return jsonify({
        "user_id": data.get("user_id"),
        "churn_predicted": bool(prediction),
        "churn_probability": churn_probability,
        "active_probability": active_probability,
        "risk_label": risk_label,
        "risk_color": risk_color,
        "recommendation": recommendation,
        "top_risk_factors": top_risk_factors,
        "model": "Random Forest Classifier"
    })


@app.route("/model/stats", methods=["GET"])
def get_model_stats():
    """Retourne les métriques académiques du modèle entraîné."""
    if not model_stats:
        return jsonify({"error": "Statistiques non disponibles. Lance train.py."}), 404
    return jsonify(model_stats)


@app.route("/model/reload", methods=["POST"])
def reload_model():
    """Recharge le modèle depuis le disque (après un ré-entraînement)."""
    load_model()
    return jsonify({
        "status": "reloaded",
        "model_loaded": model is not None
    })


# ─────────────────────────────────────────────
# Démarrage
# ─────────────────────────────────────────────
if __name__ == "__main__":
    print("🚀 TrustedWork AI Service démarré sur http://localhost:5001")
    app.run(host="0.0.0.0", port=5001, debug=False)
