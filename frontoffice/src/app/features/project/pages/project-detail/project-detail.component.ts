import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectApiService } from '../../services/project-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  Project, Task, Deliverable, DeliveryRiskSignal, ProgressReport,
  TaskStatus, TaskPriority, RiskSeverity
} from '../../models/project.models';

@Component({
  selector: 'app-project-detail',
  templateUrl: './project-detail.component.html',
  styleUrls: ['./project-detail.component.css']
})
export class ProjectDetailComponent implements OnInit {

  projectId!: number;
  project!: Project;
  tasks: Task[] = [];
  deliverables: Deliverable[] = [];
  risks: DeliveryRiskSignal[] = [];
  report: ProgressReport | null = null;

  loading = true;
  error = '';
  analyzingRisks = false;

  // ── Rôles ──
  currentUserId = 0;
  currentRole = '';
  isClient = false;
  isFreelancer = false;
  isAdmin = false;

  // Modal création tâche
  showTaskModal = false;
  newTask = { title: '', description: '', priority: 'MEDIUM' as TaskPriority, deadline: '', estimatedHours: 8 };

  constructor(
    private route: ActivatedRoute,
    public router: Router,
    private projectApi: ProjectApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));

    const user = this.authService.getCurrentAuthUser();
    this.currentUserId = user?.userId || 0;
    this.currentRole = user?.role?.toUpperCase() || '';
    this.isAdmin = this.currentRole === 'ADMIN';

    this.loadAll();
  }

 private loadAll(): void {
  this.loading = true;
  forkJoin({
    project: this.projectApi.getProjectById(this.projectId),
    tasks: this.projectApi.getTasksByProjectId(this.projectId),
    deliverables: this.projectApi.getDeliverablesByProjectId(this.projectId),
    risks: this.projectApi.getActiveRisks(this.projectId),
    report: this.projectApi.generateReport(this.projectId)
  }).subscribe({
    next: (data) => {
      this.project = data.project;
      this.tasks = data.tasks;
      this.deliverables = data.deliverables;
      this.risks = data.risks;
      this.report = data.report;

      // ✅ CORRECTION : Le rôle vient du JWT, PAS de la comparaison clientId/freelancerId
      // On garde isClient/isFreelancer basés sur le rôle JWT
      this.isClient = this.currentRole === 'CLIENT';
      this.isFreelancer = this.currentRole === 'FREELANCER';
      // isAdmin est déjà set dans ngOnInit

      this.loading = false;
    },
    error: (err) => {
      this.error = 'Impossible de charger le projet.';
      this.loading = false;
      console.error(err);
    }
  });
}

  /** Le freelancer et l'admin peuvent créer des tâches, pas le client */
  get canCreateTask(): boolean {
    return this.isFreelancer || this.isAdmin;
  }

  /** Le client peut reviewer les livrables */
  get canReviewDeliverables(): boolean {
    return this.isClient || this.isAdmin;
  }

  /** Le freelancer peut soumettre des livrables */
  get canSubmitDeliverables(): boolean {
    return this.isFreelancer || this.isAdmin;
  }

  /** L'admin peut supprimer / modifier */
  get canDelete(): boolean {
    return this.isAdmin;
  }

  /** L'admin et le freelancer peuvent lancer l'analyse IA */
  get canAnalyzeRisks(): boolean {
    return this.isFreelancer || this.isAdmin;
  }

  // ══════════════════════════════════════
  // STATS CALCULÉES (inchangées)
  // ══════════════════════════════════════

  get tasksByStatus(): Record<TaskStatus, Task[]> {
    const grouped: Record<string, Task[]> = { TODO: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] };
    this.tasks.forEach(t => {
      if (grouped[t.status]) grouped[t.status].push(t);
    });
    return grouped as Record<TaskStatus, Task[]>;
  }

  get daysRemaining(): number {
    if (!this.project?.endDate) return 0;
    const end = new Date(this.project.endDate);
    const now = new Date();
    const diff = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return Math.max(0, diff);
  }

  get isOverdue(): boolean {
    return this.daysRemaining === 0 && this.project?.status === 'ACTIVE';
  }

  get criticalRisks(): DeliveryRiskSignal[] {
    return this.risks.filter(r => r.severity === 'CRITICAL' || r.severity === 'HIGH');
  }

  get pendingDeliverables(): Deliverable[] {
    return this.deliverables.filter(d => d.status === 'SUBMITTED');
  }

  get approvedDeliverables(): Deliverable[] {
    return this.deliverables.filter(d => d.status === 'APPROVED');
  }

  // ══════════════════════════════════════
  // ACTIONS (inchangées)
  // ══════════════════════════════════════

  goToKanban(): void {
    this.router.navigate(['/app/projects', this.projectId, 'kanban']);
  }

  goToDeliverables(): void {
    this.router.navigate(['/app/projects', this.projectId, 'deliverables']);
  }

  triggerAiAnalysis(): void {
    this.analyzingRisks = true;
    this.projectApi.analyzeRisks(this.projectId).subscribe({
      next: () => {
        this.projectApi.getActiveRisks(this.projectId).subscribe(risks => {
          this.risks = risks;
          this.analyzingRisks = false;
        });
      },
      error: () => this.analyzingRisks = false
    });
  }

  resolveRisk(riskId: number): void {
    this.projectApi.resolveRisk(riskId).subscribe({
      next: () => {
        this.risks = this.risks.filter(r => r.id !== riskId);
      }
    });
  }

  exportPdf(): void {
    this.projectApi.exportPdf(this.projectId).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `projet-${this.projectId}-rapport.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  exportCsv(): void {
    this.projectApi.exportCsv(this.projectId).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `projet-${this.projectId}-taches.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  // ══════════════════════════════════════
  // CRÉATION TÂCHE (Modal)
  // ══════════════════════════════════════

  openTaskModal(): void {
    this.newTask = { title: '', description: '', priority: 'MEDIUM', deadline: '', estimatedHours: 8 };
    this.showTaskModal = true;
  }

  closeTaskModal(): void {
    this.showTaskModal = false;
  }

  submitNewTask(): void {
    if (!this.newTask.title.trim()) return;
    const user = this.authService.getCurrentAuthUser();
    this.projectApi.createTask(this.projectId, {
      title: this.newTask.title,
      description: this.newTask.description,
      priority: this.newTask.priority,
      assigneeId: user?.userId || 0,
      deadline: this.newTask.deadline,
      estimatedHours: this.newTask.estimatedHours
    }).subscribe({
      next: (task) => {
        this.tasks.push(task);
        this.showTaskModal = false;
      }
    });
  }

  // ══════════════════════════════════════
  // HELPERS (inchangés)
  // ══════════════════════════════════════

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'Actif', ON_HOLD: 'En pause', COMPLETED: 'Terminé', CANCELLED: 'Annulé',
      TODO: 'À faire', IN_PROGRESS: 'En cours', IN_REVIEW: 'En review', DONE: 'Terminée',
      SUBMITTED: 'En attente', APPROVED: 'Approuvé', REJECTED: 'Rejeté'
    };
    return map[status] || status;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'badge--green', ON_HOLD: 'badge--amber', COMPLETED: 'badge--blue', CANCELLED: 'badge--red',
      TODO: 'badge--gray', IN_PROGRESS: 'badge--blue', IN_REVIEW: 'badge--amber', DONE: 'badge--green',
      SUBMITTED: 'badge--amber', APPROVED: 'badge--green', REJECTED: 'badge--red'
    };
    return map[status] || '';
  }

  getPriorityIcon(priority: string): string {
    const map: Record<string, string> = {
      LOW: 'fa-arrow-down', MEDIUM: 'fa-minus', HIGH: 'fa-arrow-up', CRITICAL: 'fa-fire'
    };
    return map[priority] || 'fa-minus';
  }

  getPriorityClass(priority: string): string {
    const map: Record<string, string> = {
      LOW: 'priority--low', MEDIUM: 'priority--medium', HIGH: 'priority--high', CRITICAL: 'priority--critical'
    };
    return map[priority] || '';
  }

  getRiskIcon(type: string): string {
    const map: Record<string, string> = {
      DELAY_RISK: 'fa-clock', BOTTLENECK: 'fa-traffic-light', INACTIVITY: 'fa-moon', SCOPE_CREEP: 'fa-expand'
    };
    return map[type] || 'fa-triangle-exclamation';
  }

  getRiskLabel(type: string): string {
    const map: Record<string, string> = {
      DELAY_RISK: 'Risque de retard', BOTTLENECK: 'Goulot d\'étranglement',
      INACTIVITY: 'Inactivité', SCOPE_CREEP: 'Dérive du périmètre'
    };
    return map[type] || type;
  }

  getSeverityClass(severity: string): string {
    const map: Record<string, string> = {
      LOW: 'severity--low', MEDIUM: 'severity--medium', HIGH: 'severity--high', CRITICAL: 'severity--critical'
    };
    return map[severity] || '';
  }

  goToRisks() {
    this.router.navigate(['/app/projects', this.projectId, 'risks']);
  }
}