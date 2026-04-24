import os
import joblib
from flask import Flask, request, jsonify
from flask_cors import CORS

# ============================================================
# ml-service — API Flask
#   POST /predict/sentiment    → POSITIVE / NEGATIVE + score
#   POST /predict/trust-score  → HIGH / MEDIUM / LOW + confidence
#   GET  /health               → statut du service
# ============================================================

app = Flask(__name__)
CORS(app)  # autorise les requêtes cross-origin (frontend / autre service)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.join(BASE_DIR, "models")

FEATURE_COLUMNS = [
    "completeness_score", "skill_count", "endorsement_count",
    "portfolio_count", "certification_count", "work_experience_months",
    "review_count", "avg_rating", "kyc_verified", "two_factor_enabled"
]

# ── Chargement paresseux des modèles (une seule fois au premier appel) ──────

_sentiment_model = None
_sentiment_vectorizer = None
_trust_model = None


def get_sentiment_model():
    global _sentiment_model, _sentiment_vectorizer
    if _sentiment_model is None:
        _sentiment_model = joblib.load(os.path.join(MODELS_DIR, "sentiment_model.pkl"))
        _sentiment_vectorizer = joblib.load(os.path.join(MODELS_DIR, "tfidf_vectorizer.pkl"))
    return _sentiment_model, _sentiment_vectorizer


def get_trust_model():
    global _trust_model
    if _trust_model is None:
        _trust_model = joblib.load(os.path.join(MODELS_DIR, "trust_model.pkl"))
    return _trust_model


# ── Routes ───────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    """Vérifie que le service tourne et que les modèles sont accessibles."""
    missing = []
    for fname in ("sentiment_model.pkl", "tfidf_vectorizer.pkl", "trust_model.pkl"):
        if not os.path.exists(os.path.join(MODELS_DIR, fname)):
            missing.append(fname)
    if missing:
        return jsonify({"status": "degraded", "missing_models": missing}), 503
    return jsonify({"status": "ok"})


@app.post("/predict/sentiment")
def predict_sentiment():
    """
    Body JSON : { "comment": "..." }
    Réponse   : { "sentiment": "POSITIVE"|"NEGATIVE", "score": 0.97 }
    """
    body = request.get_json(silent=True)
    if not body or "comment" not in body:
        return jsonify({"error": "Champ 'comment' manquant dans le body JSON"}), 400

    comment = str(body["comment"]).strip()
    if not comment:
        return jsonify({"error": "Le commentaire est vide"}), 400

    model, vectorizer = get_sentiment_model()
    vec = vectorizer.transform([comment])
    sentiment = model.predict(vec)[0]
    score = float(model.predict_proba(vec).max())

    return jsonify({
        "sentiment": sentiment,
        "score": round(score, 4),
        "comment": comment
    })


@app.post("/predict/trust-score")
def predict_trust_score():
    """
    Body JSON : {
      "completeness_score": 85, "skill_count": 8, "endorsement_count": 12,
      "portfolio_count": 5, "certification_count": 3,
      "work_experience_months": 36, "review_count": 15,
      "avg_rating": 4.5, "kyc_verified": 1, "two_factor_enabled": 1
    }
    Réponse : { "level": "HIGH"|"MEDIUM"|"LOW", "confidence": 0.91 }
    """
    body = request.get_json(silent=True)
    if not body:
        return jsonify({"error": "Body JSON manquant"}), 400

    missing_fields = [col for col in FEATURE_COLUMNS if col not in body]
    if missing_fields:
        return jsonify({"error": "Champs manquants", "missing": missing_fields}), 400

    model = get_trust_model()
    features = [[body.get(col, 0) for col in FEATURE_COLUMNS]]
    level = model.predict(features)[0]
    confidence = float(model.predict_proba(features).max())

    return jsonify({
        "level": level,
        "confidence": round(confidence, 4)
    })


# ── Point d'entrée ───────────────────────────────────────────────────────────

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    print(f"🚀 ml-service démarré sur http://localhost:{port}")
    print(f"   GET  /health")
    print(f"   POST /predict/sentiment")
    print(f"   POST /predict/trust-score")
    app.run(host="0.0.0.0", port=port, debug=True)
