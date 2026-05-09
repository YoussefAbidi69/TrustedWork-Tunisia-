import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectApiService } from '../../../services/project-api.service';
import {
  Project, Task, Deliverable, DeliveryRiskSignal, ProgressReport,
  TaskStatus, TaskPriority, CreateTaskDTO
} from '../../../models/project.models';

@Component({
  selector: 'app-admin-detail',
  templateUrl: './admin-detail.component.html',
  styleUrls: ['./admin-detail.component.css']
})
export class AdminDetailComponent implements OnInit {

  projectId!: number;
  project!: Project;
  tasks: Task[] = [];
  deliverables: Deliverable[] = [];
  risks: DeliveryRiskSignal[] = [];
  report: ProgressReport | null = null;
  loading = true;
  analyzing = false;

  // Modal ajout tâche
  showTaskModal = false;
  newTask: CreateTaskDTO = { title: '', description: '', priority: 'MEDIUM', assigneeId: 0, deadline: '', estimatedHours: 8 };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ProjectApiService
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll();
  }

  loadAll(): void {
    this.loading = true;
    forkJoin({
      project: this.api.getProjectByIdEnriched(this.projectId),
      tasks: this.api.getTasksByProjectId(this.projectId),
      deliverables: this.api.getDeliverablesByProjectId(this.projectId),
      risks: this.api.getActiveRisks(this.projectId),
      report: this.api.generateReport(this.projectId)
    }).subscribe({
      next: data => {
        this.project = data.project;
        this.tasks = data.tasks;
        this.deliverables = data.deliverables;
        this.risks = data.risks;
        this.report = data.report;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  // ── IA ──
  analyzeRisks(): void {
    this.analyzing = true;
    this.api.analyzeRisks(this.projectId).subscribe({
      next: risks => { this.risks = risks; this.analyzing = false; },
      error: () => this.analyzing = false
    });
  }

  resolveRisk(id: number): void {
    this.api.resolveRisk(id).subscribe({
      next: () => { this.risks = this.risks.filter(r => r.id !== id); }
    });
  }

  // ── Tasks ──
  openTaskModal(): void {
    this.newTask = { title: '', description: '', priority: 'MEDIUM', assigneeId: 0, deadline: '', estimatedHours: 8 };
    this.showTaskModal = true;
  }

  submitTask(): void {
    if (!this.newTask.title.trim()) return;
    this.api.createTask(this.projectId, this.newTask).subscribe({
      next: t => { this.tasks.push(t); this.showTaskModal = false; }
    });
  }

  deleteTask(id: number): void {
    if (!confirm('Supprimer cette tâche ?')) return;
    this.api.deleteTask(id).subscribe({
      next: () => { this.tasks = this.tasks.filter(t => t.id !== id); }
    });
  }

  // ── Exports ──
  exportPdf(): void {
    this.api.exportPdf(this.projectId).subscribe(blob => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `rapport_projet_${this.projectId}.pdf`;
      a.click();
    });
  }

  exportCsv(): void {
    this.api.exportCsv(this.projectId).subscribe(blob => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `taches_projet_${this.projectId}.csv`;
      a.click();
    });
  }

  goBack(): void { this.router.navigate(['/admin/projects/admin/list']); }

  // ── Computed ──
  get tasksDone(): number { return this.tasks.filter(t => t.status === 'DONE').length; }
  get pendingDeliverables(): number { return this.deliverables.filter(d => d.status === 'SUBMITTED').length; }
  get daysRemaining(): number {
    if (!this.project?.endDate) return 0;
    const diff = Math.ceil((new Date(this.project.endDate).getTime() - Date.now()) / 86400000);
    return Math.max(0, diff);
  }

  // ── Helpers ──
  statusBadge(s: string): string {
    return ({ ACTIVE: 'badge-success', ON_HOLD: 'badge-warning', COMPLETED: 'badge-info', CANCELLED: 'badge-danger' } as any)[s] || 'badge-muted';
  }
  taskStatusBadge(s: string): string {
    return ({ TODO: 'badge-muted', IN_PROGRESS: 'badge-info', IN_REVIEW: 'badge-warning', DONE: 'badge-success' } as any)[s] || 'badge-muted';
  }
  taskStatusLabel(s: string): string {
    return ({ TODO: 'To Do', IN_PROGRESS: 'In Progress', IN_REVIEW: 'In Review', DONE: 'Done' } as any)[s] || s;
  }
  priorityBadge(p: string): string {
    return ({ LOW: 'badge-muted', MEDIUM: 'badge-info', HIGH: 'badge-warning', CRITICAL: 'badge-danger' } as any)[p] || 'badge-muted';
  }
  delivStatusBadge(s: string): string {
    return ({ SUBMITTED: 'badge-warning', APPROVED: 'badge-success', REJECTED: 'badge-danger' } as any)[s] || 'badge-muted';
  }
  severityBadge(s: string): string {
    return ({ LOW: 'badge-success', MEDIUM: 'badge-warning', HIGH: 'badge-danger', CRITICAL: 'badge-critical' } as any)[s] || 'badge-muted';
  }
  riskIcon(t: string): string {
    return ({ DELAY_RISK: 'fa-clock', BOTTLENECK: 'fa-traffic-light', INACTIVITY: 'fa-moon', SCOPE_CREEP: 'fa-expand' } as any)[t] || 'fa-triangle-exclamation';
  }
  riskLabel(t: string): string {
    return ({ DELAY_RISK: 'Retard', BOTTLENECK: 'Goulot', INACTIVITY: 'Inactivité', SCOPE_CREEP: 'Scope Creep' } as any)[t] || t;
  }
}
