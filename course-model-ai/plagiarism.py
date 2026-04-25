import os
os.environ["USE_TF"] = "0"  # Prevent transformers from trying to load TensorFlow/Keras

import re
import pandas as pd
import numpy as np
from sqlalchemy import text
from sentence_transformers import SentenceTransformer, util
from data_loader import get_engine

print("[plagiarism] Loading SentenceTransformer model (all-MiniLM-L6-v2)...")
model = SentenceTransformer("all-MiniLM-L6-v2")

# Global index: list of dicts, one per published course
# Each entry: { "course_id": int, "sentences": [str], "embeddings": tensor }
course_index = []


def split_into_sentences(text_content: str) -> list:
    """
    Splits text into meaningful sentences/phrases.
    Filters out very short fragments (< 5 words) to avoid false positives.
    """
    if not text_content or not text_content.strip():
        return []

    # Split on sentence-ending punctuation, newlines, or semicolons
    raw = re.split(r'[.!?\n;]+', text_content)
    sentences = []
    for s in raw:
        s = s.strip()
        # Only keep sentences with at least 5 words (meaningful phrases)
        if s and len(s.split()) >= 5:
            sentences.append(s)
    return sentences


def build_index():
    """
    Fetches all published courses from the DB, splits their content into
    sentences, encodes each sentence, and stores them for comparison.
    """
    global course_index
    print("[plagiarism] Building sentence-level course index from database...")

    try:
        engine = get_engine()
    except Exception as e:
        print(f"[plagiarism] ERROR: Could not connect to database: {e}")
        course_index = []
        return

    query = text("""
        SELECT 
            c.id AS course_id, 
            COALESCE(c.title, '') AS course_title, 
            COALESCE(c.description, '') AS course_description,
            COALESCE(b.content, '') AS block_content
        FROM course c
        LEFT JOIN section s ON s.course_id = c.id
        LEFT JOIN block b ON b.section_id = s.id
        WHERE c.published = 1
    """)

    try:
        with engine.connect() as conn:
            df = pd.read_sql(query, conn)
    except Exception as e:
        print(f"[plagiarism] ERROR: Query failed: {e}")
        course_index = []
        return

    print(f"[plagiarism] Query returned {len(df)} rows")

    if df.empty:
        print("[plagiarism] WARNING: No published courses found in database.")
        course_index = []
        return

    # Group by course_id and concatenate all text
    grouped = df.groupby('course_id').agg({
        'course_title': 'first',
        'course_description': 'first',
        'block_content': lambda x: ' '.join(str(v) for v in x if v and str(v).strip())
    }).reset_index()

    new_index = []
    for _, row in grouped.iterrows():
        full_text = f"{row['course_title']}. {row['course_description']}. {row['block_content']}"
        sentences = split_into_sentences(full_text)

        if not sentences:
            continue

        embeddings = model.encode(sentences, convert_to_tensor=True)
        new_index.append({
            "course_id": int(row['course_id']),
            "title": row['course_title'],
            "sentences": sentences,
            "embeddings": embeddings
        })
        print(f"[plagiarism]   Course #{row['course_id']}: \"{row['course_title']}\" — {len(sentences)} sentences")

    course_index = new_index
    print(f"[plagiarism] Index built: {len(course_index)} courses indexed.")


def check_similarity(course_data: dict) -> dict:
    """
    Sentence-level plagiarism check.

    Algorithm:
        1. Split the new course into sentences.
        2. For each sentence, find its best match across ALL sentences in each existing course.
        3. A sentence is "plagiarized" if the cosine similarity >= 0.85 (very high = near-copy).
        4. The plagiarism score = (number of plagiarized sentences / total sentences) * 100.
        5. Flag as plagiarized if >= 70% of sentences are near-copies.

    This avoids false positives from topic similarity — only actual copied phrases trigger it.
    """
    # Refresh index
    build_index()

    if not course_index:
        print("[plagiarism] WARNING: Index is empty — nothing to compare against.")
        return {
            "is_plagiarized": False,
            "max_similarity": 0.0,
            "matched_course_id": None,
            "debug": "No published courses in database"
        }

    # Build full text from the new course
    title = course_data.get("title", "") or ""
    description = course_data.get("description", "") or ""

    blocks_text = []
    sections = course_data.get("sections", [])
    for sec in sections:
        for block in sec.get("blocks", []):
            content = block.get("content", "")
            if content and content.strip():
                blocks_text.append(content)

    full_text = f"{title}. {description}. {' '.join(blocks_text)}"
    new_sentences = split_into_sentences(full_text)

    print(f"[plagiarism] Checking course: \"{title}\"")
    print(f"[plagiarism]   New course has {len(new_sentences)} sentences")

    if not new_sentences:
        return {"is_plagiarized": False, "max_similarity": 0.0, "matched_course_id": None}

    # Encode new course sentences
    new_embeddings = model.encode(new_sentences, convert_to_tensor=True)

    SENTENCE_THRESHOLD = 0.85  # A sentence must be 85%+ similar to count as copied

    best_match_course_id = None
    best_match_percentage = 0.0

    for indexed_course in course_index:
        existing_embeddings = indexed_course["embeddings"]

        # Compute similarity matrix: (new_sentences x existing_sentences)
        sim_matrix = util.cos_sim(new_embeddings, existing_embeddings)

        # For each new sentence, get its max similarity to any existing sentence
        max_per_sentence = sim_matrix.max(dim=1).values  # shape: (num_new_sentences,)

        # Count how many new sentences have a near-copy in this existing course
        copied_count = int((max_per_sentence >= SENTENCE_THRESHOLD).sum().item())
        match_percentage = (copied_count / len(new_sentences)) * 100

        print(f"[plagiarism]   vs Course #{indexed_course['course_id']} (\"{indexed_course['title']}\"): "
              f"{copied_count}/{len(new_sentences)} sentences copied = {match_percentage:.1f}%")

        if match_percentage > best_match_percentage:
            best_match_percentage = match_percentage
            best_match_course_id = indexed_course["course_id"]

    PLAGIARISM_THRESHOLD = 70  # 70% of sentences must be copied
    is_plagiarized = best_match_percentage >= PLAGIARISM_THRESHOLD

    result = {
        "is_plagiarized": is_plagiarized,
        "max_similarity": round(best_match_percentage, 2),
        "matched_course_id": best_match_course_id if is_plagiarized else None
    }
    print(f"[plagiarism] Result: {result}")
    return result
