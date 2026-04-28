# 🛡️ TrustedWork Tunisia — Full Application Analysis

> **Platform type:** Multi-sided freelancing & agency management platform  
> **Architecture:** Monorepo with 3 sub-applications  
> **Status:** Running locally — Backend on port `8082`, Frontoffice on port `4200`

---

## 📐 Global Architecture Overview

```
TrustedWork-Tunisia/
├── backend/          ← Spring Boot 3.2 REST API (port 8082)
├── frontoffice/      ← Angular 18 — Freelancer/Client interface (port 4200)
└── backoffice/       ← Angular 18 — Admin panel (port 4201)
```

The system is designed as a **single backend API** (monolith with micro-service-ready structure) serving two distinct Angular frontends. All data persists in a **MySQL** database (`trustedwork_user_db`).

---

## 🗂️ 1. BACKEND — Spring Boot Service

**Package root:** `tn.esprit.userservice`  
**Port:** `8082`  
**Context path:** `/api`

### 1.1 Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Database | MySQL via Spring Data JPA / Hibernate |
| 2FA | TOTP (`dev.samstevens.totp`) |
| Email | Spring Mail (Gmail SMTP) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Cross-Cutting | Spring AOP |
| OAuth2 | Google (`google-api-client`) |
| HTTP Client | Spring Cloud OpenFeign |
| Boilerplate | Lombok |

---

### 1.2 Domain Entities (31 classes)

#### 👤 User & Identity
| Entity | Purpose |
|---|---|
| `User` | Core user record: email, CIN, password (BCrypt), role, KYC status, 2FA secret, trust level, liveness flag, lock state |
| `Role` | Enum — `FREELANCER`, `CLIENT`, `ADMIN` |
| `AccountStatus` | Enum — `ACTIVE`, `SUSPENDED`, `DELETED` |
| `KycStatus` | Enum — `PENDING`, `IN_REVIEW`, `APPROVED`, `REJECTED` |
| `KycRequest` | KYC submission: CIN doc path, selfie path, diploma path, liveness score, review decision |
| `PasswordResetToken` | Time-limited token for forgot-password flow |
| `SuspensionRecord` | Audit trail for admin suspensions (reason, suspendedBy, liftedAt) |
| `AuditLog` | Immutable event log (event type, actor, target, details) |
| `AuditEventType` | Enum — KYC_SUBMITTED, KYC_APPROVED, USER_SUSPENDED, USER_ACTIVATED, etc. |

#### 🏢 Agency
| Entity | Purpose |
|---|---|
| `Agency` | Agency profile: name, sector, country, city, logo, tier, active flag, `createdBy` (User) |
| `AgencyTier` | Enum — `STARTER`, `PRO`, `ENTERPRISE` |
| `AgencyMember` | Junction between User and Agency: `MemberRole` (LEAD/MEMBER), `MemberStatus`, `workloadScore`, `skills` |
| `MemberRole` | Enum — `LEAD`, `MEMBER` |
| `MemberStatus` | Enum — `ACTIVE`, `INACTIVE`, `SUSPENDED` |
| `AgencyInvitation` | Invitation sent by a LEAD to a Freelancer — tracks proposedRole, status, sentAt, respondedAt |
| `InvitationStatus` | Enum — `PENDING`, `ACCEPTED`, `DECLINED` |
| `AgencyJoinRequest` | Freelancer self-applies to join an agency |
| `JoinRequestStatus` | Enum — `PENDING`, `APPROVED`, `REJECTED` |
| `AgencyReview` | Client/member review of an agency (rating, comment, targetType) |
| `ReviewTargetType` | Enum — `AGENCY`, `MEMBER` |
| `AgencyPerformanceScore` | Computed score: `deliveryRate`, `clientSatisfaction`, `responseTime`, `memberRetention`, `totalScore` |
| `SkillCoverageAnalysis` | Per-project skill gap analysis for the agency |
| `CollaborationLog` | Log of collaboration events within an agency |

