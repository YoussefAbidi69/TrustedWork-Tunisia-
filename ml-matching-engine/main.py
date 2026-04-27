from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import numpy as np

app = FastAPI(title="TrustedWork ML Matching Engine")

score_model = None
prob_model = None

@app.on_event("startup")
def load_models():
    global score_model, prob_model
    try:
        score_model = joblib.load("models/score_model.pkl")
        prob_model = joblib.load("models/prob_model.pkl")
        print("Models loaded successfully.")
    except Exception as e:
        print(f"Error loading models: {e}")

class MatchRequest(BaseModel):
    skill_match: float
    reputation: float
    success_rate: float
    budget_fit: float
    availability: float

class MatchResponse(BaseModel):
    totalScore: float
    successProbability: float
    confidence: str

def get_confidence_label(prob: float) -> str:
    if prob < 0.4:
        return "LOW"
    elif prob <= 0.7:
        return "MEDIUM"
    return "HIGH"

@app.post("/predict", response_model=MatchResponse)
def predict_match(request: MatchRequest):
    print(f"Incoming features: skill={request.skill_match}, rep={request.reputation}, succ={request.success_rate}, budg={request.budget_fit}, avail={request.availability}")
    features = np.array([[
        request.skill_match,
        request.reputation,
        request.success_rate,
        request.budget_fit,
        request.availability
    ]])
    
    score_pred = score_model.predict(features)[0]
    prob_pred = prob_model.predict(features)[0]
    
    score_pred = max(0.0, min(100.0, float(score_pred)))
    prob_pred = max(0.0, min(1.0, float(prob_pred)))
    
    confidence = get_confidence_label(prob_pred)
    
    return MatchResponse(
        totalScore=round(score_pred, 2),
        successProbability=round(prob_pred, 4),
        confidence=confidence
    )
