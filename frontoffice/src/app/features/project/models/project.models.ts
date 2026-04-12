// ══════════════════════════════════════════════
// Models — Module 08 : Project Management
// ══════════════════════════════════════════════

export type ProjectStatus = 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type DeliverableStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';
export type RiskType = 'DELAY_RISK' | 'BOTTLENECK' | 'INACTIVITY' | 'SCOPE_CREEP';
export type RiskSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type NotificationType =
  | 'DEADLINE_24H'
  | 'DELIVERABLE_PENDING'
  | 'DELIVERABLE_REVIEWED'
  | 'RISK_DETECTED'
  | 'TASK_BLOCKED'
  | 'WEEKLY_REPORT';

// ── Project ──────────────────────────────────

export interface Project {
  id: number;
  title: string;
  description: string;
  status: ProjectStatus;
  contractId: number;
  clientId: number;
  freelancerId: number;
  startDate: string;
  endDate: string;
  completionRate: number;
  budget: number;
  createdAt: string;
  updatedAt: string;

  // Champs enrichis depuis le User Service (présents sur /my et /enriched)
  clientCin?: number;
  clientFirstName?: string;
  clientLastName?: string;
  clientEmail?: string;
  freelancerCin?: number;
  freelancerFirstName?: string;
  freelancerLastName?: string;
  freelancerEmail?: string;
}

// ── Task ─────────────────────────────────────

export interface Task {
  id: number;
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  projectId: number;
  assigneeId: number;
  deadline: string;
  estimatedHours: number;
  actualHours: number;
  createdAt: string;
  updatedAt: string;
}

// ── SubTask ──────────────────────────────────

export interface SubTask {
  id: number;
  title: string;
  done: boolean;
  taskId: number;
  createdAt: string;
}

// ── Deliverable ──────────────────────────────

export interface Deliverable {
  id: number;
  title: string;
  description: string;
  fileUrl: string;
  status: DeliverableStatus;
  taskId: number | null;
  projectId: number;
  submittedAt: string;
  reviewedAt: string | null;
  reviewComment: string | null;
}

// ── ProgressReport ───────────────────────────

export interface ProgressReport {
  id: number;
  projectId: number;
  completedTasks: number;
  totalTasks: number;
  completionRate: number;
  openDeliverables: number;
  activeRisks: number;
  summary: string;
  generatedAt: string;
}

// ── DeliveryRiskSignal ───────────────────────

export interface DeliveryRiskSignal {
  id: number;
  projectId: number;
  riskType: RiskType;
  severity: RiskSeverity;
  message: string;
  affectedTaskId: number | null;
  resolved: boolean;
  detectedAt: string;
  resolvedAt: string | null;
}

// ── Notification ─────────────────────────────

export interface ProjectNotification {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: NotificationType;
  projectId: number;
  taskId: number | null;
  read: boolean;
  createdAt: string;
}

// ── DTOs pour création / mise à jour ─────────

export interface CreateProjectDTO {
  title: string;
  description: string;
  contractId: number;
  clientId: number;
  freelancerId: number;
  startDate: string;
  endDate: string;
  budget: number;
}

export interface CreateTaskDTO {
  title: string;
  description: string;
  priority: TaskPriority;
  assigneeId: number;
  deadline: string;
  estimatedHours: number;
}

export interface CreateSubTaskDTO {
  title: string;
}

export interface CreateDeliverableDTO {
  title: string;
  description: string;
  fileUrl: string;
  taskId?: number;
}