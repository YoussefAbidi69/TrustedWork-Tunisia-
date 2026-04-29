# 🚀 TrustedWork - Module 06: Engagement & Gamification

Welcome to the **Engagement & Gamification** module of TrustedWork. This module is designed to maximize user retention and community activity through state-of-the-art AI predictions, behavioral analytics, and a premium gamified experience.

---

## 🌟 Key Features

### 🧠 AI & Machine Learning
*   **Churn Prediction**: Real-time analysis of user behavior to predict the risk of disengagement (using a Random Forest model).
*   **Smart Recommendations**: Personalized AI-driven suggestions (powered by Groq/LLM) to guide users back to active participation.
*   **Intelligent Fallback**: Robust local logic that ensures continuous service even if the AI microservice is offline.

### 📊 Advanced Analytics
*   **Engagement Score**: A composite metric reflecting overall user participation.
*   **Influence Score**: A sophisticated calculation incorporating event attendance, challenge completions, and badge acquisition.
*   **Behavioral Tracking**: Monitoring streaks and inactivity periods to trigger proactive retention actions.

### 🎮 Gamification Engine
*   **Leaderboard Premium**: A futuristic, glassmorphism-style hall of fame with animated medals (🥇, 🥈, 🥉) for top contributors.
*   **Dynamic Badges**: Recognition system for various milestones and achievements.
*   **Streak System**: Encouraging daily participation through activity streaks and XP bonuses.

### 🏢 Dual Administration
*   **Frontoffice**: A stunning, high-performance interface for users to track their progress and receive AI tips.
*   **Backoffice**: A dedicated admin panel to monitor community health, visualize churn risks, and manage engagement strategies.

---

## 🛠️ Technology Stack

| Component | Technology |
| :--- | :--- |
| **Backend** | Java 22, Spring Boot 3.x, PostgreSQL, Maven |
| **Frontend** | Angular 17, Vanilla CSS (Premium Custom Styles) |
| **AI Service** | Python 3.10+, Flask, Scikit-learn (Random Forest) |
| **Architecture** | Microservices-ready, RESTful API |

---

## 📂 Project Structure (Module 06)

*   **`backend/`**: Spring Boot services, Repositories, and ML Integration logic.
*   **`frontend/`**: Angular components (Gamification, Leaderboard, Growth Admin).
*   **`ai-service/`**: Python scripts for dataset generation, model training, and prediction API.

---

## 🚀 Quick Start

### 1. AI Service (Python)
```bash
cd ai-service
pip install -r requirements.txt
python app.py
```

### 2. Backend (Java)
```bash
mvn clean install
mvn spring-boot:run
```

### 3. Frontend (Angular)
```bash
npm install
ng serve
```

---

## 🎨 UI/UX Design Philosophy
This module uses a **Premium Dark Theme** with:
*   **Glassmorphism**: Frosted glass effects for modern UI components.
*   **Micro-animations**: Smooth transitions and interactive feedback.
*   **Data Visualization**: Clear, color-coded badges and progress indicators.

---

## 🛡️ Quality Standards
*   **Coverage**: Optimized unit tests for all service implementations.
*   **Clean Code**: Adherence to SonarQube quality gates and Java 22 standards.

---
*Developed with ❤️ for the TrustedWork Ecosystem.*
