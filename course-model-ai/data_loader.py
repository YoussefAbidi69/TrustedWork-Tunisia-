"""
data_loader.py
--------------
Connects to your MySQL database, pulls courses with their vote scores,
comment counts, and report counts, and returns a clean pandas DataFrame
ready for training.

HOW TO USE:
    from data_loader import load_training_data
    df = load_training_data()
"""

import pandas as pd
from sqlalchemy import create_engine, text
from itertools import cycle
import random

# ── Database config ────────────────────────────────────────────────────────────
# Change these to match your actual DB credentials
DB_CONFIG = {
    "host":     "localhost",
    "port":     3306,
    "user":     "root",
    "password": "",
    "database": "trustedwork_community_db",
}

def get_engine():
    url = (
        f"mysql+pymysql://{DB_CONFIG['user']}:{DB_CONFIG['password']}"
        f"@{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}"
    )
    return create_engine(url)


def load_training_data() -> pd.DataFrame:
    """
    Returns a DataFrame with one row per published course.
    Columns:
        course_id       - int
        title           - str
        description     - str
        report_count    - int
        up_votes        - int
        down_votes      - int
        vote_score      - int  (up - down)
        comment_count   - int
        label           - str  ('high_quality' | 'average' | 'low_quality')
    """
    engine = get_engine()

    query = text("""
        SELECT
            c.id                                        AS course_id,
            COALESCE(c.title, '')                       AS title,
            COALESCE(c.description, '')                 AS description,
            c.report_count                              AS report_count,

            -- count upvotes and downvotes separately
            COUNT(CASE WHEN cv.type = 'UP'   THEN 1 END) AS up_votes,
            COUNT(CASE WHEN cv.type = 'DOWN' THEN 1 END) AS down_votes,

            -- net vote score = upvotes minus downvotes
            (COUNT(CASE WHEN cv.type = 'UP'   THEN 1 END)
           - COUNT(CASE WHEN cv.type = 'DOWN' THEN 1 END)) AS vote_score,

            -- total comments
            COUNT(DISTINCT cc.id)                       AS comment_count

        FROM course c
        LEFT JOIN course_vote    cv ON cv.course_id = c.id
        LEFT JOIN course_comment cc ON cc.course_id = c.id

        -- only train on published courses that have been seen by the community
        WHERE c.published = 1

        GROUP BY c.id, c.title, c.description, c.report_count

        -- need at least 1 vote to be a useful training example
        HAVING (up_votes + down_votes) >= 1
    """)

    with engine.connect() as conn:
        df = pd.read_sql(query, conn)

    if df.empty:
        raise ValueError(
            "No training data found. Make sure you have published courses "
            "with at least one vote in the database."
        )

    # ── Assign quality label based on vote score ───────────────────────────────
    # Adjust these thresholds to fit your community size.
    # With a small community, even score >= 2 is "high quality".
    df["label"] = df["vote_score"].apply(assign_label)

    print(f"[data_loader] Loaded {len(df)} courses")
    print(f"[data_loader] Label distribution:\n{df['label'].value_counts()}")

    return df


def assign_label(vote_score: int) -> str:
    """
    Converts a raw vote score into one of three quality classes.
    Thresholds:
        >= 5  → high_quality
        >= -2 → average
        <  -2 → low_quality
    """
    if vote_score >= 5:
        return "high_quality"
    elif vote_score >= -2:
        return "average"
    else:
        return "low_quality"


