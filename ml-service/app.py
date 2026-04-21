from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import pandas as pd
import numpy as np
import pickle
import os
import json

# Chemin vers le dossier contenant les données
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "freelancer_datasets")

print(f"📁 Dossier de base: {BASE_DIR}")
print(f"📁 Dossier des données: {DATA_DIR}")

# Importer le service d'embeddings
from embedding_service import EmbeddingService

app = FastAPI(title="Freelance Matching ML Service")

# Ajouter CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Variables globales
model = None
freelancers = None
embedding_service = None
feature_cols = None
CATEGORIES = None

# --- Modèles de données ---
class ProjectRequest(BaseModel):
    description: str
    budget: float
    deadline_days: int
    category: str
    optimization_mode: str

class RecommendationResponse(BaseModel):
    freelancer_id: str
    name: str
    category: str
    experience_level: str
    hourly_rate_usd: float
    avg_rating: float
    success_proba: float
    semantic_score: float
    final_score: float
    cin: str
    skills: List[str]

@app.on_event("startup")
async def load_models():
    global model, freelancers, feature_cols, CATEGORIES, embedding_service
    
    print("\n" + "="*50)
    print("🚀 DÉMARRAGE DU SERVICE ML")
    print("="*50)
    
    # 1. Charger les freelancers
    freelancers_path = os.path.join(DATA_DIR, "freelancers_dataset_with_cin.csv")
    print(f"📂 Chargement des freelancers: {freelancers_path}")
    
    if not os.path.exists(freelancers_path):
        print(f"❌ Fichier non trouvé: {freelancers_path}")
        print("📁 Contenu du dossier DATA_DIR:")
        if os.path.exists(DATA_DIR):
            for f in os.listdir(DATA_DIR):
                print(f"   - {f}")
        else:
            print(f"   ❌ Le dossier {DATA_DIR} n'existe pas!")
        return
    
    freelancers = pd.read_csv(freelancers_path)
    print(f"✅ {len(freelancers)} freelancers chargés")
    
    # 2. Charger les catégories
    CATEGORIES = freelancers['category'].unique().tolist()
    print(f"📂 Catégories: {CATEGORIES}")
    
    # 3. Charger le modèle LightGBM
    model_path = os.path.join(DATA_DIR, "model.pkl")
    print(f"📂 Chargement du modèle: {model_path}")
    with open(model_path, "rb") as f:
        model = pickle.load(f)
    print(f"✅ Modèle LightGBM chargé")
    
    # 4. Charger les feature columns
    metadata_path = os.path.join(DATA_DIR, "metadata.json")
    with open(metadata_path, "r", encoding="utf-8") as f:
        metadata = json.load(f)
        feature_cols = metadata['feature_cols']
    print(f"✅ {len(feature_cols)} features chargées")
    print(f"   AUC du modèle: {metadata['auc']:.4f}")
    
    # 5. Initialiser le service d'embeddings
    embedding_service = EmbeddingService()
    embedding_service.load_freelancers(freelancers)
    
    print("\n✅ SERVICE ML PRÊT !")
    print("="*50 + "\n")

@app.get("/health")
async def health_check():
    return {
        "status": "ok",
        "model_loaded": model is not None,
        "n_freelancers": len(freelancers) if freelancers is not None else 0
    }

