# 📦 Module 08 — Project Management & Delivery Intelligence

> **TrustedWork Tunisia** — Plateforme Freelance Microservices  
> Année Universitaire 2024-2025

---

## 🧭 Vue d'ensemble

Le **Module 08** est le maillon d'exécution de l'écosystème TrustedWork. Il intervient automatiquement après la signature d'un contrat (Module 05) et fournit un espace de travail collaboratif complet où client et freelancer suivent l'avancement du projet en temps réel, assistés par un moteur d'intelligence artificielle de détection des risques.

| Propriété | Valeur |
|-----------|--------|
| **Service** | `ms-project-service` |
| **Port** | `8090` |
| **Base de données** | `trustedwork_project_db` |
| **Package de base** | `tn.esprit.msprojectservice` |
| **Swagger UI** | `http://localhost:8090/swagger-ui.html` |

---

## 🛠️ Stack Technique

| Couche | Technologie |
|--------|-------------|
| Backend | Spring Boot 3 |
| Sécurité | Spring Security + JWT |
| Persistance | JPA / Hibernate + MySQL |
| Frontend | Angular 18 |
| Documentation API | Swagger / OpenAPI 3 (springdoc 2.5.0) |
| Machine Learning | scikit-learn (Python) → PMML → `jpmml-evaluator` (`pmml-evaluator-metro:1.7.7`) |
| Export | iText 7 (PDF) + CSV natif |
| Build | Maven |
| Lombok | `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` |
| AOP | Spring AOP (`spring-boot-starter-aop`) |
| Mailing | Spring Mail (Gmail SMTP) + Thymeleaf |

---

## 🗂️ Structure des Packages

```
tn.esprit.msprojectservice/
├── MsProjectServiceApplication.java        (@SpringBootApplication + @EnableScheduling)
├── controllers/
│   ├── ProjectRestController.java
│   ├── TaskRestController.java
│   ├── SubTaskRestController.java
│   ├── DeliverableRestController.java
│   ├── ProgressReportRestController.java
│   ├── RiskSignalRestController.java
│   ├── NotificationRestController.java
│   └── ExportRestController.java
├── entities/
│   ├── Project.java
│   ├── Task.java
│   ├── SubTask.java
│   ├── Deliverable.java
│   ├── ProgressReport.java
│   ├── DeliveryRiskSignal.java
│   ├── Notification.java
│   └── enums/  (ProjectStatus, TaskStatus, TaskPriority, DeliverableStatus,
│                 RiskType, RiskSeverity, NotificationType)
├── dto/
│   ├── ProjectDTO.java
│   ├── TaskDTO.java
│   ├── SubTaskDTO.java
│   ├── DeliverableDTO.java
│   ├── ProgressReportDTO.java
│   ├── DeliveryRiskSignalDTO.java
│   └── NotificationDTO.java
├── repositories/
│   ├── IProjectRepository.java
│   ├── ITaskRepository.java
│   ├── ISubTaskRepository.java
│   ├── IDeliverableRepository.java
│   ├── IProgressReportRepository.java
│   ├── IRiskSignalRepository.java
│   └── INotificationRepository.java
├── services/
│   ├── IProjectService.java + ProjectServiceImpl.java
│   ├── ITaskService.java + TaskServiceImpl.java
│   ├── ISubTaskService.java + SubTaskServiceImpl.java
│   ├── IDeliverableService.java + DeliverableServiceImpl.java
│   ├── IProgressReportService.java + ProgressReportServiceImpl.java
│   ├── IRiskSignalService.java + RiskSignalServiceImpl.java
│   ├── INotificationService.java + NotificationServiceImpl.java
│   ├── IMailService.java + MailServiceImpl.java
│   └── IExportService.java + ExportServiceImpl.java
├── scheduler/
│   └── DeliveryRiskScheduler.java          (@Scheduled — IA quotidienne + hebdo)
├── aspect/
│   ├── LoggingAspect.java                  (@Before + @After)
│   └── PerformanceAspect.java              (@Around)
├── feign/
│   └── UserServiceClient.java              (→ Module 01, port 8081)
└── config/
    └── GlobalExceptionHandler.java
```

---

## 🗃️ Les 6 Entités JPA

### 1. `Project` — Le Projet (entité centrale)

Créé automatiquement depuis Module 05 après signature du contrat.

