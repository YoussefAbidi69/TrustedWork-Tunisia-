# 🖥️ Frontend — Module 08 : Project Management & Delivery Intelligence

> **TrustedWork Tunisia** — Angular 18 | Feature `project`  
> Année Universitaire 2024-2025

---

## 📁 Localisation dans le projet

```
src/
├── app/
│   ├── core/
│   │   ├── guards/
│   │   │   ├── auth.guard.ts
│   │   │   └── complete-profile.guard.ts
│   │   ├── interceptors/
│   │   │   └── token.interceptor.ts         ← Injection automatique du JWT
│   │   ├── models/
│   │   │   └── user.model.ts
│   │   └── services/
│   │       └── auth.service.ts              ← getCurrentAuthUser(), userId, role
│   │
│   └── features/
│       └── project/                         ← 📦 TON MODULE
│           ├── project.module.ts
│           ├── project-routing.module.ts
│           ├── models/
│           │   └── project.models.ts        ← Tous les types TypeScript
│           ├── services/
│           │   └── project-api.service.ts   ← Toutes les requêtes HTTP
│           └── pages/
│               ├── project-list/
│               ├── project-detail/
│               ├── kanban-board/
│               ├── deliverables/
│               ├── risk-signals/
│               ├── gantt-chart/
│               ├── burndown/
│               └── notifications/
```

---

## 🛠️ Stack & Dépendances

| Technologie | Usage |
|-------------|-------|
| Angular 18 | Framework principal |
| Angular CDK — `DragDropModule` | Kanban Drag & Drop |
| `HttpClient` | Appels REST vers `ms-project-service` (port 8090) |
| `AuthService` | Lecture du JWT (userId, role : CLIENT / FREELANCER / ADMIN) |
| `TokenInterceptor` | Injection automatique du Bearer token sur chaque requête |
| SVG natif | Burndown Chart et Gantt Chart (sans librairie externe) |

---

## 🗺️ Routes Angular

Toutes les routes sont **lazy-loaded** dans `ProjectModule` via `project-routing.module.ts`.

| Route | Composant | Description |
|-------|-----------|-------------|
| `/projects` | `ProjectListComponent` | Liste de tous mes projets |
| `/projects/notifications` | `NotificationsComponent` | Centre de notifications |
| `/projects/:id` | `ProjectDetailComponent` | Dashboard du projet |
| `/projects/:id/kanban` | `KanbanBoardComponent` | Tableau Kanban |
| `/projects/:id/deliverables` | `DeliverablesComponent` | Gestion des livrables |
| `/projects/:id/risks` | `RiskSignalsComponent` | Signaux de risque IA |
| `/projects/:id/gantt` | `GanttChartComponent` | Diagramme de Gantt |
| `/projects/:id/burndown` | `BurndownComponent` | Burndown Chart |

---

## 📐 Modèles TypeScript (`project.models.ts`)

### Types & Enums

```typescript
type ProjectStatus   = 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
type TaskStatus      = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
type TaskPriority    = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
type DeliverableStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';
type RiskType        = 'DELAY_RISK' | 'BOTTLENECK' | 'INACTIVITY' | 'SCOPE_CREEP';
type RiskSeverity    = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
type NotificationType = 'DEADLINE_24H' | 'DELIVERABLE_PENDING' | 'DELIVERABLE_REVIEWED'
                      | 'RISK_DETECTED' | 'TASK_BLOCKED' | 'WEEKLY_REPORT';
```

### Interfaces principales

| Interface | Description |
|-----------|-------------|
| `Project` | Entité centrale — inclut les champs enrichis `clientFirstName`, `freelancerEmail`, etc. |
| `Task` | Unité de travail — champ `status` pilote le Kanban |
| `SubTask` | Décomposition granulaire — booléen `done` |
| `Deliverable` | Livrable soumis par le freelancer |
| `ProgressReport` | Snapshot automatique de l'état du projet |
| `DeliveryRiskSignal` | Alerte IA générée par le scheduler backend |
| `ProjectNotification` | Notification interne (persistée en DB) |
| `MLPrediction` | Résultat du modèle Random Forest (PMML) |
| `BurndownChart` | Données du burndown chart avec courbes idéale et réelle |
| `TaskSuggestionResponse` | Suggestions IA de tâches à créer |

### DTOs pour la création

```typescript
interface CreateTaskDTO {
  title: string;
  description: string;
  priority: TaskPriority;
  assigneeId: number;
  deadline: string;
  estimatedHours: number;
}
```

---

## 🔌 Service HTTP (`project-api.service.ts`)

Base URL : `/api` (proxy vers `ms-project-service` port 8090)

### Projects

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `getMyProjects()` | `GET /projects/my` | Projets de l'utilisateur connecté (JWT) |
| `getProjectByIdEnriched(id)` | `GET /projects/:id/enriched` | Projet + infos client/freelancer |
| `updateProjectStatus(id, status)` | `PATCH /projects/:id/status` | Changer le statut |

### Tasks

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `createTask(projectId, dto)` | `POST /projects/:id/tasks` | Créer une tâche |
| `updateTaskStatus(id, status)` | `PATCH /tasks/:id/status` | Déplacer une carte Kanban |
| `assignTask(id, assigneeId)` | `PATCH /tasks/:id/assign` | Assigner une tâche |

### SubTasks

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `toggleSubTask(id)` | `PATCH /subtasks/:id/toggle` | Cocher / décocher |

### Deliverables

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `submitDeliverable(projectId, dto)` | `POST /projects/:id/deliverables` | Soumettre |
| `reviewDeliverable(id, status, comment)` | `PATCH /deliverables/:id/review` | Approuver/Rejeter |

