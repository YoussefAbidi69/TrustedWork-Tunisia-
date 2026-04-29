import pandas as pd
import joblib
import os
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report

# ============================================================
# Entraînement des modèles Sentiment + Trust Score
# Exécuter UNE FOIS : python train_models.py
# Génère : models/sentiment_model.pkl + models/tfidf_vectorizer.pkl
#          models/trust_model.pkl
# ============================================================

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATASET_PATH = os.path.join(BASE_DIR, "datasets", "reviews_sentiment_dataset.csv")
MODEL_PATH = os.path.join(BASE_DIR, "models", "sentiment_model.pkl")
VECTORIZER_PATH = os.path.join(BASE_DIR, "models", "tfidf_vectorizer.pkl")


def train_sentiment_model():
    print("=== Entraînement du modèle Sentiment ===")

    # Chargement du dataset
    df = pd.read_csv(DATASET_PATH)
    print(f"Dataset chargé : {len(df)} lignes")
    print(f"Distribution : {df['sentiment'].value_counts().to_dict()}")

    X = df["comment"]
    y = df["sentiment"]

    # Split train/test (80/20) avec stratification
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    # Vectorisation TF-IDF avec bigrammes
    vectorizer = TfidfVectorizer(
        max_features=3000,
        ngram_range=(1, 2),   # unigrammes + bigrammes
        sublinear_tf=True,    # log(tf) pour réduire l'effet des mots fréquents
        strip_accents="unicode"  # normalise é→e, â→a… pour tolérer les textes sans accents
    )
    X_train_vec = vectorizer.fit_transform(X_train)
    X_test_vec = vectorizer.transform(X_test)

    # Entraînement Logistic Regression
    model = LogisticRegression(max_iter=300, random_state=42, C=1.0)
    model.fit(X_train_vec, y_train)

    # Évaluation
    y_pred = model.predict(X_test_vec)
    accuracy = accuracy_score(y_test, y_pred)
    print(f"\nAccuracy : {accuracy:.4f} ({accuracy * 100:.1f}%)")
    print("\nRapport de classification :")
    print(classification_report(y_test, y_pred))

    # Sauvegarde des modèles
    os.makedirs(os.path.join(BASE_DIR, "models"), exist_ok=True)
    joblib.dump(model, MODEL_PATH)
    joblib.dump(vectorizer, VECTORIZER_PATH)
    print(f"\nModèle sauvegardé : {MODEL_PATH}")
    print(f"Vectorizer sauvegardé : {VECTORIZER_PATH}")

    # Test rapide sur quelques phrases
    print("\n=== Tests de prédiction ===")
    tests = [
        "Excellent travail, très professionnel et rapide",
        "Très déçu, travail bâclé et délais non respectés",
        "Great work, delivered on time and above expectations",
        "Terrible experience, missed every deadline",
        "Très bon freelancer, je recommande vivement",
        "Freelancer peu sérieux, résultat médiocre",
    ]
    for comment in tests:
        vec = vectorizer.transform([comment])
        pred = model.predict(vec)[0]
        score = float(model.predict_proba(vec).max())
        print(f"  [{pred} | {score:.2f}] {comment[:55]}")

    return accuracy


def train_trust_model():
    print("\n=== Entraînement du modèle Trust Score ===")

    from sklearn.ensemble import RandomForestClassifier

    TRUST_DATASET_PATH = os.path.join(BASE_DIR, "datasets", "freelancer_trust_dataset.csv")
    TRUST_MODEL_PATH = os.path.join(BASE_DIR, "models", "trust_model.pkl")

    # Chargement du dataset
    df = pd.read_csv(TRUST_DATASET_PATH)
    print(f"Dataset chargé : {len(df)} lignes")
    print(f"Distribution : {df['trust_level'].value_counts().to_dict()}")

    FEATURE_COLUMNS = [
        "completeness_score", "skill_count", "endorsement_count",
        "portfolio_count", "certification_count", "work_experience_months",
        "review_count", "avg_rating", "kyc_verified", "two_factor_enabled"
    ]

    X = df[FEATURE_COLUMNS]
    y = df["trust_level"]

    # Split train/test (80/20)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    # Random Forest — 100 arbres
    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        random_state=42,
        class_weight="balanced"
    )
    model.fit(X_train, y_train)

    # Évaluation
    y_pred = model.predict(X_test)
    accuracy = accuracy_score(y_test, y_pred)
    print(f"\nAccuracy : {accuracy:.4f} ({accuracy * 100:.1f}%)")
    print("\nRapport de classification :")
    print(classification_report(y_test, y_pred))

    # Importance des features — utile pour expliquer au jury
    print("Importance des features :")
    for col, imp in sorted(
        zip(FEATURE_COLUMNS, model.feature_importances_),
        key=lambda x: x[1], reverse=True
    ):
        print(f"  {col:<30} {imp:.4f}")

    # Sauvegarde
    os.makedirs(os.path.join(BASE_DIR, "models"), exist_ok=True)
    joblib.dump(model, TRUST_MODEL_PATH)
    print(f"\nModèle sauvegardé : {TRUST_MODEL_PATH}")

    # Tests de prédiction
    print("\n=== Tests de prédiction ===")
    tests = [
        {"completeness_score": 95, "skill_count": 10, "endorsement_count": 20,
         "portfolio_count": 8, "certification_count": 5, "work_experience_months": 48,
         "review_count": 25, "avg_rating": 4.8, "kyc_verified": 1, "two_factor_enabled": 1},
        {"completeness_score": 55, "skill_count": 4, "endorsement_count": 5,
         "portfolio_count": 2, "certification_count": 2, "work_experience_months": 12,
         "review_count": 5, "avg_rating": 3.5, "kyc_verified": 1, "two_factor_enabled": 0},
        {"completeness_score": 20, "skill_count": 1, "endorsement_count": 0,
         "portfolio_count": 0, "certification_count": 0, "work_experience_months": 2,
         "review_count": 0, "avg_rating": 1.5, "kyc_verified": 0, "two_factor_enabled": 0},
    ]
    labels = ["HIGH attendu", "MEDIUM attendu", "LOW attendu"]
    for data, label in zip(tests, labels):
        features = [[data[col] for col in FEATURE_COLUMNS]]
        pred = model.predict(features)[0]
        conf = float(model.predict_proba(features).max())
        print(f"  [{pred} | {conf:.2f}] {label}")

    return accuracy


if __name__ == "__main__":
    accuracy_sentiment = train_sentiment_model()
    accuracy_trust = train_trust_model()

    print("\n========== RÉSUMÉ ==========")
    if accuracy_sentiment >= 0.80:
        print(f"[OK] Sentiment   : {accuracy_sentiment*100:.1f}% >= 80%")
    else:
        print(f"[!!] Sentiment   : {accuracy_sentiment*100:.1f}% (insuffisant)")

    if accuracy_trust >= 0.80:
        print(f"[OK] Trust Score : {accuracy_trust*100:.1f}% >= 80%")
    else:
        print(f"[!!] Trust Score : {accuracy_trust*100:.1f}% (insuffisant)")