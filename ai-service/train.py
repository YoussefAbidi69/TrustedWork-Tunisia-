"""
TrustedWork – AI Service
train.py

Entraîne un Random Forest Classifier sur le dataset de churn.
Génère :
  - model.pkl         : modèle sérialisé prêt pour la production
  - model_stats.json  : métriques académiques complètes
"""

import json
import joblib
import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score, StratifiedKFold
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    confusion_matrix,
    classification_report,
    roc_auc_score
)
from sklearn.preprocessing import StandardScaler
from generate_dataset import generate_dataset

# ─────────────────────────────────────────────
# 1. Chargement du dataset
# ─────────────────────────────────────────────
print("=" * 60)
print("  TrustedWork – Churn Prediction Model Training")
print("=" * 60)

try:
    df = pd.read_csv("dataset.csv")
    print(f"✅ Dataset chargé depuis dataset.csv ({len(df)} lignes)")
except FileNotFoundError:
    print("⚙️  dataset.csv introuvable, génération en cours...")
    df = generate_dataset()

# ─────────────────────────────────────────────
# 2. Préparation des features
# ─────────────────────────────────────────────
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
TARGET = "churn"

X = df[FEATURES]
y = df[TARGET]

print(f"\n📊 Features utilisées : {FEATURES}")
print(f"   Cible              : {TARGET}")
print(f"   Distribution churn : {y.value_counts().to_dict()}")

# ─────────────────────────────────────────────
# 3. Train / Test Split (80/20)
# ─────────────────────────────────────────────
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)
print(f"\n📂 Split : {len(X_train)} train | {len(X_test)} test")

# ─────────────────────────────────────────────
# 4. Entraînement du modèle
# ─────────────────────────────────────────────
print("\n🚀 Entraînement du Random Forest Classifier...")
model = RandomForestClassifier(
    n_estimators=200,
    max_depth=10,
    min_samples_split=5,
    min_samples_leaf=2,
    random_state=42,
    class_weight="balanced",
    n_jobs=-1
)
model.fit(X_train, y_train)
print("✅ Entraînement terminé.")

# ─────────────────────────────────────────────
# 5. Évaluation sur le test set
# ─────────────────────────────────────────────
y_pred = model.predict(X_test)
y_prob = model.predict_proba(X_test)[:, 1]

accuracy   = round(accuracy_score(y_test, y_pred) * 100, 2)
precision  = round(precision_score(y_test, y_pred) * 100, 2)
recall     = round(recall_score(y_test, y_pred) * 100, 2)
f1         = round(f1_score(y_test, y_pred) * 100, 2)
roc_auc    = round(roc_auc_score(y_test, y_prob) * 100, 2)
cm         = confusion_matrix(y_test, y_pred).tolist()

print(f"\n📈 Résultats sur le Test Set :")
print(f"   Accuracy  : {accuracy}%")
print(f"   Precision : {precision}%")
print(f"   Recall    : {recall}%")
print(f"   F1-Score  : {f1}%")
print(f"   ROC-AUC   : {roc_auc}%")
print(f"\n   Confusion Matrix :\n   {cm}")
print(f"\n{classification_report(y_test, y_pred, target_names=['Actif', 'Churn'])}")

# ─────────────────────────────────────────────
# 6. Cross-Validation (k=5 folds stratifiés)
# ─────────────────────────────────────────────
print("🔄 Cross-Validation (StratifiedKFold, k=5)...")
cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
cv_scores = cross_val_score(model, X, y, cv=cv, scoring="accuracy")
cv_mean = round(cv_scores.mean() * 100, 2)
cv_std  = round(cv_scores.std() * 100, 2)
print(f"   CV Accuracy : {cv_mean}% ± {cv_std}%")
print(f"   Scores par fold : {[round(s * 100, 2) for s in cv_scores]}")

# ─────────────────────────────────────────────
# 7. Feature Importance
# ─────────────────────────────────────────────
feature_importance = {
    feat: round(float(imp) * 100, 2)
    for feat, imp in zip(FEATURES, model.feature_importances_)
}
sorted_fi = dict(sorted(feature_importance.items(), key=lambda x: x[1], reverse=True))
print(f"\n🏆 Feature Importance :")
for feat, score in sorted_fi.items():
    bar = "█" * int(score // 2)
    print(f"   {feat:<25} {score:>6.2f}%  {bar}")

# ─────────────────────────────────────────────
# 8. Sauvegarde du modèle et des stats
# ─────────────────────────────────────────────
joblib.dump(model, "model.pkl")
print("\n✅ Modèle sauvegardé → model.pkl")

stats = {
    "algorithm": "Random Forest Classifier",
    "n_estimators": 200,
    "training_samples": len(X_train),
    "test_samples": len(X_test),
    "features": FEATURES,
    "metrics": {
        "accuracy": accuracy,
        "precision": precision,
        "recall": recall,
        "f1_score": f1,
        "roc_auc": roc_auc
    },
    "cross_validation": {
        "k_folds": 5,
        "mean_accuracy": cv_mean,
        "std_accuracy": cv_std,
        "fold_scores": [round(s * 100, 2) for s in cv_scores]
    },
    "confusion_matrix": {
        "true_negative": cm[0][0],
        "false_positive": cm[0][1],
        "false_negative": cm[1][0],
        "true_positive": cm[1][1]
    },
    "feature_importance": sorted_fi
}

with open("model_stats.json", "w", encoding="utf-8") as f:
    json.dump(stats, f, indent=2, ensure_ascii=False)

print("✅ Statistiques sauvegardées → model_stats.json")
print("\n" + "=" * 60)
print(f"  ✅ MODÈLE PRÊT — Accuracy: {accuracy}% | F1: {f1}% | AUC: {roc_auc}%")
print("=" * 60)