# ── Seed data for development (when DB is not available) ──────────────────────
# This lets your teammate run and test the model without a real DB.
# Remove this in production.
def load_seed_data() -> pd.DataFrame:
    """
    Returns fake data for development and testing.
    Call this instead of load_training_data() when you have no DB.
    """
    rng = random.Random(42)

    quality_specs = {
        "high_quality": {
            "title_prefixes": [
                "Complete", "Advanced", "Mastering", "Production Ready", "Practical",
                "Comprehensive", "Hands-On", "Modern", "Deep Dive into", "Professional",
            ],
            "topics": [
                ("React Hooks", "useState useEffect useMemo and custom hooks with real-world examples."),
                ("REST API Design", "Clean endpoints pagination validation authentication and versioning."),
                ("Python Data Analysis", "pandas numpy visualization and reproducible notebook workflows."),
                ("Docker Deployment", "Images containers volumes networking and production rollout tips."),
                ("TypeScript Advanced Types", "Generics unions mapped types utility types and inference patterns."),
                ("Machine Learning Pipelines", "Feature engineering model evaluation and deployment best practices."),
                ("SQL Optimization", "Indexes joins query plans and schema design for large datasets."),
                ("Testing Fundamentals", "Unit integration and end-to-end testing strategies with examples."),
            ],
            "vote_range": (7, 16),
            "comment_range": (4, 14),
            "report_range": (0, 1),
            "down_range": (0, 2),
        },
        "average": {
            "title_prefixes": [
                "Introduction to", "Getting Started with", "Practical", "Quick Guide to",
                "Basics of", "Overview of", "Working with", "Foundation of",
            ],
            "topics": [
                ("Git Branching", "Core commands for branching merging and working with remotes."),
                ("CSS Flexbox", "Layout basics alignment and responsive containers."),
                ("JavaScript Arrays", "Iteration mapping filtering and small utility functions."),
                ("HTTP Methods", "GET POST PUT DELETE and when each method is appropriate."),
                ("SQL Queries", "SELECT WHERE GROUP BY JOIN and filtering simple reports."),
                ("Angular Components", "Inputs outputs services and common app structure."),
                ("Debugging Skills", "Reading logs isolating bugs and using browser devtools."),
                ("Command Line Basics", "Files directories pipes and everyday terminal workflows."),
            ],
            "vote_range": (1, 5),
            "comment_range": (0, 5),
            "report_range": (0, 2),
            "down_range": (0, 3),
        },
        "low_quality": {
            "title_prefixes": [
                "quick notes on", "random thoughts about", "todo list for", "unfinished", "draft",
                "messy", "rough ideas on", "broken guide to",
            ],
            "topics": [
                ("React", "just use it and figure the rest out later."),
                ("Coding Tips", "some notes with no structure or examples."),
                ("javascript tutorial", "watch a video and copy the code."),
                ("Study Notes", "short reminders without context or steps."),
                ("Project Ideas", "unfinished thoughts and loose bullet points."),
                ("Database Stuff", "queries mentioned without explanation."),
                ("How to Code", "practice every day and hope it works."),
                ("General Notes", "random snippets collected from different places."),
            ],
            "vote_range": (-8, -1),
            "comment_range": (0, 2),
            "report_range": (1, 5),
            "down_range": (3, 10),
        },
    }

    rows = []
    course_id = 1

    for label, spec in quality_specs.items():
        topics = cycle(spec["topics"])
        prefixes = cycle(spec["title_prefixes"])
        for index in range(300):
            topic, base_description = next(topics)
            prefix = next(prefixes)
            vote_score = rng.randint(*spec["vote_range"])
            up_votes = max(0, vote_score + rng.randint(0, 3))
            down_votes = max(0, up_votes - vote_score)
            comment_count = rng.randint(*spec["comment_range"])
            report_count = rng.randint(*spec["report_range"])

            title = f"{prefix} {topic} #{index + 1}"
            description = (
                f"{base_description} "
                f"Lesson {index + 1} focuses on practical examples, exercises, and review notes."
            )

            if label == "low_quality":
                down_votes = max(down_votes, rng.randint(*spec["down_range"]))
                up_votes = rng.randint(0, max(1, down_votes - 1))
                vote_score = up_votes - down_votes
            elif label == "average" and vote_score < -2:
                vote_score = rng.randint(-2, 4)
                up_votes = max(up_votes, vote_score + rng.randint(0, 2))
                down_votes = max(0, up_votes - vote_score)

            rows.append(
                {
                    "course_id": course_id,
                    "title": title,
                    "description": description,
                    "report_count": report_count,
                    "up_votes": up_votes,
                    "down_votes": down_votes,
                    "vote_score": vote_score,
                    "comment_count": comment_count,
                }
            )
            course_id += 1

    df = pd.DataFrame(rows)
    df["label"] = df["vote_score"].apply(assign_label)

    print(f"[data_loader] Using seed data — {len(df)} courses")
    print(f"[data_loader] Label distribution:\n{df['label'].value_counts()}")

    return df
