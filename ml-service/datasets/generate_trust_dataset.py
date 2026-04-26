import pandas as pd
import numpy as np
import random

random.seed(42)
np.random.seed(42)

# ============================================================
# Génération du dataset Trust Score — 500 lignes synthétiques
# Features basées sur les entités FreelancerProfile + Skill
# Labels : LOW / MEDIUM / HIGH
# ============================================================

rows = []

def generate_profile(trust_level: str) -> dict:
    """Génère un profil freelancer selon le niveau de confiance cible"""

    if trust_level == "HIGH":
        return {
            "completeness_score":       random.randint(80, 100),
            "skill_count":              random.randint(6, 15),
            "endorsement_count":        random.randint(10, 40),
            "portfolio_count":          random.randint(5, 20),
            "certification_count":      random.randint(3, 10),
            "work_experience_months":   random.randint(24, 120),
            "review_count":             random.randint(10, 60),
            "avg_rating":               round(random.uniform(4.2, 5.0), 1),
            "kyc_verified":             1,
            "two_factor_enabled":       1,
            "trust_level":              "HIGH"
        }
    elif trust_level == "MEDIUM":
        return {
            "completeness_score":       random.randint(45, 79),
            "skill_count":              random.randint(3, 7),
            "endorsement_count":        random.randint(2, 12),
            "portfolio_count":          random.randint(1, 6),
            "certification_count":      random.randint(1, 4),
            "work_experience_months":   random.randint(6, 30),
            "review_count":             random.randint(2, 12),
            "avg_rating":               round(random.uniform(3.0, 4.3), 1),
            "kyc_verified":             random.choice([0, 1]),
            "two_factor_enabled":       random.choice([0, 1]),
            "trust_level":              "MEDIUM"
        }
    else:  # LOW
        return {
            "completeness_score":       random.randint(0, 44),
            "skill_count":              random.randint(0, 3),
            "endorsement_count":        random.randint(0, 3),
            "portfolio_count":          random.randint(0, 2),
            "certification_count":      random.randint(0, 1),
            "work_experience_months":   random.randint(0, 8),
            "review_count":             random.randint(0, 3),
            "avg_rating":               round(random.uniform(1.0, 3.2), 1),
            "kyc_verified":             0,
            "two_factor_enabled":       0,
            "trust_level":              "LOW"
        }

# 500 lignes — équilibre entre les 3 classes
for _ in range(167):
    rows.append(generate_profile("HIGH"))
for _ in range(167):
    rows.append(generate_profile("MEDIUM"))
for _ in range(166):
    rows.append(generate_profile("LOW"))

random.shuffle(rows)

df = pd.DataFrame(rows)
df.index = range(len(df))

output_path = "datasets/freelancer_trust_dataset.csv"
df.to_csv(output_path, index=False, encoding="utf-8")

print(f"Dataset généré : {len(df)} lignes")
print(f"\nDistribution trust_level :")
print(df["trust_level"].value_counts())
print(f"\nAperçu statistiques :")
print(df.describe().round(2))