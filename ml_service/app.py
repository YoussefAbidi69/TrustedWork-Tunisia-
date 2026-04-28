from flask import Flask, request, jsonify
import joblib
import pandas as pd
import os

app = Flask(__name__)

model_path = os.path.join(os.path.dirname(__file__), 'model', 'recommendation_model.joblib')
pipeline = None

def load_model():
    global pipeline
    if os.path.exists(model_path):
        pipeline = joblib.load(model_path)
        print("Model loaded successfully.")
    else:
        print(f"Warning: Model not found at {model_path}. Please train the model first.")

def build_explanation(features):
    skill_pct = int(features['skill_match_score'] * 100)
    trust_label = "High" if features['trust_score'] >= 0.8 else "Medium" if features['trust_score'] >= 0.4 else "Low"
    avail_map = {1.0: "FULL_TIME", 0.6: "PART_TIME", 0.3: "WEEKENDS", 0.0: "UNAVAILABLE"}
    # find closest key
    avail_score = features['availability_score']
    closest_key = min(avail_map.keys(), key=lambda k: abs(k - avail_score))
    avail = avail_map.get(closest_key, "UNKNOWN")
    exp_years = round(features['experience_score'] * 10)
    sim_pct = int(features['similarity_score'] * 100)
    loc = "match" if features['location_score'] >= 0.9 else "nearby" if features['location_score'] >= 0.4 else "remote"
    return f"Skill match: {skill_pct}% | Trust: {trust_label} | Availability: {avail} | Experience: {exp_years} years | Similar to team: {sim_pct}% | Location: ({loc})"

@app.route('/recommend', methods=['POST'])
def recommend():
    if pipeline is None:
        return jsonify({"error": "Model not loaded"}), 503

    data = request.json
    agency_id = data.get('agency_id')
    candidates = data.get('candidates', [])

    if not candidates:
        return jsonify({"agency_id": agency_id, "recommendations": []})

    # Prepare features dataframe
    df = pd.DataFrame(candidates)
    
    # Extract feature columns exactly as trained
    feature_cols = ['skill_match_score', 'trust_score', 'experience_score', 'availability_score', 'similarity_score', 'location_score', 'kyc_bonus', 'liveness_bonus']
    
    # Check if all columns exist
    missing = [c for c in feature_cols if c not in df.columns]
    if missing:
        return jsonify({"error": f"Missing features: {missing}"}), 400

    X = df[feature_cols]

    # Predict probabilities (class 1)
    # The pipeline contains scaler and model, or just model if XGBoost handles scaling natively
    try:
        scaler = pipeline.get('scaler')
        model = pipeline.get('model')
        
        if scaler:
            X_scaled = X.copy()
            # Assuming the first 6 features are continuous, last 2 binary
            continuous = feature_cols[:6]
            X_scaled[continuous] = scaler.transform(X[continuous])
            scores = model.predict_proba(X_scaled)[:, 1]
        else:
            scores = model.predict_proba(X)[:, 1]
    except Exception as e:
        # Fallback if pipeline format is different
        scores = pipeline.predict_proba(X)[:, 1]

    recommendations = []
    for i, row in df.iterrows():
        score = float(scores[i])
        freelancer_id = int(row['freelancer_id'])
        
        # Build original features for explanation
        feats = {col: float(row[col]) for col in feature_cols}
        explanation = build_explanation(feats)
        
        recommendations.append({
            "freelancer_id": freelancer_id,
            "recommendation_score": score,
            "skill_match_score": feats['skill_match_score'],
            "trust_score": feats['trust_score'],
            "experience_score": feats['experience_score'],
            "availability_score": feats['availability_score'],
            "similarity_score": feats['similarity_score'],
            "location_score": feats['location_score'],
            "explanation": explanation
        })

    # Sort by recommendation score descending
    recommendations.sort(key=lambda x: x['recommendation_score'], reverse=True)

    return jsonify({
        "agency_id": agency_id,
        "recommendations": recommendations
    })

if __name__ == '__main__':
    load_model()
    app.run(host='0.0.0.0', port=5001)