| Attribut | Type | Description |
|----------|------|-------------|
| `id` | `Long` | PK auto-générée |
| `title` | `String` | Titre du projet |
| `description` | `String (TEXT)` | Description détaillée |
| `status` | `ProjectStatus` | ACTIVE / ON_HOLD / COMPLETED / CANCELLED |
| `contractId` | `Long` | ID contrat (Module 05) |
| `clientId` | `Long` | ID client (JWT) |
| `freelancerId` | `Long` | ID freelancer (JWT) |
| `startDate` | `LocalDate` | Date de début |
| `endDate` | `LocalDate` | Date de fin prévue |
| `completionRate` | `int` | 0–100, calculé automatiquement |
| `budget` | `Double` | Budget en dinars tunisiens |
| `createdAt` | `LocalDateTime` | Auto via `@PrePersist` |
| `updatedAt` | `LocalDateTime` | Auto via `@PreUpdate` |

### 2. `Task` — La Tâche

Pilote le **Kanban Angular** via son attribut `status`.

| Attribut | Type | Description |
|----------|------|-------------|
| `status` | `TaskStatus` | **TODO / IN_PROGRESS / IN_REVIEW / DONE** |
| `priority` | `TaskPriority` | LOW / MEDIUM / HIGH / CRITICAL |
| `assigneeId` | `Long` | ID membre assigné |
| `deadline` | `LocalDate` | Date limite |
| `estimatedHours` | `Integer` | Durée estimée |
| `actualHours` | `Integer` | Durée réelle |

> Chaque changement de statut recalcule automatiquement le `completionRate` du projet parent.

### 3. `SubTask` — La Sous-Tâche

Décomposition granulaire. Champ `done` (boolean) uniquement.

### 4. `Deliverable` — Le Livrable

Workflow : `SUBMITTED` → `APPROVED` / `REJECTED` avec commentaire client.

| Attribut notable | Description |
|-----------------|-------------|
| `fileUrl` | Lien Google Drive, Dropbox, etc. |
| `reviewComment` | Commentaire client (approbation ou motif de rejet) |
| `reviewedAt` | Date de décision |

### 5. `ProgressReport` — Rapport de Progression

Snapshot automatique de l'état du projet. Généré chaque lundi à 8h via `@Scheduled`.

### 6. `DeliveryRiskSignal` — Signal de Risque IA

Jamais créé manuellement. Généré uniquement par le scheduler IA.

| Attribut | Description |
|----------|-------------|
| `riskType` | DELAY_RISK / BOTTLENECK / INACTIVITY / SCOPE_CREEP |
| `severity` | LOW / MEDIUM / HIGH / CRITICAL |
| `resolved` | `false` = actif, `true` = résolu |

---

## 🤖 Features IA — Delivery Intelligence

Le moteur IA s'exécute quotidiennement via `@Scheduled` et analyse chaque projet `ACTIVE`.

### 1. DELAY_RISK — Détection de retard

```
DeliveryRisk = (timeRatio × 0.35) + (taskCompletionRate × 0.35) + (assigneeHistoryScore × 0.30)
```

Seuil de déclenchement : `score >= 0.6`

Deux modèles ML intégrés via PMML (`jpmml-evaluator`) :
- **Delivery Risk Classifier** — Random Forest, AUC-ROC 0.91, précision 86%
- **Task Duration Regressor** — Détecte le planning fallacy à la création d'une tâche

### 2. BOTTLENECK — Tâche bloquée

Tâche en `IN_PROGRESS` ou `IN_REVIEW` sans modification depuis > 3 jours.
Sévérité : MEDIUM (3–5 jours) / HIGH (> 6 jours).

### 3. INACTIVITY — Inactivité projet

Aucun changement de statut de tâche depuis > 5 jours.
Sévérité : MEDIUM (5–9 jours) / HIGH (> 10 jours).

### 4. SCOPE_CREEP — Dérive du périmètre

```
scopeRatio = addedTasks / initialTaskCount
```
Seuil : > 30% de tâches ajoutées après `startDate`.
Sévérité : MEDIUM (30–60%) / HIGH (> 60%).

### Tableau de sévérité automatique

| Score | Sévérité |
|-------|----------|
| ≥ 0.85 | CRITICAL |
| ≥ 0.75 | HIGH |
| ≥ 0.60 | MEDIUM |
| < 0.60 | LOW |

---

## ⏰ Scheduler — Exécutions automatiques

| Cron | Fréquence | Action |
|------|-----------|--------|
| `0 0 7 * * *` | Tous les jours à 7h | Envoi notifications (deadline 24h, livrables, tâches bloquées) |
| `0 0 8 * * *` | Tous les jours à 8h | Analyse IA de tous les projets ACTIVE |
| `0 0 8 * * MON` | Chaque lundi à 8h | Génération des rapports hebdomadaires + email |