@app.post("/recommend", response_model=List[RecommendationResponse])
async def get_recommendations(request: ProjectRequest):
    if model is None:
        raise HTTPException(status_code=500, detail="Model not loaded")
    
    print("\n" + "-"*40)
    print(f"📝 Nouvelle requête:")
    print(f"   Description: {request.description[:100]}...")
    print(f"   Budget: {request.budget} USD")
    print(f"   Délai: {request.deadline_days} jours")
    print(f"   Catégorie: {request.category}")
    print(f"   Mode: {request.optimization_mode}")
    print("-"*40)
    
    # 1. Filtrage sémantique (Top 50 similaires)
    semantic_matches = embedding_service.get_similarity_scores(request.description, top_k=50)
    semantic_ids = [m['freelancer_id'] for m in semantic_matches]
    semantic_scores = {m['freelancer_id']: m['semantic_score'] for m in semantic_matches}
    
    # 2. Filtrer les freelancers sémantiquement pertinents
    filtered_freelancers = freelancers[freelancers['freelancer_id'].isin(semantic_ids)].copy()
    print(f"📊 {len(filtered_freelancers)} freelancers après filtrage sémantique")
    
    if len(filtered_freelancers) == 0:
        return []
    
    # 3. Calculer les features pour chaque freelancer
    rows = []
    for _, fl in filtered_freelancers.iterrows():
        estimated_total = fl['hourly_rate_usd'] * 40
        budget_match = 1 - abs(estimated_total - request.budget) / max(request.budget, 1)
        budget_match = float(np.clip(budget_match, -1, 1))
        deadline_feasible = 1 if fl['available_in_days'] < request.deadline_days * 0.5 else 0
        category_match = 1 if fl['category'] == request.category else 0
        cat_enc = CATEGORIES.index(request.category) if request.category in CATEGORIES else 0
        
        row = {
            "project_budget_usd": request.budget,
            "project_deadline_days": request.deadline_days,
            "fl_hourly_rate": fl['hourly_rate_usd'],
            "fl_n_projects": fl['n_completed_projects'],
            "fl_completion_rate": fl['completion_rate'],
            "fl_on_time_rate": fl['on_time_delivery_rate'],
            "fl_avg_rating": fl['avg_rating'],
            "fl_dispute_rate": fl['dispute_rate'],
            "fl_repeat_client_rate": fl['repeat_client_rate'],
            "fl_available_in_days": fl['available_in_days'],
            "fl_hours_per_week": fl['hours_per_week'],
            "fl_avg_response_hours": fl['avg_response_hours'],
            "fl_portfolio_score": fl['portfolio_score'],
            "budget_match_score": budget_match,
            "deadline_feasible": deadline_feasible,
            "category_match": category_match,
            "exp_level_num": {"junior": 1, "mid": 2, "senior": 3, "expert": 4}[fl['experience_level']],
            "project_category_enc": cat_enc,
        }
        rows.append(row)
    
    X_score = pd.DataFrame(rows)[feature_cols]
    success_probas = model.predict_proba(X_score)[:, 1]
    
    optimization_weights = {
        'best': (1.0, 0.0, 0.0, 0.0),
        'fastest': (0.4, 0.4, 0.0, 0.2),
        'best_value': (0.4, 0.0, 0.4, 0.2),
        'lowest_risk': (0.4, 0.0, 0.0, 0.6)
    }
    
    w_success, w_time, w_value, w_risk = optimization_weights.get(
        request.optimization_mode, (1.0, 0.0, 0.0, 0.0)
    )
    
    results = []
    for i, (_, fl) in enumerate(filtered_freelancers.iterrows()):
        time_score = 1 - min(1, fl['available_in_days'] / 30)
        value_score = max(0, rows[i]['budget_match_score'])
        risk_score = (1 - fl['dispute_rate']) * 0.5 + fl['completion_rate'] * 0.5
        
        final_score = (
            w_success * success_probas[i] +
            w_time * time_score +
            w_value * value_score +
            w_risk * risk_score
        )
        
        semantic_score = semantic_scores.get(fl['freelancer_id'], 0)
        final_score = final_score * 0.7 + semantic_score * 0.3
        
        skills_list = fl['skills_str'].split(', ') if pd.notna(fl['skills_str']) else []
        
        results.append({
            'freelancer_id': fl['freelancer_id'],
            'name': fl['name'],
            'category': fl['category'],
            'experience_level': fl['experience_level'],
            'hourly_rate_usd': float(fl['hourly_rate_usd']),
            'avg_rating': float(fl['avg_rating']),
            'success_proba': float(success_probas[i]),
            'semantic_score': float(semantic_score),
            'final_score': float(final_score),
            'cin': str(fl['cin']),
            'skills': skills_list
        })
    
    results.sort(key=lambda x: x['final_score'], reverse=True)
    print(f"✅ {len(results[:20])} recommandations générées")
    
    return results[:20]

if __name__ == "__main__":
    import uvicorn
    print("\n🚀 Démarrage du serveur ML sur http://localhost:8001")
    uvicorn.run(app, host="0.0.0.0", port=8001)