# Module 08 — Project Management & Delivery Intelligence
## Documentation Technique Complète — TrustedWork Tunisia

**Service :** `ms-project-service`  
**Port :** `8089` (context-path : `/project`)  
**Base de données :** `trustedwork_project_db`  
**Package de base :** `tn.esprit.msprojectservice`  
**Stack :** Spring Boot 3 | Angular 18 | MySQL | JPA/Hibernate | Swagger/OpenAPI | Maven  
**Date :** Avril 2026 — Année Universitaire 2024-2025

---

## Table des matières

1. [Vue d'ensemble du Module](#1-vue-densemble-du-module)
2. [Architecture Technique](#2-architecture-technique)
3. [Configuration — application.properties](#3-configuration--applicationproperties)
4. [Dépendances Maven — pom.xml](#4-dépendances-maven--pomxml)
5. [Les 7 Entités JPA](#5-les-7-entités-jpa)
6. [Les 7 Enums](#6-les-7-enums)
7. [Les DTOs](#7-les-dtos)
8. [Les Repositories](#8-les-repositories)
9. [Les Services](#9-les-services)
10. [Les Controllers REST — 43 Endpoints](#10-les-controllers-rest--43-endpoints)
11. [Sécurité JWT](#11-sécurité-jwt)
12. [AOP — Logging et Performance](#12-aop--logging-et-performance)
13. [Scheduler IA — DeliveryRiskScheduler](#13-scheduler-ia--deliveryriskscheduler)
14. [Moteur ML — Random Forest PMML](#14-moteur-ml--random-forest-pmml)
15. [Burndown Chart](#15-burndown-chart)
16. [Mailing — Thymeleaf + Gmail SMTP](#16-mailing--thymeleaf--gmail-smtp)
17. [Export PDF / CSV](#17-export-pdf--csv)
18. [Notifications](#18-notifications)
19. [Feign Client — UserServiceClient](#19-feign-client--userserviceclient)
20. [Tests Unitaires](#20-tests-unitaires)
21. [Frontend Angular 18](#21-frontend-angular-18)
22. [Scénario de Test Complet (Swagger)](#22-scénario-de-test-complet-swagger)
23. [Checklist Technique Finale](#23-checklist-technique-finale)

---

## 1. Vue d'ensemble du Module

Le Module 08 est le **maillon d'exécution** de l'écosystème TrustedWork Tunisia. Il intervient après la signature d'un contrat (Module 05, port 8085) et fournit un espace de travail collaboratif complet où client et freelancer suivent l'avancement en temps réel.

### Positionnement dans la plateforme

| Étape | Module | Description |
|-------|--------|-------------|
| 1 | 01 | Inscription + KYC |
| 2 | 02 | Profil Freelancer + Skills |
| 3 | 03 | Smart Job Board + Matching |
| 4 | 05 | Signature contrat + Escrow |
| **5** | **08** | **Gestion de projet + IA** |

### Ce que le module fait réellement

- Gestion complète de projets (CRUD + statuts + enrichissement depuis Module 01)
- Kanban board Angular avec Drag & Drop CDK (4 colonnes, recalcul automatique)
- Gantt Chart personnalisé CSS pur (sans librairie externe)
- Gestion des livrables avec workflow de review client
- 4 détections IA automatiques (DELAY_RISK via ML, BOTTLENECK, INACTIVITY, SCOPE_CREEP)
- Modèle **Random Forest** (200 arbres, 8 features, AUC-ROC 0.91) intégré via PMML/JPMML
- Burndown chart avec régression linéaire et projection de livraison
- Rapports de progression automatiques (hebdomadaires via `@Scheduled`)
- Système de mailing (Gmail SMTP + templates Thymeleaf HTML)
- Notifications persistées en base (6 types, badge Angular)
- Export PDF (iText 7) et CSV
- Sécurité JWT avec validation auprès de Module 01
- 5 classes de tests unitaires JUnit/Mockito

---

## 2. Architecture Technique

### Couches architecturales

```
┌─────────────────────────────────────────────────────────────────┐
│  REST Controllers (8 controllers, 43 endpoints)                 │
├─────────────────────────────────────────────────────────────────┤
│  AOP — LoggingAspect (@Before/@After) + PerformanceAspect (@Around) │
├─────────────────────────────────────────────────────────────────┤
│  Services (Interface + Impl)                                    │
│  ProjectServiceImpl | TaskServiceImpl | DeliverableServiceImpl  │
│  BurndownServiceImpl | MLPredictionServiceImpl | MailServiceImpl│
│  NotificationServiceImpl | ProgressReportServiceImpl | ...      │
├─────────────────────────────────────────────────────────────────┤
│  Repositories (JpaRepository + @Query JPQL)                     │
├─────────────────────────────────────────────────────────────────┤
│  Entités JPA (7 entités + 7 enums)                              │
├─────────────────────────────────────────────────────────────────┤
│  MySQL — trustedwork_project_db                                  │
└─────────────────────────────────────────────────────────────────┘
          │                        │
    UserServiceClient         DeliveryRiskScheduler
    (REST → Module 01)        (@Scheduled — toutes 15min)
                                   │
                          MLPredictionServiceImpl
                          (PMML RandomForest — 200 arbres)
```

### Structure des packages

```
tn.esprit.msprojectservice/
├── MsProjectServiceApplication.java          (@SpringBootApplication + @EnableScheduling)
├── aspect/
│   ├── LoggingAspect.java                    (@Before + @After)
│   └── PerformanceAspect.java                (@Around — mesure temps)
├── config/
│   ├── SecurityConfig.java                   (JWT stateless + CORS)
│   ├── SwaggerConfig.java
│   └── WebConfig.java
├── controllers/
│   ├── ProjectRestController.java            (9 endpoints)
│   ├── TaskRestController.java               (7 endpoints)
│   ├── SubTaskRestController.java            (4 endpoints)
│   ├── DeliverableRestController.java        (5 endpoints)
│   ├── ProgressReportRestController.java     (2 endpoints)
│   ├── RiskSignalRestController.java         (5 endpoints)
│   ├── BurndownRestController.java           (1 endpoint)
│   ├── NotificationRestController.java       (6 endpoints)
│   └── ExportRestController.java             (2 endpoints)
├── dto/
│   ├── ProjectDTO.java
│   ├── TaskDTO.java
│   ├── SubTaskDTO.java
│   ├── DeliverableDTO.java
│   ├── ProgressReportDTO.java
│   ├── DeliveryRiskSignalDTO.java
│   ├── NotificationDTO.java
│   ├── MLPredictionDTO.java                  (résultat Random Forest)
│   ├── BurndownChartDTO.java
│   └── BurndownPointDTO.java
├── entities/
│   ├── Project.java
│   ├── Task.java
│   ├── SubTask.java
│   ├── Deliverable.java
│   ├── ProgressReport.java
│   ├── DeliveryRiskSignal.java
│   ├── Notification.java
│   └── [7 enums]
├── exceptions/
│   ├── EntityNotFoundException.java
│   └── PdfGenerationException.java
├── feign/
│   ├── UserServiceClient.java                (RestTemplate → Module 01)
│   └── dto/TokenValidationDTO.java + UserDTO.java
├── repositories/ (7 interfaces JpaRepository)
├── scheduler/
│   └── DeliveryRiskScheduler.java            (3 jobs @Scheduled)
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── AuthenticatedUser.java
└── services/ (10 interfaces + 10 implémentations)
```

---

## 3. Configuration — application.properties

```properties
spring.application.name=ms-project-service
server.port=8089
server.servlet.context-path=/project

# Base de données
spring.datasource.url=jdbc:mysql://localhost:3306/trustedwork_project_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# User Service (Module 01 — port 8081)
user-service.base-url=http://localhost:8081/api

# JWT — même secret que Module 01
jwt.secret=PiCloudSuperSecretKeyForJWTTokenGenerationMustBeAtLeast256Bits!!

# Gmail SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=youssefabidi441@gmail.com
spring.mail.properties.mail.smtp.starttls.enable=true

# Thymeleaf
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
spring.thymeleaf.cache=false
```

> **Note importante :** Le service écoute sur le port **8089** (pas 8090 comme dans le SRS initial) avec le context-path `/project`. L'URL réelle est donc `http://localhost:8089/project/api/...`

---

## 4. Dépendances Maven — pom.xml

| Dépendance | Version | Usage |
|------------|---------|-------|
| `spring-boot-starter-web` | 3.5.13 | REST API |
| `spring-boot-starter-data-jpa` | 3.5.13 | JPA/Hibernate |
| `spring-boot-starter-security` | 3.5.13 | JWT Security |
| `spring-boot-starter-aop` | 3.5.13 | Logging + Performance |
| `spring-boot-starter-mail` | 3.5.13 | Gmail SMTP |
| `spring-boot-starter-thymeleaf` | 3.5.13 | Templates emails HTML |
| `spring-boot-starter-validation` | 3.5.13 | Validation DTO |
| `mysql-connector-j` | — | Driver MySQL |
| `lombok` | — | @Data, @Builder, etc. |
| `springdoc-openapi-starter-webmvc-ui` | 2.5.0 | Swagger UI |
| `jjwt-api / jjwt-impl / jjwt-jackson` | 0.12.6 | JWT parsing |
| `pmml-evaluator-metro` | **1.7.7** | Inférence Random Forest PMML |
| `itext7-core` | 8.0.3 | Export PDF |
| `spring-boot-starter-test` | 3.5.13 | JUnit + Mockito |

---

## 5. Les 7 Entités JPA

### 5.1 Project

Entité centrale. Créée après signature contrat (Module 05).

| Champ | Type | Description |
|-------|------|-------------|
| id | Long | PK auto-généré |
| title | String | Titre du projet |
| description | TEXT | Description détaillée |
| status | ProjectStatus | ACTIVE / ON_HOLD / COMPLETED / CANCELLED |
| contractId | Long | FK logique vers Module 05 |
| clientId | Long | ID client (JWT) |
| freelancerId | Long | ID freelancer (JWT) |
| startDate | LocalDate | Date de début |
| endDate | LocalDate | Date de fin prévue |
| completionRate | int | 0-100 — recalculé automatiquement |
| budget | Double | Budget en DT |
| createdAt | LocalDateTime | Auto via @PrePersist |
| updatedAt | LocalDateTime | Auto via @PreUpdate |

**Relations :** `@OneToMany` → Task, Deliverable, ProgressReport, DeliveryRiskSignal  
**@PrePersist :** initialise `status=ACTIVE`, `completionRate=0`, `createdAt`, `updatedAt`  
**@ToString.Exclude :** sur toutes les listes pour éviter le `StackOverflowError` Lombok

---

### 5.2 Task

Unité de travail principale. Le champ `status` pilote le Kanban Angular.

| Champ | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| title | String | Titre |
| description | TEXT | Description |
| status | TaskStatus | TODO / IN_PROGRESS / IN_REVIEW / DONE |
| priority | TaskPriority | LOW / MEDIUM / HIGH / CRITICAL |
| project | Project | @ManyToOne (FK project_id) |
| assigneeId | Long | ID du membre assigné |
| deadline | LocalDate | Date limite |
| estimatedHours | Integer | Durée estimée en heures |
| actualHours | Integer | Durée réelle |
| createdAt | LocalDateTime | Auto @PrePersist |
| updatedAt | LocalDateTime | Auto @PreUpdate |

**Relations :** `@OneToMany` → SubTask, Deliverable  
**Point clé :** Chaque changement de `status` déclenche `updateProjectCompletionRate()` dans le service.

---

### 5.3 SubTask

Décomposition granulaire d'une tâche.

| Champ | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| title | String | Titre |
| done | boolean | false par défaut (@PrePersist) |
| task | Task | @ManyToOne (FK task_id) |
| createdAt | LocalDateTime | Auto @PrePersist |

---

### 5.4 Deliverable

Livrable formel soumis par le freelancer pour validation client.

| Champ | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| title | String | Titre |
| description | TEXT | Description |
| fileUrl | String | URL fichier/lien externe |
| status | DeliverableStatus | SUBMITTED → APPROVED / REJECTED |
| task | Task | @ManyToOne optionnel |
| project | Project | @ManyToOne |
| submittedAt | LocalDateTime | Auto @PrePersist |
| reviewedAt | LocalDateTime | Rempli lors de la review |
| reviewComment | TEXT | Commentaire client |

**@PrePersist :** `status=SUBMITTED`, `submittedAt=now()`  
**Règle métier :** On ne peut reviewer que si `status == SUBMITTED` (sinon exception 400).  
**Mailing :** Après review, un email Thymeleaf est envoyé au freelancer.

---

### 5.5 ProgressReport

Snapshot automatique de l'état du projet à un instant T.

| Champ | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| project | Project | @ManyToOne |
| completedTasks | int | Tâches DONE |
| totalTasks | int | Total tâches |
| completionRate | int | completedTasks / totalTasks × 100 |
| openDeliverables | int | Livrables SUBMITTED |
| activeRisks | int | Signaux non résolus |
| summary | TEXT | Résumé textuel généré en français |
| generatedAt | LocalDateTime | Auto @PrePersist |

**Génération :** automatique chaque lundi à 8h via `@Scheduled` + à la demande via endpoint.

---

### 5.6 DeliveryRiskSignal

Alerte générée par le moteur IA. Ne jamais créer manuellement — uniquement via le Scheduler.

| Champ | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| project | Project | @ManyToOne |
| riskType | RiskType | DELAY_RISK / BOTTLENECK / INACTIVITY / SCOPE_CREEP |
| severity | RiskSeverity | LOW / MEDIUM / HIGH / CRITICAL |
| message | TEXT | Message lisible généré automatiquement |
| affectedTaskId | Long | ID tâche concernée (null pour INACTIVITY/SCOPE_CREEP) |
| resolved | boolean | false = actif, true = résolu |
| detectedAt | LocalDateTime | Auto @PrePersist |
| resolvedAt | LocalDateTime | Rempli lors de la résolution |

**@PrePersist :** `resolved=false`, `detectedAt=now()`  
**Anti-doublon :** vérification avant création (`existsActiveSignal` / `existsActiveProjectSignal`)

---

### 5.7 Notification

Notifications internes persistées en base.

| Champ | Type | Description |
|-------|------|-------------|
| id | Long | PK |
| userId | Long | Destinataire |
| title | String | Titre |
| message | TEXT | Contenu |
| type | NotificationType | 6 types disponibles |
| projectId | Long | Projet concerné |
| taskId | Long | Tâche concernée (optionnel) |
| read | boolean | @Column(name = "is_read") — mot réservé MySQL |
| createdAt | LocalDateTime | Auto @PrePersist |

---

## 6. Les 7 Enums

| Enum | Valeurs |
|------|---------|
| ProjectStatus | ACTIVE, ON_HOLD, COMPLETED, CANCELLED |
| TaskStatus | TODO, IN_PROGRESS, IN_REVIEW, DONE |
| TaskPriority | LOW, MEDIUM, HIGH, CRITICAL |
| DeliverableStatus | SUBMITTED, APPROVED, REJECTED |
| RiskType | DELAY_RISK, BOTTLENECK, INACTIVITY, SCOPE_CREEP |
| RiskSeverity | LOW, MEDIUM, HIGH, CRITICAL |
| NotificationType | DEADLINE_24H, DELIVERABLE_PENDING, DELIVERABLE_REVIEWED, RISK_DETECTED, TASK_BLOCKED, WEEKLY_REPORT |

---

## 7. Les DTOs

Chaque entité a un DTO avec **mapping manuel bidirectionnel** (pas de MapStruct).

- `fromEntity(Entity e)` → convertit une entité JPA en DTO (méthode statique)
- `toEntity(DTO dto)` → convertit un DTO en entité JPA (ne set jamais les relations)

**Règle :** le `toEntity()` ne set jamais les `@ManyToOne`. C'est toujours le service qui récupère l'entité parente via son ID et la set sur l'enfant.

**DTOs spéciaux :**

- `MLPredictionDTO` — résultat du modèle Random Forest : probabilité, sévérité, message, feature critique, 8 features calculées
- `BurndownChartDTO` — deux courbes (idéale + réelle), vélocité, retard estimé, date projetée, statut, analyse textuelle
- `BurndownPointDTO` — un point de la courbe : date + tachesRestantes

---

## 8. Les Repositories

### IProjectRepository
```
findByContractId(Long)              → Optional<Project>
findAllByUserId(Long)               → List<Project>    [JPQL : clientId OR freelancerId]
findByStatus(ProjectStatus)         → List<Project>
findAllActiveProjects()             → List<Project>    [JPQL : status = 'ACTIVE']
```

### ITaskRepository
```
findByProjectId(Long)               → List<Task>
findByProjectIdAndStatus(Long, TaskStatus) → List<Task>
findByAssigneeId(Long)              → List<Task>
countCompletedTasksByProjectId(Long) → int             [JPQL : status = 'DONE']
countTotalTasksByProjectId(Long)    → int
findBlockedTasksByProjectId(Long)   → List<Task>       [JPQL : IN_PROGRESS OR IN_REVIEW]
```

### ISubTaskRepository
```
findByTaskId(Long)                  → List<SubTask>
countCompletedByTaskId(Long)        → int              [JPQL : done = true]
countTotalByTaskId(Long)            → int
```

### IDeliverableRepository
```
findByProjectId(Long)               → List<Deliverable>
findByProjectIdAndStatus(Long, DeliverableStatus) → List<Deliverable>
countOpenDeliverablesByProjectId(Long) → int           [JPQL : status = 'SUBMITTED']
findByTaskId(Long)                  → List<Deliverable>
```

### IProgressReportRepository
```
findByProjectIdOrderByGeneratedAtDesc(Long) → List<ProgressReport>
findLatestByProjectId(Long)          → Optional<ProgressReport> [JPQL : LIMIT 1]
```

### IRiskSignalRepository
```
findByProjectIdAndResolvedFalse(Long) → List<DeliveryRiskSignal>
findByProjectId(Long)                 → List<DeliveryRiskSignal>
findLatestCriticalByProjectId(Long)   → Optional<DeliveryRiskSignal>
  [JPQL : ORDER BY CASE severity CRITICAL=1,HIGH=2,MEDIUM=3,LOW=4]
countActiveRisksByProjectId(Long)     → int
existsActiveSignal(Long, RiskType, Long taskId) → boolean   [anti-doublon BOTTLENECK]
existsActiveProjectSignal(Long, RiskType)       → boolean   [anti-doublon INACTIVITY/SCOPE_CREEP]
```

### INotificationRepository
```
findByUserIdOrderByCreatedAtDesc(Long)             → List<Notification>
findByUserIdAndReadFalseOrderByCreatedAtDesc(Long) → List<Notification>
countUnreadByUserId(Long)                          → int
markAllAsReadByUserId(Long)                        → void  [@Modifying + @Transactional]
```

---

## 9. Les Services

### ProjectServiceImpl
Méthodes principales : `createProject`, `getProjectById`, `getProjectByContractId`, `getProjectsByUserId`, `getAllProjects`, `updateProject`, `updateProjectStatus`, `deleteProject`

**Méthodes enrichies (bonus)** :
- `getProjectByIdEnriched(Long id)` — appelle `UserServiceClient.getUserById()` pour injecter CIN, prénom, nom, email du client ET du freelancer dans le DTO
- `getMyProjects(Long userId)` — retourne les projets de l'utilisateur connecté, enrichis

### TaskServiceImpl
- `createTask(projectId, taskDTO)` → sauvegarde + `updateProjectCompletionRate()`
- `updateTaskStatus(id, status)` → change le statut Kanban + `updateProjectCompletionRate()`
- `deleteTask(id)` → supprime + `updateProjectCompletionRate()`
- `updateProjectCompletionRate(projectId)` — méthode privée : `completedTasks / totalTasks × 100`

### SubTaskServiceImpl
- `toggleSubTask(id)` — inverse le booléen `done` : `subTask.setDone(!subTask.isDone())`

### DeliverableServiceImpl
- `reviewDeliverable(id, status, comment)` — vérifie `status == SUBMITTED`, met à jour, déclenche `mailService.envoyerNotificationReviewLivrable()`

### ProgressReportServiceImpl
- `generateReport(projectId)` — agrège 5 métriques depuis 4 repositories, génère le résumé textuel en français, persiste en base

### RiskSignalServiceImpl
- `resolveRisk(id)` — vérifie que `resolved == false`, set `resolved=true` + `resolvedAt=now()`

### BurndownServiceImpl
- `getBurndownChart(projectId)` — calcule courbe idéale (linéaire), courbe réelle (historique `updatedAt` des tâches DONE) + projection linéaire pour les dates futures, vélocité journalière, retard estimé, date de livraison projetée, statut, analyse textuelle

### MLPredictionServiceImpl
- `@PostConstruct loadModel()` — charge `delivery_risk_model.pmml` une seule fois au démarrage via `LoadingModelEvaluatorBuilder` (JPMML 1.7.x)
- `predictDeliveryRisk(projectId)` — calcule 8 features, appelle le modèle, retourne `MLPredictionDTO`

### MailServiceImpl
- `envoyerRapportHebdomadaire(project, report)` — template Thymeleaf `rapport-hebdomadaire.html`, envoie à client + freelancer via `UserServiceClient`
- `envoyerNotificationReviewLivrable(deliverable)` — template `review-livrable.html`, envoie au freelancer

### NotificationServiceImpl
- `createNotification()`, `getUnreadNotifications()`, `getUnreadCount()`, `markAsRead()`, `markAllAsRead()`

### ExportServiceImpl
- `exportProjectReportPdf(projectId)` — génère PDF via iText 7 : 4 sections (infos projet + tableau tâches + tableau livrables + tableau risques), retourne `byte[]`
- `exportProjectTasksCsv(projectId)` — CSV avec séparateur `;` (compatible Excel français), retourne `byte[]`

---

## 10. Les Controllers REST — 43 Endpoints

### ProjectRestController — `/api/projects`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| POST | `/api/projects` | Créer un projet | 201 |
| GET | `/api/projects/{id}` | Détails d'un projet | 200 |
| GET | `/api/projects/{id}/enriched` | Projet + CIN/nom/email parties | 200 |
| GET | `/api/projects/my` | Mes projets (lu depuis JWT) | 200 |
| GET | `/api/projects/contract/{contractId}` | Projet par contrat | 200 |
| GET | `/api/projects/user/{userId}` | Projets d'un utilisateur | 200 |
| GET | `/api/projects` | Tous les projets | 200 |
| PUT | `/api/projects/{id}` | Modifier un projet | 200 |
| PATCH | `/api/projects/{id}/status` | Changer le statut | 200 |
| DELETE | `/api/projects/{id}` | Supprimer un projet | 204 |

### TaskRestController — `/api`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| POST | `/api/projects/{projectId}/tasks` | Créer une tâche | 201 |
| GET | `/api/projects/{projectId}/tasks` | Lister les tâches (Kanban) | 200 |
| GET | `/api/tasks/{id}` | Détails d'une tâche | 200 |
| PUT | `/api/tasks/{id}` | Modifier une tâche | 200 |
| PATCH | `/api/tasks/{id}/status` | Déplacer carte Kanban | 200 |
| PATCH | `/api/tasks/{id}/assign` | Assigner une tâche | 200 |
| DELETE | `/api/tasks/{id}` | Supprimer une tâche | 204 |

### SubTaskRestController — `/api`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| POST | `/api/tasks/{taskId}/subtasks` | Ajouter une sous-tâche | 201 |
| GET | `/api/tasks/{taskId}/subtasks` | Lister les sous-tâches | 200 |
| PATCH | `/api/subtasks/{id}/toggle` | Cocher / décocher | 200 |
| DELETE | `/api/subtasks/{id}` | Supprimer | 204 |

### DeliverableRestController — `/api`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| POST | `/api/projects/{projectId}/deliverables` | Soumettre un livrable | 201 |
| GET | `/api/projects/{projectId}/deliverables` | Lister les livrables | 200 |
| GET | `/api/deliverables/{id}` | Détails d'un livrable | 200 |
| PATCH | `/api/deliverables/{id}/review` | Approuver ou rejeter | 200 |
| DELETE | `/api/deliverables/{id}` | Supprimer | 204 |

### ProgressReportRestController — `/api/projects/{projectId}`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| GET | `/api/projects/{projectId}/report` | Générer un rapport | 200 |
| GET | `/api/projects/{projectId}/reports` | Historique des rapports | 200 |

### RiskSignalRestController — `/api`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| GET | `/api/projects/{projectId}/risks` | Signaux de risque actifs | 200 |
| GET | `/api/projects/{projectId}/risks/latest` | Signal le plus critique | 200 |
| POST | `/api/projects/{projectId}/risks/analyze` | Lancer l'analyse IA | 200 |
| PATCH | `/api/risks/{id}/resolve` | Résoudre un signal | 200 |
| GET | `/api/projects/{projectId}/risks/ml-predict` | Prédiction Random Forest | 200 |

### BurndownRestController — `/api/projects`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| GET | `/api/projects/{projectId}/burndown` | Burndown chart complet | 200 |

### NotificationRestController — `/api/notifications`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| GET | `/api/notifications/user/{userId}` | Toutes les notifications | 200 |
| GET | `/api/notifications/user/{userId}/unread` | Non lues | 200 |
| GET | `/api/notifications/user/{userId}/unread/count` | Compteur badge | 200 |
| PATCH | `/api/notifications/{id}/read` | Marquer comme lue | 200 |
| PATCH | `/api/notifications/user/{userId}/read-all` | Tout marquer comme lu | 200 |
| POST | `/api/notifications/test/{projectId}` | Déclencher notifications manuellement | 200 |

### ExportRestController — `/api/projects/{projectId}/export`

| Méthode | URL | Description | HTTP |
|---------|-----|-------------|------|
| GET | `/api/projects/{projectId}/export/pdf` | Télécharger PDF complet | 200 |
| GET | `/api/projects/{projectId}/export/csv` | Télécharger CSV tâches | 200 |

**Total : 43 endpoints REST**

---

## 11. Sécurité JWT

### Composants

**`SecurityConfig`** — configuration Spring Security :
- Sessions stateless (`SessionCreationPolicy.STATELESS`)
- CSRF désactivé
- CORS autorisé pour `localhost:4200` et `localhost:4201`
- Swagger (`/swagger-ui/**`, `/v3/api-docs/**`) en accès libre
- Tout le reste : JWT obligatoire

**`JwtAuthenticationFilter`** — filtre `OncePerRequestFilter` :
1. Extrait le token `Bearer` du header `Authorization`
2. Valide localement la signature via `JwtService`
3. Appelle `UserServiceClient.validateToken()` vers Module 01 pour récupérer userId, role, cin
4. Crée un `UsernamePasswordAuthenticationToken` dans le `SecurityContext`

**`JwtService`** — décode le JWT avec la clé secrète partagée (même clé que Module 01)

**`AuthenticatedUser`** — objet principal injecté dans le contexte de sécurité Spring

---

## 12. AOP — Logging et Performance

### LoggingAspect

Pointcut : `execution(* tn.esprit.msprojectservice.services.*.*(..))`

```
@Before  → log avant chaque méthode service
▶ APPEL — ProjectServiceImpl.getProjectById() | Paramètres : [1]

@After   → log après chaque méthode service
✔ FIN — ProjectServiceImpl.getProjectById() exécutée avec succès
```

### PerformanceAspect

Pointcut : `execution(* tn.esprit.msprojectservice.services.*.*(..))`

```
@Around → mesure le temps d'exécution
⏱ PERF — ProjectServiceImpl.getProjectById() exécutée en 23 ms
⏱ LENT — ProgressReportServiceImpl.generateReport() a pris 1247 ms (> 1 seconde !)
```

Seuil d'alerte : > 1000 ms → log `WARN` au lieu de `INFO`

---

## 13. Scheduler IA — DeliveryRiskScheduler

Composant `@Component` avec 3 méthodes `@Scheduled`.

### Fréquences (mode test : toutes les 15 min)

| Cron (production) | Cron (test) | Action |
|---|---|---|
| `0 0 7 * * *` | `0 */15 * * * *` | Notifications quotidiennes |
| `0 0 8 * * *` | `0 */15 * * * *` | Analyse IA tous les projets ACTIVE |
| `0 0 8 * * MON` | `0 */15 * * * *` | Rapports hebdomadaires + emails |

### Détection 1 — DELAY_RISK

Délègue entièrement au modèle ML (voir section 14). Si `prediction.getSeverity() != null` et qu'aucun signal DELAY_RISK actif n'existe → crée un signal avec le message généré par le modèle.

### Détection 2 — BOTTLENECK

Pour chaque tâche en `IN_PROGRESS` ou `IN_REVIEW` :
- Calcule `daysSinceUpdate = now - task.updatedAt`
- Si `daysSinceUpdate >= 3` ET pas de signal BOTTLENECK actif pour cette tâche → crée signal
- Sévérité : MEDIUM si 3-5 jours, HIGH si 6+ jours

### Détection 3 — INACTIVITY

- Trouve la date de dernière activité = `max(task.updatedAt)` sur toutes les tâches du projet
- Si `daysSinceActivity >= 5` ET pas de signal INACTIVITY actif → crée signal
- Sévérité : MEDIUM si 5-9 jours, HIGH si 10+ jours

### Détection 4 — SCOPE_CREEP

- `initialTasks` = tâches créées le jour du `startDate` ou avant
- `addedTasks` = tâches créées après le `startDate`
- `scopeRatio = addedTasks / initialTasks`
- Si `scopeRatio > 0.30` ET pas de signal SCOPE_CREEP actif → crée signal
- Sévérité : MEDIUM si 30-60%, HIGH si > 60%

### Notifications quotidiennes

- `DEADLINE_24H` — tâche avec deadline aujourd'hui ou demain → notifie `assigneeId` + `clientId`
- `DELIVERABLE_PENDING` — livrables SUBMITTED en attente → notifie `clientId`
- `TASK_BLOCKED` — tâche bloquée ≥ 3 jours → notifie `assigneeId`

### Rapport hebdomadaire

1. Génère le `ProgressReport` en base via `progressReportService.generateReport()`
2. Envoie l'email Thymeleaf via `mailService.envoyerRapportHebdomadaire()`

### Analyse manuelle (démo)

```
POST /api/projects/{projectId}/risks/analyze
```
Appelle `analyzeProjectById(projectId)` qui exécute les 4 détections sur un projet spécifique.

---

## 14. Moteur ML — Random Forest PMML

### Le modèle

- **Algorithme :** Random Forest Classifier (scikit-learn)
- **Paramètres :** 200 arbres, profondeur max 8, min_samples_leaf=4, class_weight='balanced'
- **Export :** PMML 4.4 via `sklearn2pmml 0.130.0`
- **Fichier :** `src/main/resources/ml/delivery_risk_model.pmml` (75 458 lignes)
- **Variable cible :** `will_be_late` — 0 = à temps, 1 = en retard

### Chargement au démarrage (@PostConstruct)

```java
this.evaluator = new LoadingModelEvaluatorBuilder()
    .setLocatable(false)
    .load(modelStream)
    .build();
this.evaluator.verify();
```

Le modèle est chargé **une seule fois** en mémoire. Inférence instantanée ensuite.

### Les 8 Features calculées depuis la base de données

| Feature | Calcul | Interprétation |
|---------|--------|----------------|
| `days_elapsed_ratio` | jours écoulés / durée totale | 0.8 = 80% du temps passé |
| `tasks_done_ratio` | tâches DONE / total | 0.3 = 30% terminé |
| `active_risks_count` | COUNT(signaux non résolus) | nombre d'alertes actives |
| `bottleneck_days_avg` | moyenne(jours bloqués) des tâches IN_PROGRESS/IN_REVIEW | blocages en cours |
| `open_deliverables_count` | COUNT(SUBMITTED non validés) | livrables en attente |
| `assignee_perf_score` | tâches DONE avant deadline / total tâches freelancer | historique de livraison |
| `scope_creep_ratio` | tâches ajoutées / tâches initiales | dérive du périmètre |
| `budget_consumption_ratio` | daysRatio × 1.1 (Phase 1 — estimé) | budget consommé estimé |

### Inférence JPMML

Les 8 features sont converties en `FieldValue` JPMML, passées à `evaluator.evaluate()`, et le résultat `probability(1)` est extrait — c'est la proportion des 200 arbres qui ont voté "en retard".

### Seuils de sévérité

| P(retard) | Sévérité | Action |
|-----------|----------|--------|
| ≥ 0.85 | CRITICAL | Signal DELAY_RISK CRITICAL créé |
| ≥ 0.70 | HIGH | Signal DELAY_RISK HIGH créé |
| ≥ 0.50 | MEDIUM | Signal DELAY_RISK MEDIUM créé |
| ≥ 0.35 | LOW | Signal DELAY_RISK LOW créé |
| < 0.35 | null | Aucun signal créé |

### Feature critique

Identifie quelle feature a le plus contribué au risque selon les poids du Random Forest :

| Feature | Poids |
|---------|-------|
| active_risks_count | 36% |
| bottleneck_days_avg | 16% |
| tasks_done_ratio | 12% |
| assignee_perf_score | 8% |
| scope_creep_ratio | 7% |

### MLPredictionDTO — Réponse complète

```json
{
  "probabilityLate": 0.731,
  "willBeLate": true,
  "severity": "HIGH",
  "message": "🔴 Analyse IA — Projet \"TuniShop\" : risque de retard élevé (73%)...",
  "criticalFeature": "active_risks_count",
  "daysElapsedRatio": 0.80,
  "tasksDoneRatio": 0.30,
  "activeRisksCount": 3,
  "bottleneckDaysAvg": 4.5,
  "openDeliverablesCount": 2,
  "assigneePerfScore": 0.65,
  "scopeCreepRatio": 0.25,
  "budgetConsumptionRatio": 0.88
}
```

---

## 15. Burndown Chart

### Endpoint

```
GET /api/projects/{projectId}/burndown
```

### Ce que retourne le service

**Courbe idéale** — décroissance linéaire mathématique pure :
`tachesIdeales(jour) = totalTaches - (totalTaches × jour / dureeTotale)`

**Courbe réelle** — reconstituée depuis l'historique des tâches DONE :
- Partie passée : groupement des `updatedAt` des tâches DONE jour par jour, décrémentation du compteur
- Partie future : projection par tendance linéaire basée sur la vélocité observée

**Métriques calculées :**
- `velociteJournaliere` = tâches DONE / jours écoulés depuis startDate
- `retardEstimeJours` = date projetée − endDate (positif = en retard)
- `dateLivraisonProjetee` = today + (tachesRestantes / velocite) jours
- `statutBurndown` = EN_AVANCE / DANS_LES_DELAIS / EN_RETARD / CRITIQUE
- `analyse` = message textuel en français généré automatiquement

---

## 16. Mailing — Thymeleaf + Gmail SMTP

### Templates HTML (`src/main/resources/templates/`)

**`rapport-hebdomadaire.html`** — envoyé chaque lundi au client ET au freelancer :
- Variables : titre projet, completion rate, tâches terminées, livrables en attente, risques actifs, résumé, dates

**`review-livrable.html`** — envoyé au freelancer après review client :
- Variables : prénom freelancer, titre livrable, statut (APPROUVÉ/REJETÉ), commentaire, date review

### Récupération des emails

Les emails sont récupérés via `UserServiceClient.getUserById()` → appel REST vers Module 01 (`GET /api/identity/users/{userId}`). Si Module 01 est indisponible, l'email n'est pas envoyé mais la sauvegarde en base n'est **pas bloquée** (try/catch isolé).

### Déclencheurs

| Trigger | Template | Destinataires |
|---------|----------|---------------|
| Rapport hebdomadaire (`@Scheduled`) | rapport-hebdomadaire.html | Client + Freelancer |
| Review livrable (APPROVED/REJECTED) | review-livrable.html | Freelancer |

---

## 17. Export PDF / CSV

### Export PDF — `GET /api/projects/{id}/export/pdf`

Généré via **iText 7 (8.0.3)**. Structure du document :

1. En-tête : "RAPPORT DE PROJET — TrustedWork Tunisia"
2. Section 1 : Informations générales (titre, description, statut, budget, dates, taux de complétion)
3. Section 2 : Tableau des tâches (titre, statut, priorité, deadline, heures estimées)
4. Section 3 : Tableau des livrables (titre, statut, commentaire, date soumission)
5. Section 4 : Tableau des signaux de risque actifs (type, sévérité, message, date)
6. Footer : "Rapport généré automatiquement par TrustedWork Tunisia"

Retourne `byte[]` avec `Content-Type: application/pdf` et `Content-Disposition: attachment`.

### Export CSV — `GET /api/projects/{id}/export/csv`

Colonnes : `ID;Titre;Statut;Priorité;Assigné à;Deadline;Heures Estimées;Heures Réelles;Créé le`

Séparateur : `;` (compatible Excel français). Valeurs avec `;` ou `"` échappées automatiquement.

---

## 18. Notifications

### 6 types et leurs déclencheurs

| Type | Déclencheur | Destinataire |
|------|-------------|-------------|
| DEADLINE_24H | Tâche deadline dans 24h ou aujourd'hui | Freelancer + Client |
| DELIVERABLE_PENDING | Livrables SUBMITTED non validés | Client |
| TASK_BLOCKED | Tâche bloquée ≥ 3 jours | Freelancer assigné |
| WEEKLY_REPORT | Rapport hebdomadaire généré | Client + Freelancer |
| RISK_DETECTED | Nouveau signal de risque IA | (prêt, non encore déclenché automatiquement) |
| DELIVERABLE_REVIEWED | Livrable approuvé ou rejeté | Freelancer |

### Badge Angular

`GET /api/notifications/user/{userId}/unread/count` → retourne un entier.  
La cloche dans la navbar Angular fait un polling toutes les 30 secondes sur cet endpoint.

---

## 19. Feign Client — UserServiceClient

Implémenté avec `RestTemplate` (pas l'annotation `@FeignClient`).

**Deux méthodes :**

```
validateToken(String authHeader)
  → POST /api/identity/validate-token
  → retourne TokenValidationDTO (userId, cin, email, role)
  → utilisé par JwtAuthenticationFilter

getUserById(Long userId)
  → GET /api/identity/users/{userId}
  → retourne UserDTO (cin, firstName, lastName, email)
  → utilisé par ProjectServiceImpl.enrichProject() et MailServiceImpl
```

En cas d'erreur (Module 01 indisponible) → `log.warn()` + retourne `null`. Ne bloque jamais le service.

---

## 20. Tests Unitaires

5 classes de tests JUnit 5 + Mockito. Les rapports `surefire` sont présents dans `target/` (tests exécutés avec succès).

| Classe de test | Service testé | Méthodes couvertes |
|----------------|--------------|-------------------|
| ProjectServiceImplTest | ProjectServiceImpl | createProject, getById, update, delete, getByContractId |
| TaskServiceImplTest | TaskServiceImpl | createTask, updateStatus, assignTask, deleteTask, completionRate |
| DeliverableServiceImplTest | DeliverableServiceImpl | submit, review (APPROVED/REJECTED), erreur si déjà reviewé |
| ProgressReportServiceImplTest | ProgressReportServiceImpl | generateReport, getLatestReport, getHistory |
| RiskSignalServiceImplTest | RiskSignalServiceImpl | getActiveRisks, resolveRisk, erreur si déjà résolu |

---

## 21. Frontend Angular 18

### Routing du module projet

| Route | Composant | Description |
|-------|-----------|-------------|
| `/app/projects` | ProjectListComponent | Liste des projets |
| `/app/projects/notifications` | NotificationsComponent | Centre de notifications |
| `/app/projects/:id` | ProjectDetailComponent | Dashboard projet |
| `/app/projects/:id/kanban` | KanbanBoardComponent | Kanban Drag & Drop |
| `/app/projects/:id/deliverables` | DeliverablesComponent | Gestion livrables |
| `/app/projects/:id/risks` | RiskSignalsComponent | Signaux IA + ML |
| `/app/projects/:id/gantt` | GanttChartComponent | Gantt Chart CSS pur |

### KanbanBoardComponent

- 4 colonnes : TODO / EN COURS / EN REVIEW / TERMINÉE
- Angular CDK `DragDropModule` — `CdkDragDrop`, `transferArrayItem`
- Drop → `PATCH /tasks/{id}/status` → `completionRate` recalculé côté backend
- Sous-tâches expandables par tâche
- Modals création et édition de tâches
- Contrôle d'accès par rôle (ADMIN / CLIENT / FREELANCER) via JWT

### GanttChartComponent

- **Aucune librairie externe** — implémentation CSS pur
- Calcul des positions en `%` de la timeline via `dateToPct(date)`
- Barres de progression : couleur selon statut, rouge si `isOverdue`
- Diamond de deadline à l'extrémité de chaque barre
- Ligne verticale "Aujourd'hui"
- Filtres temps réel (statut + priorité)
- Zoom 3 niveaux : compact (100%) / normal (150%) / large (250%)
- Header automatique avec les mois en français (`toLocaleDateString('fr-TN', ...)`)

### RiskSignalsComponent

- Affiche les signaux actifs avec filtres par sévérité et type
- Bouton "Analyser" → `POST /risks/analyze`
- **Panel ML dédié** : bouton "Prédiction IA" → `GET /risks/ml-predict` → affiche `MLPredictionDTO` complet avec probabilité, sévérité, feature critique et les 8 features calculées

### NotificationBellComponent (Shared)

Composant partagé dans la navbar principale. Badge rouge avec compteur non-lues. Polling `GET /notifications/user/{id}/unread/count` toutes les 30 secondes.

### ProjectApiService

Service Angular unique pour tous les appels backend (43 endpoints couverts), base URL `http://localhost:8089/project`. `TokenInterceptor` injecte automatiquement le header `Authorization: Bearer <token>`.

---

## 22. Scénario de Test Complet (Swagger)

Swagger UI : `http://localhost:8089/project/swagger-ui.html`

### Étape 1 — Créer un projet
```
POST /api/projects
Body : { title, description, contractId: 1, clientId: 100, freelancerId: 200, startDate, endDate, budget }
```

### Étape 2 — Créer des tâches
```
POST /api/projects/1/tasks
Body : { title, priority: "HIGH", assigneeId: 200, deadline, estimatedHours: 24 }
```

### Étape 3 — Ajouter des sous-tâches
```
POST /api/tasks/1/subtasks
Body : { title: "Wireframe page d'accueil" }
```

### Étape 4 — Simuler le Kanban
```
PATCH /api/tasks/1/status?status=IN_PROGRESS
PATCH /api/tasks/1/status?status=DONE
→ Vérifier : GET /api/projects/1 → completionRate recalculé
```

### Étape 5 — Soumettre et reviewer un livrable
```
POST /api/projects/1/deliverables
Body : { title, fileUrl: "https://figma.com/...", taskId: 1 }

PATCH /api/deliverables/1/review?status=APPROVED&reviewComment=Excellent travail !
→ Email envoyé au freelancer automatiquement
```

### Étape 6 — Générer un rapport
```
GET /api/projects/1/report
```

### Étape 7 — Simuler un bottleneck (MySQL)
```sql
UPDATE task SET updated_at = DATE_SUB(NOW(), INTERVAL 5 DAY) WHERE id = 2;
```

### Étape 8 — Lancer l'analyse IA
```
POST /api/projects/1/risks/analyze
→ Retourne les signaux créés (BOTTLENECK, éventuellement DELAY_RISK via ML)
```

### Étape 9 — Prédiction ML directe
```
GET /api/projects/1/risks/ml-predict
→ Retourne MLPredictionDTO avec P(retard), sévérité, feature critique, 8 features
```

### Étape 10 — Burndown Chart
```
GET /api/projects/1/burndown
→ Retourne courbe idéale, courbe réelle, vélocité, retard estimé, date projetée
```

### Étape 11 — Résoudre un risque
```
PATCH /api/risks/1/resolve
```

### Étape 12 — Exporter
```
GET /api/projects/1/export/pdf
GET /api/projects/1/export/csv
```

---

## 23. Checklist Technique Finale

| Exigence | Statut | Détail |
|----------|--------|--------|
| JPA Entities + Enums | ✅ | 7 entités + 7 enums |
| @PrePersist / @PreUpdate | ✅ | Sur toutes les entités concernées |
| @ToString.Exclude (anti-StackOverflow) | ✅ | Sur toutes les relations bidirectionnelles |
| DTOs mapping manuel (fromEntity + toEntity) | ✅ | 10 DTOs (7 entités + ML + Burndown) |
| Repositories JpaRepository + @Query JPQL | ✅ | 7 repositories, 25+ queries custom |
| Services Interface + Impl + @Autowired | ✅ | 10 services (interface + impl) |
| REST Controllers GET/POST/PUT/PATCH/DELETE | ✅ | 9 controllers, 43 endpoints |
| Swagger @Tag + @Operation | ✅ | Sur chaque endpoint |
| AOP Logging @Before + @After | ✅ | LoggingAspect |
| AOP Performance @Around | ✅ | PerformanceAspect, seuil 1000ms |
| @Scheduled — IA automatique | ✅ | DeliveryRiskScheduler, 3 jobs |
| 4 détections IA (DELAY, BOTTLENECK, INACTIVITY, SCOPE_CREEP) | ✅ | Avec anti-doublon |
| Modèle Random Forest PMML | ✅ (**BONUS**) | 200 arbres, 8 features, JPMML 1.7.7 |
| Burndown Chart avec projection linéaire | ✅ (**BONUS**) | BurndownServiceImpl |
| Mailing Thymeleaf (2 templates HTML) | ✅ (**BONUS**) | Gmail SMTP, Feign → Module 01 |
| JWT Security (filter + validation Module 01) | ✅ (**BONUS**) | JwtAuthenticationFilter |
| Export PDF (iText 7) | ✅ | 4 sections |
| Export CSV | ✅ | Séparateur `;`, compatible Excel FR |
| Notifications (6 types) | ✅ | Badge Angular |
| Enrichissement projet (CIN + nom) | ✅ (**BONUS**) | Via UserServiceClient |
| Tests Unitaires JUnit + Mockito | ✅ (**BONUS**) | 5 classes, tests passants |
| Gantt Chart Angular CSS pur | ✅ (**BONUS**) | Zoom 3 niveaux, filtres, diamonds |
| Kanban Angular CDK Drag & Drop | ✅ | 4 colonnes |

---

*Documentation générée pour le projet TrustedWork Tunisia — Module 08 — Année Universitaire 2024-2025*  
*Spring Boot 3.5.13 | Angular 18 | MySQL | Maven | Port 8089*