---

## 📧 Notifications

### Types de notifications internes

| Type | Déclencheur | Destinataire |
|------|-------------|-------------|
| `DEADLINE_24H` | Deadline dans 24h | Freelancer + Client |
| `DELIVERABLE_PENDING` | Livrable en attente | Client |
| `DELIVERABLE_REVIEWED` | Livrable approuvé/rejeté | Freelancer |
| `TASK_BLOCKED` | Tâche bloquée N jours | Freelancer assigné |
| `WEEKLY_REPORT` | Rapport hebdo généré | Client + Freelancer |
| `RISK_DETECTED` | Nouveau signal IA | Client + Freelancer |

### Emails automatiques (Thymeleaf + Gmail SMTP)

- **Rapport hebdomadaire** — via `DeliveryRiskScheduler` chaque lundi
- **Notification livrable reviewé** — via `DeliverableServiceImpl` après `reviewDeliverable()`

---

## 🔌 AOP

| Aspect | Annotation | Cible | Description |
|--------|-----------|-------|-------------|
| `LoggingAspect` | `@Before` + `@After` | Couche service | Log des appels et paramètres |
| `PerformanceAspect` | `@Around` | Couche service | Mesure du temps d'exécution |

Seuil d'alerte performance : **> 1000 ms**

---

## 📡 Endpoints REST API (38 endpoints)

### Projects

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/projects` | Créer un projet |
| `GET` | `/api/projects/{id}` | Détails d'un projet |
| `GET` | `/api/projects/contract/{contractId}` | Projet par contrat |
| `GET` | `/api/projects/user/{userId}` | Projets d'un utilisateur |
| `GET` | `/api/projects` | Tous les projets |
| `PUT` | `/api/projects/{id}` | Modifier un projet |
| `PATCH` | `/api/projects/{id}/status` | Changer le statut |
| `DELETE` | `/api/projects/{id}` | Supprimer un projet |

### Tasks & Kanban

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/projects/{projectId}/tasks` | Créer une tâche |
| `GET` | `/api/projects/{projectId}/tasks` | Lister les tâches (Kanban) |
| `GET` | `/api/tasks/{id}` | Détails d'une tâche |
| `PUT` | `/api/tasks/{id}` | Modifier une tâche |
| `PATCH` | `/api/tasks/{id}/status` | Déplacer carte Kanban |
| `PATCH` | `/api/tasks/{id}/assign` | Assigner une tâche |
| `DELETE` | `/api/tasks/{id}` | Supprimer une tâche |

### SubTasks

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/tasks/{taskId}/subtasks` | Ajouter une sous-tâche |
| `GET` | `/api/tasks/{taskId}/subtasks` | Lister les sous-tâches |
| `PATCH` | `/api/subtasks/{id}/toggle` | Cocher / décocher |
| `DELETE` | `/api/subtasks/{id}` | Supprimer |

### Deliverables

| Méthode | URL | Description |
|---------|-----|-------------|
| `POST` | `/api/projects/{projectId}/deliverables` | Soumettre un livrable |
| `GET` | `/api/projects/{projectId}/deliverables` | Lister les livrables |
| `GET` | `/api/deliverables/{id}` | Détails d'un livrable |
| `PATCH` | `/api/deliverables/{id}/review` | Approuver ou rejeter |
| `DELETE` | `/api/deliverables/{id}` | Supprimer |

### Progress Reports

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET` | `/api/projects/{projectId}/report` | Générer un rapport |
| `GET` | `/api/projects/{projectId}/reports` | Historique des rapports |

### Risk Signals (IA)

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET` | `/api/projects/{projectId}/risks` | Signaux actifs |
| `GET` | `/api/projects/{projectId}/risks/latest` | Signal le plus critique |
| `POST` | `/api/projects/{projectId}/risks/analyze` | Lancer l'analyse IA manuellement |
| `PATCH` | `/api/risks/{id}/resolve` | Résoudre un signal |

### Notifications

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET` | `/api/notifications/user/{userId}` | Toutes les notifications |
| `GET` | `/api/notifications/user/{userId}/unread` | Non lues |
| `GET` | `/api/notifications/user/{userId}/unread/count` | Compteur (badge Angular) |
| `PATCH` | `/api/notifications/{id}/read` | Marquer comme lue |
| `PATCH` | `/api/notifications/user/{userId}/read-all` | Tout marquer comme lu |

