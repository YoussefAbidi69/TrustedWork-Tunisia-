# ML-Powered Freelancer Recommendation System (Module 07)

## 1. Overview
The Recommendation System provides Agency Leads with a data-driven approach to finding the best freelancers to invite to their agency. The model uses a multi-featured scoring algorithm that takes into account the agency's needs, existing team composition, and freelancer attributes.

## 2. Technical Stack
- **Backend:** Spring Boot 3, Spring Data JPA, MySQL
- **Frontend:** Angular 18
- **Core Concept:** Weighted Scoring Model (Deterministic Rule-Based)

## 3. Algorithm & Feature Engineering
The model ranks freelancers using six normalized features (0.0 to 1.0). The final score is a weighted sum:

`Recommendation Score = (F1 × 0.35) + (F2 × 0.25) + (F3 × 0.20) + (F4 × 0.10) + (F5 × 0.05) + (F6 × 0.05)`

### Feature Breakdown
1. **F1 - Skill Match Score (35%)**: Calculates the Jaccard similarity between the candidate's skills and the agency's missing skills. If no missing skills are identified, it matches against the union of all current members' skills.
2. **F2 - Trust Score (25%)**: Normalizes the base Trust Level (1-5) and applies a bonus if the user's KYC is fully approved and liveness is verified.
3. **F3 - Availability Score (20%)**: Evaluates availability strictly (FULL_TIME=1.0, PART_TIME=0.6, WEEKENDS=0.3).
4. **F4 - Experience Score (10%)**: Extracts numeric years of experience using regex (`(\d+)\s*year`). Capped at 7+ years (1.0).
5. **F5 - Team Similarity Score (5%)**: Computes average Jaccard similarity with existing members. Extremely high similarity (>0.9) is penalized to ensure team diversity.
6. **F6 - Location Score (5%)**: Checks exact city match (1.0), general country match (0.5), or mismatch (0.1).

## 4. Optimization & Architecture
- **Async Execution:** The heavy lifting (fetching all eligible freelancers and recomputing scores) is offloaded to a background thread (`@Async`).
- **Caching:** Computed scores are persisted in `freelancer_recommendation_scores` and reused for 24 hours to prevent redundant database strain.
- **Explainability:** Each score computation generates a human-readable `explanation` string, providing full transparency to the agency lead.

## 5. Offline ML Evaluation Strategy
For future model tuning, you can evaluate the performance using historical invitation data:

1. **Precision@K**: Out of the top K freelancers recommended by the algorithm, how many were actually invited by the agency lead?
2. **Recall@K**: Out of all the freelancers ever successfully invited to the agency, what percentage were ranked in the top K by the model?
3. **NDCG@K (Normalized Discounted Cumulative Gain)**: Uses the order of recommendations and assigns a relevance score (e.g., 1 for ACCEPTED invite, 0 otherwise) to measure how well the model ranks truly relevant candidates at the top.

To perform this evaluation, join `freelancer_recommendation_scores` against `agency_invitations` filtered by `status='ACCEPTED'`.

## 6. How to Run & Test
1. Execute the migration script `create_recommendation_scores_table.sql` on your database.
2. Execute the synthetic dataset seed script `seed_ml_dataset.sql` to populate 200 diverse freelancers and pre-computed scores.
3. Start the Spring Boot backend (`mvn spring-boot:run`).
4. Start the Angular frontoffice (`ng serve`).
5. Log in as a User who owns an agency (LEAD role).
6. Navigate to `Agencies` -> `[Your Agency]` -> `Trouver des collaborateurs` (`/app/agencies/:id/members`).
7. You can now use the filters, view explanations, and invite top-ranked freelancers.
