import pandas as pd
import numpy as np
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import precision_score, recall_score, roc_auc_score, average_precision_score
import joblib
import json
import os

def dcg_at_k(r, k):
    r = np.asfarray(r)[:k]
    if r.size:
        return np.sum(np.subtract(np.power(2, r), 1) / np.log2(np.arange(2, r.size + 2)))
    return 0.

def ndcg_at_k(r, k):
    idcg = dcg_at_k(sorted(r, reverse=True), k)
    if not idcg:
        return 0.
    return dcg_at_k(r, k) / idcg

def train():
    df = pd.read_csv('dataset.csv')
    
    feature_cols = ['skill_match_score', 'trust_score', 'experience_score', 'availability_score', 'similarity_score', 'location_score', 'kyc_bonus', 'liveness_bonus']
    
    X = df[feature_cols]
    y = df['was_hired']
    
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, stratify=y, random_state=42)
    
    # Scale only continuous features
    scaler = StandardScaler()
    continuous = feature_cols[:6]
    X_train_scaled = X_train.copy()
    X_test_scaled = X_test.copy()
    
    X_train_scaled[continuous] = scaler.fit_transform(X_train[continuous])
    X_test_scaled[continuous] = scaler.transform(X_test[continuous])
    
    pos_weight = sum(y_train == 0) / sum(y_train == 1) if sum(y_train == 1) > 0 else 1
    
    model = xgb.XGBClassifier(
        n_estimators=300,
        max_depth=5,
        learning_rate=0.05,
        subsample=0.8,
        colsample_bytree=0.8,
        scale_pos_weight=pos_weight,
        eval_metric='aucpr',
        early_stopping_rounds=20,
        random_state=42
    )
    
    model.fit(
        X_train_scaled, y_train,
        eval_set=[(X_test_scaled, y_test)],
        verbose=10
    )
    
    # Evaluate
    y_pred_proba = model.predict_proba(X_test_scaled)[:, 1]
    y_pred = model.predict(X_test_scaled)
    
    auc_roc = roc_auc_score(y_test, y_pred_proba)
    auc_pr = average_precision_score(y_test, y_pred_proba)
    
    # Simulate Precision@K and NDCG@K by grouping predictions into mock queries (e.g. by agency_id if available)
    # For simplicity across test set
    idx = np.argsort(y_pred_proba)[::-1]
    y_test_sorted = y_test.iloc[idx].values
    
    p_at_10 = sum(y_test_sorted[:10]) / 10 if len(y_test_sorted) >= 10 else 0
    p_at_20 = sum(y_test_sorted[:20]) / 20 if len(y_test_sorted) >= 20 else 0
    r_at_10 = sum(y_test_sorted[:10]) / sum(y_test) if sum(y_test) > 0 else 0
    
    ndcg_10 = ndcg_at_k(y_test_sorted, 10)
    
    report = {
        'auc_roc': float(auc_roc),
        'auc_pr': float(auc_pr),
        'precision@10': float(p_at_10),
        'precision@20': float(p_at_20),
        'recall@10': float(r_at_10),
        'ndcg@10': float(ndcg_10)
    }
    
    with open('training_report.json', 'w') as f:
        json.dump(report, f, indent=2)
        
    feature_importances = dict(zip(feature_cols, [float(v) for v in model.feature_importances_]))
    with open('feature_importances.json', 'w') as f:
        json.dump(feature_importances, f, indent=2)
        
    os.makedirs('model', exist_ok=True)
    joblib.dump({'model': model, 'scaler': scaler}, 'model/recommendation_model.joblib')
    
    print("Training complete. Model saved to model/recommendation_model.joblib")
    print("Report:", report)

if __name__ == '__main__':
    train()
