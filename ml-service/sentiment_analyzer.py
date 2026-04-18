import joblib
import os

# ============================================================
# Analyseur de sentiment — TF-IDF + Logistic Regression
# Utilisé par app.py pour le endpoint POST /predict/sentiment
# ============================================================

# Chemins vers les modèles sauvegardés
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "models", "sentiment_model.pkl")
VECTORIZER_PATH = os.path.join(BASE_DIR, "models", "tfidf_vectorizer.pkl")


def load_sentiment_model():
    """Charge le modèle et le vectorizer depuis les fichiers .pkl"""
    model = joblib.load(MODEL_PATH)
    vectorizer = joblib.load(VECTORIZER_PATH)
    return model, vectorizer


def predict_sentiment(comment: str) -> dict:
    """
    Prédit le sentiment d'un commentaire.
    Retourne : { sentiment: POSITIVE/NEGATIVE, score: float }
    """
    model, vectorizer = load_sentiment_model()

    # Vectorisation du commentaire
    comment_vec = vectorizer.transform([comment])

    # Prédiction + probabilité
    sentiment = model.predict(comment_vec)[0]
    score = float(model.predict_proba(comment_vec).max())

    return {
        "sentiment": sentiment,
        "score": round(score, 4)
    }