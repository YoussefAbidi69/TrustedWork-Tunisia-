"""
TrustedWork – AI Service
generate_dataset.py

Génère un dataset synthétique réaliste de 1200 utilisateurs
avec des comportements cohérents pour entraîner le modèle de Churn Prediction.
"""

import pandas as pd
import numpy as np

np.random.seed(42)
N = 1200  # nombre d'utilisateurs simulés


def generate_dataset():
    records = []

    for i in range(N):
        # --- Features comportementales ---
        days_inactive = np.random.choice(
            [0, 1, 2, 3, 5, 7, 10, 14, 20, 30, 45, 60],
            p=[0.12, 0.10, 0.10, 0.09, 0.09, 0.08, 0.08, 0.08, 0.07, 0.07, 0.06, 0.06]
        )
        current_streak = max(0, int(np.random.exponential(scale=8)) if days_inactive <= 2 else 0)
        xp_points = max(0, int(np.random.normal(loc=1500, scale=800)))
        level = max(1, min(10, xp_points // 300))
        engagement_score = max(0.0, min(100.0, round(np.random.normal(
            loc=max(10, 80 - days_inactive * 2.5), scale=10), 2)))
        badges_count = max(0, int(np.random.poisson(lam=max(0.5, 5 - days_inactive * 0.1))))
        events_attended = max(0, int(np.random.poisson(lam=max(0.3, 4 - days_inactive * 0.08))))
        challenges_completed = max(0, int(np.random.poisson(lam=max(0.2, 3 - days_inactive * 0.07))))
        longest_streak = max(current_streak, int(np.random.exponential(scale=10)))

        # --- Calcul de la cible (churn) ---
        # Règles logiques réalistes :
        churn_score = 0

        if days_inactive >= 30:
            churn_score += 4
        elif days_inactive >= 14:
            churn_score += 3
        elif days_inactive >= 7:
            churn_score += 2
        elif days_inactive >= 3:
            churn_score += 1

        if current_streak == 0:
            churn_score += 2
        elif current_streak < 3:
            churn_score += 1

        if engagement_score < 20:
            churn_score += 2
        elif engagement_score < 40:
            churn_score += 1

        if badges_count == 0:
            churn_score += 1

        if events_attended == 0:
            churn_score += 1

        if xp_points < 300:
            churn_score += 1

        # Seuil : churn si score >= 5
        churn = 1 if churn_score >= 5 else 0

        # Ajouter du bruit réaliste (5%)
        if np.random.random() < 0.05:
            churn = 1 - churn

        records.append({
            "user_id": i + 1,
            "xp_points": xp_points,
            "level": level,
            "engagement_score": engagement_score,
            "current_streak": current_streak,
            "longest_streak": longest_streak,
            "days_inactive": days_inactive,
            "badges_count": badges_count,
            "events_attended": events_attended,
            "challenges_completed": challenges_completed,
            "churn": churn
        })

    df = pd.DataFrame(records)

    # Affichage stats
    print(f"✅ Dataset généré : {len(df)} lignes")
    print(f"   Actifs (churn=0) : {len(df[df['churn'] == 0])} ({100 * len(df[df['churn'] == 0]) / len(df):.1f}%)")
    print(f"   Churned (churn=1): {len(df[df['churn'] == 1])} ({100 * len(df[df['churn'] == 1]) / len(df):.1f}%)")
    print(f"\n{df.describe().to_string()}")

    df.to_csv("dataset.csv", index=False)
    print("\n✅ Fichier dataset.csv sauvegardé.")
    return df


if __name__ == "__main__":
    generate_dataset()