### Export

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET` | `/api/projects/{projectId}/export/pdf` | Rapport PDF complet |
| `GET` | `/api/projects/{projectId}/export/csv` | Tâches CSV (séparateur `;`) |

---

## ⚙️ Configuration

### `application.properties`

```properties
server.port=8090
spring.datasource.url=jdbc:mysql://localhost:3306/trustedwork_project_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
spring.application.name=ms-project-service
```

### Dépendances principales (`pom.xml`)

| Dépendance | Version | Usage |
|------------|---------|-------|
| `spring-boot-starter-web` | — | REST API |
| `spring-boot-starter-data-jpa` | — | JPA/Hibernate |
| `mysql-connector-j` | — | Driver MySQL |
| `lombok` | — | Réduction boilerplate |
| `springdoc-openapi-starter-webmvc-ui` | `2.5.0` | Swagger UI |
| `spring-boot-starter-aop` | — | AOP Logging + Performance |
| `spring-boot-starter-mail` | — | Gmail SMTP |
| `spring-boot-starter-thymeleaf` | — | Templates email HTML |
| `pmml-evaluator-metro` | `1.7.7` | Modèles ML PMML |
| `itext7-core` | `8.0.3` | Export PDF |

---

## 🚀 Lancement

### Prérequis

- Java 17+
- Maven 3.8+
- MySQL lancé (XAMPP, WAMP, ou MySQL Workbench)
- IntelliJ IDEA (recommandé)

### Démarrage

```bash
# Cloner le projet
git clone <url-du-repo>
cd ms-project-service

# Lancer via Maven
mvn spring-boot:run
```

La base de données `trustedwork_project_db` est créée automatiquement grâce à `createDatabaseIfNotExist=true`.

### Accès Swagger

```
http://localhost:8090/swagger-ui.html
```

---

## 🧪 Scénario de Test Rapide

```bash
# 1. Créer un projet
POST /api/projects

# 2. Créer une tâche
POST /api/projects/1/tasks

# 3. Simuler le Kanban
PATCH /api/tasks/1/status?status=IN_PROGRESS
PATCH /api/tasks/1/status?status=DONE

# 4. Soumettre un livrable
POST /api/projects/1/deliverables

# 5. Reviewer le livrable
PATCH /api/deliverables/1/review?status=APPROVED&reviewComment=Excellent !

# 6. Lancer l'analyse IA manuellement
POST /api/projects/1/risks/analyze

# 7. Exporter le rapport
GET /api/projects/1/export/pdf
```

**Simuler un bottleneck pour tester l'IA :**

```sql
UPDATE task SET updated_at = DATE_SUB(NOW(), INTERVAL 5 DAY) WHERE id = 2;
```

---

## 🔗 Intégration avec les autres modules

| Module | Port | Interaction |
|--------|------|-------------|
| **Module 01** — Identity & Access | `8081` | Feign Client `UserServiceClient` → récupération des emails utilisateurs |
| **Module 05** — Contracting | `8085` | REST `POST /api/projects` déclenché après signature du contrat |

> Le seul élément partagé entre tous les modules est le `userId` extrait du token JWT. Aucun module n'accède directement à la base de données d'un autre module.

---

## ✅ Checklist Exigences Académiques

| Exigence | Statut |
|----------|--------|
| JPA Entities + Enums | ✅ 7 entités + 7 enums |
| DTOs avec mapping manuel (pas MapStruct) | ✅ 7 DTOs |
| Repository `JpaRepository` + `@Query` JPQL | ✅ 7 repositories |
| Service Interface + Impl + `@Autowired` | ✅ 9 services |
| REST Controller GET/POST/PUT/PATCH/DELETE | ✅ 8 controllers, 38 endpoints |
| Swagger `@Tag` + `@Operation` | ✅ Sur chaque endpoint |
| AOP Logging `@Before` + `@After` | ✅ `LoggingAspect` |
| AOP Performance `@Around` | ✅ `PerformanceAspect` |
| `@Scheduled` IA | ✅ `DeliveryRiskScheduler` (quotidien + hebdo) |
| Feature IA (DELAY, BOTTLENECK, INACTIVITY, SCOPE_CREEP) | ✅ 4 détections |
| Modèles ML PMML intégrés | ✅ 2 modèles Random Forest |
| Notifications (internes + email) | ✅ 6 types + Thymeleaf |
| Export PDF/CSV | ✅ iText 7 + CSV natif |

---

## 👨‍💻 Auteur

Projet académique — **TrustedWork Tunisia**  
Module 08 — Project Management & Delivery Intelligence  
Année Universitaire **2024-2025**

---

*ms-project-service | Port 8090 | Spring Boot 3 | Angular 18 | MySQL*
