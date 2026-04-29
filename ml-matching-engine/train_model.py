import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score
import joblib
import os

def generate_synthetic_data(num_samples=10000):
    np.random.seed(42)
    
    # Generate random features
    skill_match = np.random.uniform(0, 100, num_samples)
    reputation = np.random.normal(70, 15, num_samples).clip(0, 100)
    success_rate = np.random.normal(75, 15, num_samples).clip(0, 100)
    budget_fit = np.random.uniform(0, 100, num_samples)
    availability = np.random.uniform(0, 100, num_samples)
    
    total_score = np.zeros(num_samples)
    probability = np.zeros(num_samples)
    
    for i in range(num_samples):
        # Base linear approximation (similar to old formula)
        base_score = (skill_match[i] * 0.40) + \
                     (reputation[i] * 0.20) + \
                     (success_rate[i] * 0.20) + \
                     (budget_fit[i] * 0.10) + \
                     (availability[i] * 0.10)
        
        # Inject non-linear real-world behavior
        # 1. Dealbreakers: if skills are too low, the score drops sharply
        if skill_match[i] < 30:
            base_score *= 0.5
            
        # 2. Synergies: high rep + high success boosts the score
        if reputation[i] > 85 and success_rate[i] > 85:
            base_score *= 1.15
            
        # 3. Budget mismatch ruins chances
        if budget_fit[i] < 20:
            base_score *= 0.7
            
        total_score[i] = np.clip(base_score, 0, 100)
        
        # Probability calculation (0 to 1)
        prob = (skill_match[i] / 100.0) * 0.5 + (reputation[i] / 100.0) * 0.3 + (success_rate[i] / 100.0) * 0.2
        if skill_match[i] < 25 or budget_fit[i] < 10:
            prob *= 0.3
        probability[i] = np.clip(prob, 0, 1)
        
    df = pd.DataFrame({
        'skill_match': skill_match,
        'reputation': reputation,
        'success_rate': success_rate,
        'budget_fit': budget_fit,
        'availability': availability,
        'total_score': total_score,
        'probability': probability
    })
    return df

def train_models():
    print("Generating synthetic historical dataset...")
    df = generate_synthetic_data(15000)
    
    features = ['skill_match', 'reputation', 'success_rate', 'budget_fit', 'availability']
    
    X = df[features]
    y_score = df['total_score']
    y_prob = df['probability']
    
    X_train, X_test, ys_train, ys_test = train_test_split(X, y_score, test_size=0.2, random_state=42)
    _, _, yp_train, yp_test = train_test_split(X, y_prob, test_size=0.2, random_state=42)
    
    print("Training Match Score Regressor (Random Forest)...")
    score_model = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
    score_model.fit(X_train, ys_train)
    
    score_preds = score_model.predict(X_test)
    print(f"Score Model - MAE: {mean_absolute_error(ys_test, score_preds):.2f}, R2: {r2_score(ys_test, score_preds):.4f}")
    
    print("Training Success Probability Regressor (Random Forest)...")
    prob_model = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
    prob_model.fit(X_train, yp_train)
    
    prob_preds = prob_model.predict(X_test)
    print(f"Prob Model - MAE: {mean_absolute_error(yp_test, prob_preds):.4f}, R2: {r2_score(yp_test, prob_preds):.4f}")
    
    os.makedirs('models', exist_ok=True)
    joblib.dump(score_model, 'models/score_model.pkl')
    joblib.dump(prob_model, 'models/prob_model.pkl')
    print("Models saved successfully to models/ directory.")

if __name__ == "__main__":
    train_models()