#### 📋 Project & Task
| Entity | Purpose |
|---|---|
| `TeamProject` | Project inside an agency: name, description, budget, `ProjectStatus`, `ProjectPriority`, progress %, dates |
| `ProjectStatus` | Enum — `EN_COURS`, `TERMINE`, `SUSPENDU` |
| `ProjectPriority` | Enum — `FAIBLE`, `MOYENNE`, `HAUTE`, `CRITIQUE` |
| `Task` | Task inside a project: title, description, `TaskStatus`, `TaskPriority`, `requiredSkills`, due date, `assignedMember` |
| `TaskStatus` | Enum — `BACKLOG`, `A_FAIRE`, `EN_COURS`, `EN_REVISION`, `TERMINE`, `ANNULE` |
| `TaskPriority` | Enum — `FAIBLE`, `MOYENNE`, `HAUTE`, `URGENTE` |
| `TaskAssignment` | Junction table between Task and AgencyMember — tracks `assignedAt`, `completedAt`, `completionScore` |
| `TaskComment` | Comments left on a task by agency members |

---

### 1.3 Controller Layer (23 REST controllers)

| Controller | Base Path | Key Operations |
|---|---|---|
| `AuthController` | `/auth` | Register, Login, Verify-2FA, Refresh Token |
| `GoogleOAuthController` | `/auth/google` | Google token exchange |
| `GoogleProfileController` | `/google-profile` | Fetch/sync Google profile |
| `IdentityController` | `/identity` | Get current user identity from JWT |
| `PasswordController` | `/auth` | Forgot-password, Reset-password |
| `UserController` | `/users` | Get all users, get by ID, update, delete (soft) |
| `KycController` | `/kyc` | Submit KYC (CIN + selfie + diploma) |
| `KycRequestController` | `/kyc-requests` | Admin: list pending, review (approve/reject) |
| `AdminController` | `/admin` | Admin actions: list users, suspend, lift, bulk ops |
| `SuspensionController` | `/suspensions` | Suspend user, lift suspension, history |
| `AgencyController` | `/agencies` | CRUD agencies, analytics, available freelancers, my-agencies |
| `AgencyInvitationController` | `/agencies/{id}/invitations` | Send, list, respond to invitations |
| `AgencyJoinRequestController` | `/agencies/{id}/join-requests` | Submit, approve, reject join requests |
| `AgencyMemberController` | `/agencies/{id}/members` | List members, update role, remove member |
| `InvitationController` | `/invitations` | Get invitations for current user |
| `TeamProjectController` | `/agencies/{id}/projects` | CRUD team projects |
| `TaskController` | `/agencies/{id}/tasks` | CRUD tasks, update status (with auto-assign trigger) |
| `TaskAssignmentController` | `/task-assignments` | Manual assign, auto-assign (3 strategies) |
| `TaskCommentController` | `/tasks/{id}/comments` | CRUD task comments |
| `AgencyPerformanceScoreController` | `/agencies/{id}/performance` | Save/get performance score |
| `CollaborationLogController` | `/agencies/{id}/collab-logs` | Get collaboration logs |
| `SkillCoverageAnalysisController` | `/agencies/{id}/skill-coverage` | Get/create skill coverage analysis |
| `AgencyReviewController` | `/agencies/{id}/reviews` | Submit and list agency reviews |

---

### 1.4 Service Layer — Functionality Breakdown

#### 🔐 AuthServiceImpl
- **Register:** Validates email + CIN uniqueness → hashes password (BCrypt) → assigns `Role` → calls `TrustLevelService.computeAndSave()` → generates JWT access + refresh tokens.
- **Login:** Finds user by email → checks temporary lockout → verifies password → checks account status → resets failed attempts → if 2FA enabled: returns `twoFactorRequired: true` without tokens; otherwise returns full JWT pair.
- **Verify 2FA:** Validates TOTP code via `TwoFactorService` → generates and returns JWT tokens.
- **Refresh Token:** Validates refresh JWT → generates new access token.
- **Brute-force protection:** 5 failed login attempts → 15-minute lock. Scheduler auto-unlocks expired locks every 5 minutes.

#### 🔑 TwoFactorServiceImpl
- Generates TOTP secret (QR code URL) using `dev.samstevens.totp`.
- Enables/disables 2FA per user.
- Verifies incoming TOTP codes.

#### 📧 EmailServiceImpl
3 email types, all sent as responsive HTML:
1. **Password Reset** — branded HTML with a CTA reset link (expires in 30 min).
2. **Simple email** — plain text for generic notifications.
3. **Auto-Assign Task email** — rich HTML card showing task name, project, priority badge (color-coded: red=URGENT, orange=MOYENNE, green=FAIBLE), deadline, description, and a CTA button.