### IA & ML

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `analyzeRisks(projectId)` | `POST /projects/:id/risks/analyze` | Déclencher l'analyse IA |
| `predictDeliveryRisk(projectId)` | `GET /projects/:id/risks/ml-predict` | Prédiction Random Forest |
| `getSuggestedTasks(projectId)` | `GET /projects/:id/suggest-tasks` | Suggestions IA de tâches |
| `getBurndownChart(projectId)` | `GET /projects/:id/burndown` | Données burndown |

### Export

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `exportPdf(projectId)` | `GET /projects/:id/export/pdf` | Télécharger PDF (`Blob`) |
| `exportCsv(projectId)` | `GET /projects/:id/export/csv` | Télécharger CSV (`Blob`) |

---

## 📄 Pages & Composants

### `ProjectListComponent` — `/projects`

Vue d'entrée après connexion. Affiche tous les projets de l'utilisateur via `getMyProjects()`. Gestion des rôles : CLIENT, FREELANCER, ADMIN. Navigation vers `ProjectDetailComponent` au clic.

---

### `ProjectDetailComponent` — `/projects/:id`

Dashboard complet du projet. Charge en parallèle via `forkJoin` :
- Détails projet enrichis (noms client/freelancer)
- Liste des tâches
- Livrables
- Signaux de risque actifs
- Dernier rapport de progression
- **Prédiction ML** `MLPrediction` (Random Forest)

Affiche le taux de complétion, les métriques clés, et la prédiction IA en temps réel.

---

### `KanbanBoardComponent` — `/projects/:id/kanban`

4 colonnes : **TODO → IN_PROGRESS → IN_REVIEW → DONE**

- **Angular CDK DragDropModule** : chaque glisser-déposer appelle `updateTaskStatus()`
- Création de tâches avec formulaire inline
- **Suggestion IA** : bouton "Suggérer des tâches" → appelle `getSuggestedTasks()` et pré-remplit le formulaire
- Gestion des sous-tâches avec toggle `done`
- Filtrage par priorité et par assignee
- Accès conditionnel selon le rôle (CLIENT ne peut pas créer de tâches)

---

### `DeliverablesComponent` — `/projects/:id/deliverables`

- Liste des livrables avec filtre par statut (ALL / SUBMITTED / APPROVED / REJECTED)
- **Freelancer** : modal de soumission avec titre, description, URL fichier, tâche liée (optionnel)
- **Client** : boutons Approuver / Rejeter avec champ commentaire obligatoire
- Appelle `reviewDeliverable()` → `PATCH /deliverables/:id/review`

---

### `RiskSignalsComponent` — `/projects/:id/risks`

- Affiche tous les signaux IA actifs avec icônes de sévérité (LOW → CRITICAL)
- Bouton "Analyser maintenant" → `analyzeRisks()` → `POST /risks/analyze`
- **Prédiction ML** : section dédiée affichant `probabilityLate`, `criticalFeature`, `severity`
- Bouton "Résoudre" par signal → `resolveRisk()`
- Badges colorés par `RiskType` (DELAY_RISK, BOTTLENECK, INACTIVITY, SCOPE_CREEP)

---

### `GanttChartComponent` — `/projects/:id/gantt`

Timeline SVG native (sans librairie externe). Calcul des positions en pourcentage selon `startDate`/`endDate` du projet. Affiche chaque tâche comme une barre horizontale colorée par statut, avec indicateur de retard.

---

### `BurndownComponent` — `/projects/:id/burndown`

SVG natif (viewport 800×300). Données depuis `getBurndownChart()` :
- `courbeIdeale` — progression théorique linéaire
- `courbeReelle` — progression réelle des tâches `DONE`
- Statut affiché : `EN_AVANCE` / `DANS_LES_DELAIS` / `EN_RETARD` / `CRITIQUE`
- Métriques : `retardEstimeJours`, `velociteJournaliere`, `dateLivraisonProjetee`

---

### `NotificationsComponent` — `/projects/notifications`

Centre de notifications de l'utilisateur connecté.

- Filtre : ALL / UNREAD / READ
- Icônes par type (`DEADLINE_24H`, `RISK_DETECTED`, `WEEKLY_REPORT`, etc.)
- Bouton "Tout marquer comme lu" → `markAllAsRead(userId)`
- Rafraîchissement du badge dans la navbar via `getUnreadCount()`

---

## 🔐 Gestion des rôles

Le rôle est lu depuis `AuthService.getCurrentAuthUser()` à l'initialisation de chaque composant.

| Rôle | Permissions |
|------|------------|
| `FREELANCER` | Créer tâches, soumettre livrables, voir les risques |
| `CLIENT` | Approuver/rejeter livrables, voir le tableau de bord |
| `ADMIN` | Accès complet |

---

## ⚙️ Configuration proxy

Le `TokenInterceptor` injecte automatiquement le header `Authorization: Bearer <token>` sur toutes les requêtes HTTP. Aucune configuration manuelle du token n'est nécessaire dans les composants.

La constante `GATEWAY_URL = '/api'` dans `project-api.service.ts` doit correspondre au proxy Angular configuré dans `proxy.conf.json` pointant vers `http://localhost:8090`.

---

## 🚀 Lancement

```bash
# Installer les dépendances
npm install

# Lancer le serveur de développement
ng serve

# Accès
http://localhost:4200
```

> Le backend `ms-project-service` doit être démarré sur le port `8090` avant de lancer le frontend.

---

*Frontend Angular 18 — Module 08 — TrustedWork Tunisia | 2024-2025*
