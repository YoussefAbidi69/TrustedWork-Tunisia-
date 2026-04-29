import joblib
import os

# ============================================================
# Classificateur Trust Score — Random Forest
# Utilisé par app.py pour le endpoint POST /predict/trust-score
# ============================================================

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # remonte à la racine du projet
MODEL_PATH = os.path.join(BASE_DIR, "models", "trust_model.pkl")

FEATURE_COLUMNS = [
    "completeness_score",
    "skill_count",
    "endorsement_count",
    "portfolio_count",
    "certification_count",
    "work_experience_months",
    "review_count",
    "avg_rating",
    "kyc_verified",
    "two_factor_enabled"
]


def load_trust_model():
    """Charge le modèle Random Forest depuis le fichier .pkl"""
    return joblib.load(MODEL_PATH)


def predict_trust(data: dict) -> dict:
    """
    Prédit le niveau de confiance d'un profil freelancer.
    Retourne : { level: HIGH/MEDIUM/LOW, confidence: float }
    """
    model = load_trust_model()

    # Construction du vecteur de features dans le bon ordre
    features = [[data.get(col, 0) for col in FEATURE_COLUMNS]]

    level = model.predict(features)[0]
    confidence = float(model.predict_proba(features).max())

    return {
        "level": level,
        "confidence": round(confidence, 4)
    }