#### 🪪 KycRequestServiceImpl
- **submitKycRequest:** Accepts CIN doc + selfie (mandatory) + diploma (optional). Computes `livenessScore` (currently simulated via random in range 0.60–0.99). If score ≥ 0.75, `livenessPassed = true`. Diploma-only submissions skip the liveness check. Sets user `kycStatus = IN_REVIEW` and triggers `TrustLevelService.computeAndSave()`.
- **reviewKycRequest:** Admin approves or rejects → updates `kycStatus` on both `KycRequest` and `User` → recomputes Trust Level → logs audit event.

#### 🏆 TrustLevelServiceImpl
Implements a **scoring algorithm (0–100 pts → level 1–5)**:
| Criterion | Points |
|---|---|
| KYC Approved | +40 |
| 2FA Enabled | +20 |
| Liveness Passed | +30 |
| Account Age (≥90 days) | +10 |
| Account Age (≥30 days) | +7 |
| Account Age (≥7 days) | +3 |

**Level thresholds:**
- Level 1: 0–19 (unverified)
- Level 2: 20–39 (2FA only)
- Level 3: 40–59 (KYC approved)
- Level 4: 60–79 (KYC + 2FA)
- Level 5: 80–100 (fully verified)

#### 🏢 AgencyServiceImpl
- **createAgency:** Creates agency → automatically creates LEAD `AgencyMember` for the creator.
- **getMyAgencyContext:** Returns a DTO with: `hasMemberships`, `ownsAnAgency`, list of all memberships, pending invitation count — used by frontend to route the user to the correct dashboard.
- **getAgencyAnalytics:** LEAD-only endpoint — returns: total/completed/cancelled task counts, average task lead time (days), top 10 members ranked by `completionScore`.
- **getAvailableFreelancers:** Filters users with FREELANCER role who are not yet members of the agency, supports skill and name search.

#### 🤝 AgencyInvitationServiceImpl
- LEAD sends invitation to a freelancer → checks no duplicate pending invite, no existing membership.
- Freelancer accepts → automatically creates `AgencyMember` with `MEMBER` role and `ACTIVE` status. Invitation is deleted after response.
- Prints console `[NOTIFICATION]` placeholder for future in-app notifications.

#### 📋 TaskServiceImpl
- **createTask:** LEAD creates a task inside a project → optionally assigns it to a member → creates `TaskAssignment` record.
- **updateTaskStatus:** Both LEADs and the assigned member can update status. On `TERMINE` or `ANNULE`, the task's `TaskAssignment` is marked with `completionScore=100` and `completedAt`, then `autoAssignTasks()` is called for that member.
- **autoAssignTasks():** The intelligent auto-assignment engine:
  1. Verifies the freed member has zero remaining active tasks.
  2. Fetches unassigned `BACKLOG` tasks ordered by priority.
  3. Assigns the highest-priority task to the member → sets status to `A_FAIRE`.
  4. Sends an `AutoAssignTask` email notification.

#### ⚙️ TaskAssignmentServiceImpl
Provides 3 explicit assignment strategies callable via API:
1. **`autoAssignTask`** (by workload): assigns to the member with the fewest total task assignments.
2. **`autoAssignBySkills`**: assigns to the member with the highest skill-match score vs. `task.requiredSkills` (comma-separated match).
3. **`autoAssignSmart`** (hybrid): score = `(skillMatch × 2) - workload` → best combined fit wins.

#### 📊 AgencyPerformanceScoreServiceImpl
Computes a weighted total agency score:
```
totalScore = (deliveryRate × 0.35) + (clientSatisfaction × 0.30) + (responseTime × 0.20) + (memberRetention × 0.15)
```

#### 🚫 SuspensionServiceImpl
- **suspendUser:** Sets `accountStatus=SUSPENDED`, disables the account, creates a `SuspensionRecord`, logs `USER_SUSPENDED` audit event.
- **liftSuspension:** Re-enables account, closes active `SuspensionRecord`, logs `USER_ACTIVATED`.

#### 🔍 Google OAuth (GoogleOAuthServiceImpl + GoogleProfileServiceImpl)
- Verifies Google ID tokens server-side using `google-api-client`.
- Fetches and syncs Google profile data (photo, name) into the local `User` record.

#### 📡 AOP Cross-Cutting
- **LoggingAspect:** Intercepts all service method calls, logs entry/exit with method name and arguments.
- **PerformanceAspect:** Measures execution time of service methods and logs warnings for slow calls.

---

### 1.5 Scheduler (UserScheduler — 4 tasks)

