from sentence_transformers import SentenceTransformer
import numpy as np
import pandas as pd

class EmbeddingService:
    def __init__(self):
        print("🔄 Chargement du modèle d'embeddings (multilingue)...")
        # Modèle français/anglais pour les embeddings
        self.model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')
        self.freelancer_embeddings = None
        self.freelancers_df = None
        print("✅ Modèle d'embeddings chargé")
        
    def load_freelancers(self, freelancers_df):
        """Charge les freelancers et calcule leurs embeddings"""
        print("🔄 Calcul des embeddings des freelancers...")
        self.freelancers_df = freelancers_df
        
        # Créer une description textuelle pour chaque freelancer
        descriptions = []
        for _, fl in freelancers_df.iterrows():
            desc = f"{fl['category']} spécialisé en {fl['skills_str']}. "
            desc += f"Expérience: {fl['experience_level']} ({fl['experience_years']} ans). "
            desc += f"Note: {fl['avg_rating']}/5. "
            desc += f"Portfolio: {fl['portfolio_score']}/10."
            descriptions.append(desc)
        
        # Calculer les embeddings
        self.freelancer_embeddings = self.model.encode(descriptions, show_progress_bar=True)
        print(f"✅ Embeddings calculés: {self.freelancer_embeddings.shape}")
        return self.freelancer_embeddings
    
    def get_similarity_scores(self, project_description, top_k=50):
        """Calcule la similarité sémantique entre projet et freelancers"""
        print(f"🔄 Calcul de similarité pour: '{project_description[:50]}...'")
        project_embedding = self.model.encode([project_description])
        similarities = np.dot(self.freelancer_embeddings, project_embedding.T).flatten()
        
        # Normaliser entre 0 et 1
        similarities = (similarities - similarities.min()) / (similarities.max() - similarities.min() + 1e-8)
        
        # Retourner les top_k freelancers avec leurs scores
        top_indices = np.argsort(similarities)[-top_k:][::-1]
        results = []
        for idx in top_indices:
            results.append({
                'freelancer_id': self.freelancers_df.iloc[idx]['freelancer_id'],
                'semantic_score': float(similarities[idx])
            })
        print(f"✅ {len(results)} freelancers similaires trouvés")
        return results