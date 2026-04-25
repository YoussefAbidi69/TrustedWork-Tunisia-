"""
features.py
-----------
Converts raw course data (title, description, metadata) into a
numeric feature matrix that the classifier can train on.

Two types of features are combined:
    1. Text embedding  — a 384-dim vector from a sentence transformer
                         that captures the MEANING of the title + description
    2. Metadata        — simple numbers: word count, comment count, report count, etc.

HOW TO USE:
    from features import FeatureBuilder
    builder = FeatureBuilder()
    builder.fit(df)           # call once during training
    X = builder.transform(df) # call to get the feature matrix
"""

import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.decomposition import TruncatedSVD
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler


class FeatureBuilder:
    """
    Builds the combined feature matrix for training and inference.

    Two types of features combined:
        1. TF-IDF text features — captures important words in title+description
           relative to the whole course catalogue. No download needed.
        2. Metadata features    — word count, comment count, report count, votes.

    Must call fit() on training data before transform().
    Same interface as scikit-learn transformers.
    """

    def __init__(self):
        # Latent Semantic Analysis (LSA): TF-IDF + TruncatedSVD
        # Captures semantic meaning and context without the heavy PyTorch dependency.
        self.semantic_pipeline = make_pipeline(
            TfidfVectorizer(
                max_features=5000,
                ngram_range=(1, 3),  # up to trigrams for better context
                sublinear_tf=True,
                stop_words="english",
            ),
            TruncatedSVD(n_components=100, random_state=42) # Dense semantic vectors
        )
        self.scaler = StandardScaler()
        self._fitted = False
        print("[features] FeatureBuilder ready (TF-IDF + metadata)")

    def _build_texts(self, df: pd.DataFrame) -> list:
        """Combine title and description into one string per course."""
        return (
            df["title"].fillna("").str.strip()
            + " "
            + df["description"].fillna("").str.strip()
        ).tolist()

    def _build_meta(self, df: pd.DataFrame) -> np.ndarray:
        """Extract numeric metadata features."""
        meta = pd.DataFrame({
            "title_word_count":       df["title"].fillna("").str.split().str.len(),
            "description_word_count": df["description"].fillna("").str.split().str.len(),
            "comment_count":          df["comment_count"].fillna(0),
            "report_count":           df["report_count"].fillna(0),
            "up_votes":               df["up_votes"].fillna(0),
            "down_votes":             df["down_votes"].fillna(0),
        }).values.astype(float)
        return meta

    def fit(self, df: pd.DataFrame):
        """Learn vocabulary, semantic projection, and scaling from the training data."""
        texts = self._build_texts(df)
        meta  = self._build_meta(df)

        self.semantic_pipeline.fit(texts)
        self.scaler.fit(meta)
        self._fitted = True

        print("[features] LSA Semantic Pipeline ready (100 dimensions)")
        return self

    def transform(self, df: pd.DataFrame) -> np.ndarray:
        """
        Returns a 2D numpy array: one row per course, features as columns.
        Shape: (n_courses, tfidf_features + 6_metadata)
        """
        if not self._fitted:
            raise RuntimeError("Call fit() before transform()")

        texts = self._build_texts(df)
        meta  = self._build_meta(df)

        # Encode texts into dense semantic embeddings using LSA
        text_matrix = self.semantic_pipeline.transform(texts)
        meta_scaled  = self.scaler.transform(meta)

        X = np.hstack([text_matrix, meta_scaled])
        print(f"[features] Feature matrix shape: {X.shape}")
        return X

    def transform_single(self, title: str, description: str,
                         comment_count: int = 0, report_count: int = 0,
                         up_votes: int = 0, down_votes: int = 0) -> np.ndarray:
        """Score a single course at inference time. Returns shape (1, n_features)."""
        row = pd.DataFrame([{
            "title":         title,
            "description":   description,
            "comment_count": comment_count,
            "report_count":  report_count,
            "up_votes":      up_votes,
            "down_votes":    down_votes,
        }])
        return self.transform(row)