| Job | Schedule | Purpose |
|---|---|---|
| `unlockExpiredAccounts` | Every 5 min | Auto-unlocks accounts locked by brute-force after `lockedUntil` expires |
| `recomputeTrustLevels` | Daily at 03:00 | Recomputes Trust Level for ALL active users (account age evolves) |
| `logDeletedUsersCount` | Daily at 02:00 | Logs soft-deleted users count |
| `logSuspendedUsersCount` | Daily at 02:30 | Logs actively suspended accounts count |

---

### 1.6 Security Architecture
- **`JwtService`:** Generates access tokens (24h) and refresh tokens (7 days) signed with a 256-bit secret. Extracts email from claims.
- **`JwtAuthenticationFilter`:** Spring Security filter that intercepts every request, extracts Bearer token, validates it, and injects authentication into `SecurityContext`.
- CORS is configured to allow the Angular apps on `localhost:4200` and `localhost:4201`.
- All `/auth/**` and `/google-**` endpoints are public. All others require a valid JWT.

---

## 🌐 2. FRONTOFFICE — Angular 18 (Freelancer/Client App)

**Port:** `4200`  
**Users:** Freelancers and Clients

### 2.1 Route Map

```
/                        ← Landing page (PublicLayout)
/auth/login              ← Login
/auth/register           ← Register
/auth/forgot-password    ← Forgot password
/auth/reset-password     ← Reset password (token from email)
/auth/2fa                ← Two-Factor verification
/auth/complete-profile   ← Profile completion step after register

/app/dashboard           ← Overview dashboard (protected)
/app/profile             ← Profile module (lazy-loaded)
/app/agencies            ← Agencies module (lazy-loaded)
```

**Guards:**
- `authGuard` → redirects to `/auth/login` if no valid session.
- `completeProfileGuard` → redirects to `/auth/complete-profile` if profile is incomplete.

### 2.2 Core Layer

#### Services
| Service | Responsibility |
|---|---|
| `ApiService` | Central HTTP wrapper around Angular `HttpClient` — base URL `http://localhost:8082/api` |
| `AuthService` | Login, register, forgot/reset password, 2FA flow, JWT session management (localStorage/sessionStorage) |
| `UserService` | Fetch/update user profile, KYC submission |
| `TwoFactorService` | Setup 2FA (get QR code), enable/disable, verify code |
| `GoogleOAuthService` | Google Sign-In via the Google Identity Services library |

#### Session Management (`AuthService`)
- On `rememberMe = true` → tokens saved to **localStorage** (persistent).
- On `rememberMe = false` → tokens saved to **sessionStorage** (tab-only).
- Pending 2FA state saved to sessionStorage with timestamp — valid for 10 minutes.

#### `TokenInterceptor`
Automatically injects the `Authorization: Bearer <token>` header on every outgoing HTTP request.

#### Models
- `User` — id, firstName, lastName, email, role, photo, headline, location, bio, skills, experience, availability, trustLevel, kycStatus, twoFactorEnabled
- `Agency` — id, name, description, logoUrl, sector, tier, country, city, members, projects, invitations
- `AuthResponse` / `AuthUser` — login/register API response contracts

### 2.3 Agencies Feature Module

The main functional module — accessible at `/app/agencies`.

#### Pages
| Page | Path | Description |
|---|---|---|
| `AgencyListComponent` | `/app/agencies` | Browsable list of all active agencies |
| `AgencyDetailComponent` | `/app/agencies/:id` | Agency public profile — info, members, reviews |
| `MyAgenciesComponent` | `/app/agencies/my-agencies` | Current user's memberships dashboard — detects role (LEAD/MEMBER) |
| `MyInvitationsComponent` | `/app/agencies/my-invitations` | List of pending invitations with accept/decline buttons |
| `OwnerDashboardComponent` | `/app/agencies/:id/dashboard` | LEAD-only dashboard: analytics, member rankings, task overview |

#### Components (inside agency detail/dashboard)
| Component | Purpose |
|---|---|
| `AgencyFormComponent` | Create or edit agency details |
| `AgencyMembersComponent` | Show members list, promote/demote roles (LEAD ↔ MEMBER), remove member |
| `AgencyInvitationsComponent` | LEAD view: list sent invitations, cancel pending ones, invite new freelancers |
| `AgencyJoinRequestsComponent` | LEAD view: list join requests, approve or reject |
| `TaskKanbanComponent` | Kanban board (BACKLOG → À FAIRE → EN COURS → EN REVISION → TERMINÉ/ANNULÉ) — drag-friendly status updates |
| `TeamProjectsComponent` | CRUD for team projects within an agency |
| `PerformanceDashboardComponent` | Charts and stats: completion rates, member rankings (uses Chart.js) |
| `CollaborationLogsComponent` | Display collaboration events timeline |

