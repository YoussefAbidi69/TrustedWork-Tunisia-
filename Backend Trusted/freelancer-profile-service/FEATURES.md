# Freelancer Profile Service — Fonctionnalités

**Module 02 — TrustedWork Tunisia**  
Spring Boot 3.2.5 | Java 17 | MySQL | Port `8082`  
Swagger UI : `http://localhost:8082/swagger-ui.html`

---

## Profils Freelancer

- Création, modification et suppression de profil
- Récupération par `userId` ou `profileId`
- Recherche multicritères : région, disponibilité, tarif horaire (min/max)
- Mise à jour du statut de disponibilité (`AVAILABLE`, `BUSY`, `NOT_AVAILABLE`)
- Visibilité du profil (`PUBLIC` / `PRIVATE`)
- **Score de complétude** (0–100%) calculé dynamiquement selon les sections remplies
- **Classement régional** : les freelancers sont classés par région selon leur score global
- Compteur de vues total exposé sur le profil

---

## Compétences

- Ajout, suppression de compétences par catégorie
- **Score d'authenticité** calculé automatiquement (exam score + endorsements)
- Mise à jour du score d'examen avec recalcul immédiat de l'authenticité
- **Détection de skill gaps** : identifie les compétences manquantes par rapport aux tendances du marché

---

## Endorsements

- Validation d'une compétence par un autre utilisateur (avec commentaire)
- Listing des endorsements par compétence
- Comptage des endorsements (impacte le score d'authenticité)

---

## Portfolio

- Ajout de projets avec titre, description, technologies, URL du projet, image, date de complétion
- **Épinglage** de projets mis en avant sur le profil public
- Désépinglage, modification et suppression de projets
- **Score de projet** calculé selon la complétude des informations renseignées (0–100)

---

## Expériences Professionnelles

- Ajout, modification, suppression d'expériences avec dates de début/fin
- Support du poste "en cours" (`isCurrent = true`)
- Calcul automatique de la durée avec label lisible (ex: "2 ans 3 mois")
- Durée totale cumulée en mois sur toute la carrière

---

## Formations

- Gestion des formations académiques (diplômes, universités)
- Listées par ordre chronologique décroissant (plus récent en premier)
- Ajout, modification, suppression

---

## Certifications

- Gestion des certifications professionnelles avec date d'expiration
- Ajout, modification, suppression
- Détection automatique des certifications expirées (via scheduler)

---

## Avis Clients

- Ajout d'un avis avec note (1–5 étoiles) et commentaire
- **Analyse de sentiment automatique** à la soumission (intégration ML Service)
- Note moyenne du profil
- Résumé complet : moyenne + distribution par nombre d'étoiles
- Réponse du freelancer à un avis reçu
- Masquage, suppression et restauration d'un avis (admin)

---

## Signalements

- Signalement d'un profil avec catégorie et description
- Workflow admin : `PENDING` → `CONFIRMED` / `REJECTED`
- Filtrage des signalements par statut ou par profil (admin)
- Listing de tous les signalements en attente (admin)

---

## Analytics Visites

- Enregistrement des vues de profil (visiteur anonyme ou connecté)
- Protection anti-spam et anti-self-view (un freelancer ne compte pas ses propres vues)
- Statistiques : total de vues, visiteurs uniques, vues sur les 7 derniers jours

---

## Notifications

- Notifications persistées en base de données
- Diffusion temps réel via **WebSocket (STOMP)**
- Récupération des notifications non lues
- Badge : compteur de notifications non lues
- Marquage de toutes les notifications comme lues

---

## Export

- Export du **CV en PDF** (iText) avec profil, compétences, expériences, formations, certifications
- Export de **tous les profils en Excel** (Apache POI) — réservé admin
- Export du **rapport admin en PDF** avec statistiques plateforme

---

## Intelligence Artificielle & ML

- **Trust Score** : prédit le niveau de confiance d'un freelancer (`HIGH` / `MEDIUM` / `LOW`) à partir de 10 features (complétude, skills, endorsements, portfolio, certifications, expérience, avis, note moyenne, KYC, 2FA)
- **Analyse de sentiment** : classifie un commentaire en `POSITIVE` / `NEGATIVE` avec score de confiance
- **Recommandation de carrière** : suggère un parcours de carrière selon les compétences actuelles
- **Détection de skill gaps** : identifie les compétences à acquérir pour progresser

---

## Scheduler (Jobs Planifiés)

4 jobs configurables dynamiquement depuis le backoffice :

| Job | Rôle |
|---|---|
| `recalculateAllSkillScores` | Recalcul des scores d'authenticité de toutes les compétences |
| `updateRegionalRankings` | Mise à jour des classements régionaux |
| `sendIncompleteProfileReminders` | Envoi d'emails de rappel aux freelancers avec profil incomplet |
| `checkCertificationExpiry` | Détection et notification des certifications expirées |

- Activation / désactivation de chaque job
- Modification de l'expression cron et de l'intervalle
- **Déclenchement manuel immédiat** via API (utile pour la démo ou les tests admin)

---

## Stack Technique

| Composant | Technologie |
|---|---|
| Framework | Spring Boot 3.2.5, Java 17 |
| Persistance | MySQL, JPA / Hibernate |
| Sécurité | JWT (via Identity Provider), Spring Security |
| Temps réel | WebSocket (STOMP) |
| Inter-services | OpenFeign |
| Notifications | Gmail SMTP, Twilio SMS |
| Export | iText (PDF), Apache POI (Excel) |
| ML | Appels REST vers Flask ML Service (port 5000) |
| Documentation | SpringDoc OpenAPI 3 — Swagger UI |
