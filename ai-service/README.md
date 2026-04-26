# 🧠 TrustedWork – AI Service (Churn Prediction)

Microservice Python de prédiction du risque d'abandon (Churn) des utilisateurs TrustedWork.
Basé sur un modèle **Random Forest Classifier** entraîné via **scikit-learn**.

---

## 📂 Structure du projet

```
ai-service/
├── generate_dataset.py   # Génération du dataset synthétique (1200 utilisateurs)
├── train.py              # Entraînement du modèle + métriques académiques
├── app.py                # API Flask (port 5001)
├── requirements.txt      # Dépendances Python
├── dataset.csv           # Généré automatiquement
├── model.pkl             # Modèle sérialisé (généré après train.py)
└── model_stats.json      # Métriques du modèle (généré après train.py)
```

---

## 🚀 Installation & Lancement

### 1. Installer les dépendances

```bash
cd ai-service
pip install -r requirements.txt
```

### 2. Générer le dataset

```bash
python generate_dataset.py
```

### 3. Entraîner le modèle

```bash
python train.py
```

> Résultat attendu : ~88% accuracy, F1 > 85%, ROC-AUC > 90%

### 4. Lancer l'API Flask

```bash
python app.py
```

> L'API sera disponible sur : **http://localhost:5001**

---

## 🔌 Endpoints disponibles

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/health` | Status du service |
| `POST` | `/predict/churn` | Prédiction du risque de churn |
| `GET` | `/model/stats` | Métriques académiques du modèle |
| `POST` | `/model/reload` | Recharger le modèle après ré-entraînement |

---

## 📊 Exemple de requête

```bash
curl -X POST http://localhost:5001/predict/churn \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": 42,
    "xp_points": 850,
    "level": 3,
    "engagement_score": 35.5,
    "current_streak": 0,
    "longest_streak": 7,
    "days_inactive": 12,
    "badges_count": 1,
    "events_attended": 1,
    "challenges_completed": 0
  }'
```

### Réponse attendue

```json
{
  "user_id": 42,
  "churn_predicted": true,
  "churn_probability": 83.4,
  "active_probability": 16.6,
  "risk_label": "HIGH",
  "risk_color": "red",
  "recommendation": "⚠️ Intervention urgente requise. Envoyer une notification de ré-engagement.",
  "model": "Random Forest Classifier"
}
```

---

## 🎓 Métriques académiques

Accessible via `GET /model/stats` ou dans `model_stats.json` après entraînement :

- **Accuracy** : ~88%
- **Precision** : ~87%
- **Recall** : ~86%
- **F1-Score** : ~86%
- **ROC-AUC** : ~93%
- **Cross-validation** : k=5 folds stratifiés

---

## 🔗 Intégration Spring Boot

Le microservice Spring Boot (port 8086) appelle ce service via :
- `GET /api/analytics/churn-prediction/{userId}` → Prédiction ML
- `GET /api/analytics/model/stats` → Métriques du modèle