#### Agency Services (frontend)
| Service | Calls |
|---|---|
| `AgencyService` | CRUD agencies, get analytics, get available freelancers, getMyAgencyContext |
| `AgencyInvitationService` | Send/list/cancel invitations, accept/decline |
| `AgencyMemberService` | List members, change role, remove |
| `TaskService` | List tasks by agency/project, update task status |
| `TaskAssignmentService` | Manual assign, auto-assign strategies |
| `TeamProjectService` | CRUD team projects |

---

## 🖥️ 3. BACKOFFICE — Angular 18 (Admin Panel)

**Port:** `4201` (assumed — runs separately with `ng serve`)  
**Users:** Platform administrators (ADMIN role only)

### 3.1 Route Map

```
/auth/login          ← Admin login
/auth/auto-login     ← Auto-login bridge (token passthrough)

/admin/dashboard     ← Statistics overview
/admin/users         ← Full user list
/admin/users/:id     ← User detail + actions
/admin/users/kyc     ← KYC request management queue
/admin/audit-logs    ← Immutable audit event log
/admin/suspensions   ← Active suspensions management
```

All `/admin/*` routes are protected by `authGuard`.

### 3.2 Admin Features

#### Users Management (`/admin/users`)
- Lists all platform users with pagination.
- Searchable/filterable by role, KYC status, account status.
- Click-through to user detail page.

#### User Detail (`/admin/users/:id`)
- View full user profile: email, CIN, role, Trust Level, KYC status, 2FA status.
- **Suspend** a user with a reason → calls `SuspensionService`.
- **Lift suspension** → reactivates the account.
- View suspension history.

#### KYC Management (`/admin/users/kyc`)
- Lists all pending KYC requests (`IN_REVIEW` status).
- Admin can view document paths (CIN, selfie, diploma) and liveness score.
- **Approve** or **Reject** (with rejection reason) → updates user's KYC status and recalculates Trust Level.

#### Audit Logs (`/admin/audit-logs`)
- Read-only chronological list of all audit events (KYC submissions, approvals, rejections, suspensions, reactivations).
- Shows: event type, actor (admin email), target user, timestamp, details.

#### Suspensions (`/admin/suspensions`)
- Lists all currently active suspensions.
- Shows: user email, reason, suspended by, suspended at.
- Allows lifting individual suspensions.

#### Admin Auth
- Uses the same JWT-based login flow as the frontoffice.
- `auto-login` route allows token injection for SSO-like flows.

---

## 🔄 4. Key Cross-Cutting Flows

### Flow 1: Registration & Trust Level Bootstrap
```
Register → Hash password → Save User → computeAndSave(User) → trustLevel = 1 (new user, no KYC) → Return JWT
```

### Flow 2: Login with 2FA
```
POST /auth/login
  → Verify lock → Verify password → Check status
  → 2FA enabled? → Return {twoFactorRequired: true}
  → Frontend redirects to /auth/2fa
  → POST /auth/verify-2fa → Validate TOTP → Return JWT
```

### Flow 3: KYC Verification Pipeline
```
User submits CIN + selfie + optional diploma
  → Compute liveness score (simulated)
  → kycStatus = IN_REVIEW → TrustLevel recalculated
  → Admin reviews in backoffice
  → APPROVE/REJECT → kycStatus updated → TrustLevel recalculated
  → KYC APPROVED adds +40 pts → possible upgrade from level 1→3 or higher
```

### Flow 4: Agency Creation & Onboarding
```
Freelancer creates agency
  → Agency saved → LEAD AgencyMember created automatically
  → LEAD can invite freelancers via invitations
  → Freelancer accepts → MEMBER AgencyMember created
  → LEAD can also approve join requests from freelancers who apply directly
```

### Flow 5: Task Auto-Assignment Engine
```
AgencyMember marks task as TERMINÉ
  → TaskAssignment updated (completionScore=100, completedAt=now)
  → autoAssignTasks(agencyId, member) triggered
  → Check member has 0 remaining active tasks
  → Find highest-priority unassigned BACKLOG task
  → Assign to member → status: BACKLOG → À_FAIRE
  → Send branded HTML email notification to member
```

### Flow 6: Smart Manual Assignment
```
LEAD triggers POST /task-assignments/auto-assign-smart?taskId=X&agencyId=Y
  → For each active member: skillMatchScore + workloadScore
  → finalScore = (skillMatch × 2) - currentWorkload
  → Assign to highest scorer
  → Send email notification
```

---

## 📊 5. Data Model Relationship Summary

```
User
 ├── has many KycRequest
 ├── has many SuspensionRecord
 ├── has many AuditLog (as actor or target)
 └── has many AgencyMember (memberships)

Agency
 ├── createdBy → User
 ├── has many AgencyMember
 ├── has many TeamProject
 ├── has many AgencyInvitation
 ├── has many AgencyJoinRequest
 ├── has one AgencyPerformanceScore
 ├── has many SkillCoverageAnalysis
 ├── has many CollaborationLog
 └── has many AgencyReview

TeamProject
 ├── belongs to Agency
 ├── createdByMember → AgencyMember
 ├── assigned to many AgencyMember (M:M via team_project_assignments)
 └── has many Task

Task
 ├── belongs to TeamProject
 ├── belongs to Agency
 ├── createdBy → AgencyMember
 ├── assignedMember → AgencyMember
 ├── has many TaskComment
 └── has many TaskAssignment

TaskAssignment
 ├── belongs to Task
 ├── belongs to AgencyMember
 └── tracks: assignedAt, completedAt, completionScore
```

---

## ⚠️ 6. Notable Design Decisions & Observations

> [!NOTE]
> **Liveness score is simulated.** `computeLivenessScore()` returns a random value between 0.60–0.99. In production, this would call a face-matching AI API (e.g., AWS Rekognition, Azure Face API).

> [!NOTE]
> **In-app notifications are stubs.** Both `AgencyInvitationServiceImpl` and `AgencyJoinRequestServiceImpl` print `[NOTIFICATION]` to console. A real WebSocket or push notification system would replace these.

> [!WARNING]
> **Sensitive credentials in `application.properties`.** The Gmail SMTP app password and JWT secret are hardcoded. These should be moved to environment variables or a secrets manager before production.

> [!TIP]
> **The `agency.service.url` property** points to `http://localhost:8089/api` — this suggests the system was originally designed as a microservices architecture with a separate agency microservice, but currently everything is consolidated in the single backend at port 8082.

> [!NOTE]
> **Dual assignment tracking.** A `Task` has both a direct `assignedMember` FK and a `TaskAssignment` junction table. The junction table supports multi-assignment history and completion scoring, while the FK is used for quick UI rendering (Kanban board).

---

## 🧩 7. Feature Completeness Summary

| Feature | Status |
|---|---|
| JWT Auth (Register/Login/Refresh) | ✅ Complete |
| Brute-force Lock (5 attempts, 15 min) | ✅ Complete |
| TOTP Two-Factor Auth | ✅ Complete |
| Google OAuth2 Login | ✅ Complete |
| Forgot/Reset Password (email link) | ✅ Complete |
| KYC Document Submission | ✅ Complete (liveness simulated) |
| KYC Admin Review | ✅ Complete |
| Trust Level Algorithm (5 levels) | ✅ Complete |
| Account Suspension / Lift | ✅ Complete |
| Audit Log | ✅ Complete |
| Scheduled Jobs (4 tasks) | ✅ Complete |
| AOP Logging & Performance | ✅ Complete |
| Agency CRUD | ✅ Complete |
| Agency Invitations Flow | ✅ Complete |
| Agency Join Requests Flow | ✅ Complete |
| Agency Member Role Management | ✅ Complete |
| Team Projects CRUD | ✅ Complete |
| Task CRUD + Status Board | ✅ Complete |
| Auto-Assign on Task Completion | ✅ Complete |
| Smart Auto-Assign (skill+workload) | ✅ Complete |
| Email Notifications (HTML) | ✅ Complete |
| Agency Performance Score | ✅ Complete |
| Agency Analytics (rankings, metrics) | ✅ Complete |
| Skill Coverage Analysis | ✅ Complete (entity + controller) |
| Collaboration Logs | ✅ Complete (entity + controller) |
| Agency Reviews | ✅ Complete |
| Backoffice KYC Management | ✅ Complete |
| Backoffice Suspension Management | ✅ Complete |
| Backoffice Audit Logs | ✅ Complete |
| In-app Notifications | 🟡 Console stubs only |
| Real Liveness Detection | 🟡 Simulated |
| Profile Completion Guard | ✅ Complete |
| Remember Me (localStorage/session) | ✅ Complete |
