import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectApiService } from '../../services/project-api.service';
import { Project, Task, TaskStatus, TaskPriority } from '../../models/project.models';

interface GanttTask {
  task: Task;
  startX: number;      // position en % depuis le début du projet
  widthPct: number;     // largeur en % de la timeline
  daysTotal: number;    // durée en jours
  isOverdue: boolean;
  progress: number;     // 0 ou 100 selon le statut
}

interface GanttMonth {
  label: string;
  startPct: number;
  widthPct: number;
}

@Component({
  selector: 'app-gantt-chart',
  templateUrl: './gantt-chart.component.html',
  styleUrls: ['./gantt-chart.component.css']
})
export class GanttChartComponent implements OnInit {

  projectId!: number;
  project!: Project;
  tasks: Task[] = [];
  ganttTasks: GanttTask[] = [];
  ganttMonths: GanttMonth[] = [];
  loading = true;
  error = '';

  // Timeline bounds
  timelineStart!: Date;
  timelineEnd!: Date;
  timelineDays = 0;
  todayPct = 0;

  // Filtre
  statusFilter: 'ALL' | TaskStatus = 'ALL';
  priorityFilter: 'ALL' | TaskPriority = 'ALL';

  // Zoom
  zoomLevel: 'compact' | 'normal' | 'wide' = 'normal';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectApi: ProjectApiService
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadData();
  }

  private loadData(): void {
    this.loading = true;

    this.projectApi.getProjectById(this.projectId).subscribe({
      next: (p) => {
        this.project = p;
        this.projectApi.getTasksByProjectId(this.projectId).subscribe({
          next: (tasks) => {
            this.tasks = tasks;
            this.buildGantt();
            this.loading = false;
          },
          error: () => {
            this.error = 'Impossible de charger les tâches.';
            this.loading = false;
          }
        });
      },
      error: () => {
        this.error = 'Projet introuvable.';
        this.loading = false;
      }
    });
  }

  // ══════════════════════════════════════
  // CONSTRUCTION DU GANTT
  // ══════════════════════════════════════

  private buildGantt(): void {
    // Déterminer les bornes de la timeline
    this.timelineStart = new Date(this.project.startDate);
    this.timelineEnd = new Date(this.project.endDate);

    // Étendre la timeline si des tâches débordent
    this.tasks.forEach(t => {
      const taskStart = new Date(t.createdAt);
      const taskEnd = t.deadline ? new Date(t.deadline) : this.addDays(taskStart, t.estimatedHours ? Math.ceil(t.estimatedHours / 8) : 7);

      if (taskStart < this.timelineStart) this.timelineStart = taskStart;
      if (taskEnd > this.timelineEnd) this.timelineEnd = taskEnd;
    });

    // Ajouter une marge de 3 jours de chaque côté
    this.timelineStart = this.addDays(this.timelineStart, -3);
    this.timelineEnd = this.addDays(this.timelineEnd, 7);

    this.timelineDays = this.daysBetween(this.timelineStart, this.timelineEnd);
    if (this.timelineDays <= 0) this.timelineDays = 30;

    // Position du curseur "Aujourd'hui"
    const today = new Date();
    this.todayPct = this.dateToPct(today);

    // Construire les barres de tâches
    this.ganttTasks = this.tasks.map(t => this.buildGanttTask(t));

    // Construire les mois pour le header
    this.buildMonths();
  }

  private buildGanttTask(task: Task): GanttTask {
    const taskStart = new Date(task.createdAt);
    let taskEnd: Date;

    if (task.deadline) {
      taskEnd = new Date(task.deadline);
    } else if (task.estimatedHours) {
      taskEnd = this.addDays(taskStart, Math.ceil(task.estimatedHours / 8));
    } else {
      taskEnd = this.addDays(taskStart, 7);
    }

    // S'assurer que la durée est d'au moins 1 jour
    if (taskEnd <= taskStart) {
      taskEnd = this.addDays(taskStart, 1);
    }

    const startX = this.dateToPct(taskStart);
    const endX = this.dateToPct(taskEnd);
    const widthPct = Math.max(endX - startX, 1.5); // minimum 1.5% pour être visible
    const daysTotal = this.daysBetween(taskStart, taskEnd);

    const now = new Date();
    const isOverdue = task.deadline ? (new Date(task.deadline) < now && task.status !== 'DONE') : false;
    const progress = task.status === 'DONE' ? 100 : task.status === 'IN_REVIEW' ? 75 : task.status === 'IN_PROGRESS' ? 40 : 0;

    return { task, startX, widthPct, daysTotal, isOverdue, progress };
  }

  private buildMonths(): void {
    this.ganttMonths = [];
    const current = new Date(this.timelineStart.getFullYear(), this.timelineStart.getMonth(), 1);

    while (current <= this.timelineEnd) {
      const monthStart = new Date(Math.max(current.getTime(), this.timelineStart.getTime()));
      const nextMonth = new Date(current.getFullYear(), current.getMonth() + 1, 1);
      const monthEnd = new Date(Math.min(nextMonth.getTime(), this.timelineEnd.getTime()));

      const startPct = this.dateToPct(monthStart);
      const endPct = this.dateToPct(monthEnd);

      this.ganttMonths.push({
        label: current.toLocaleDateString('fr-TN', { month: 'short', year: 'numeric' }),
        startPct,
        widthPct: endPct - startPct
      });

      current.setMonth(current.getMonth() + 1);
    }
  }

  // ══════════════════════════════════════
  // FILTRES
  // ══════════════════════════════════════

  get filteredGanttTasks(): GanttTask[] {
    return this.ganttTasks.filter(gt => {
      if (this.statusFilter !== 'ALL' && gt.task.status !== this.statusFilter) return false;
      if (this.priorityFilter !== 'ALL' && gt.task.priority !== this.priorityFilter) return false;
      return true;
    });
  }

  setStatusFilter(f: 'ALL' | TaskStatus): void { this.statusFilter = f; }
  setPriorityFilter(f: 'ALL' | TaskPriority): void { this.priorityFilter = f; }

  // ══════════════════════════════════════
  // HELPERS DATE
  // ══════════════════════════════════════

  private dateToPct(date: Date): number {
    const days = this.daysBetween(this.timelineStart, date);
    return Math.max(0, Math.min(100, (days / this.timelineDays) * 100));
  }

  private daysBetween(a: Date, b: Date): number {
    return Math.ceil((b.getTime() - a.getTime()) / (1000 * 60 * 60 * 24));
  }

  private addDays(date: Date, days: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  }

  // ══════════════════════════════════════
  // HELPERS DISPLAY
  // ══════════════════════════════════════

  goBack(): void {
    this.router.navigate(['/app/projects', this.projectId]);
  }

  getStatusLabel(s: string): string {
    return { TODO: 'À faire', IN_PROGRESS: 'En cours', IN_REVIEW: 'En review', DONE: 'Terminée' }[s] || s;
  }

  getStatusClass(s: string): string {
    return { TODO: 'gs--todo', IN_PROGRESS: 'gs--progress', IN_REVIEW: 'gs--review', DONE: 'gs--done' }[s] || '';
  }

  getBarColor(gt: GanttTask): string {
    if (gt.isOverdue) return '#ef4444';
    return { TODO: '#94a3b8', IN_PROGRESS: '#3b82f6', IN_REVIEW: '#f59e0b', DONE: '#22c55e' }[gt.task.status] || '#94a3b8';
  }

  getBarBg(gt: GanttTask): string {
    if (gt.isOverdue) return 'rgba(239,68,68,.15)';
    return { TODO: 'rgba(148,163,184,.15)', IN_PROGRESS: 'rgba(59,130,246,.12)', IN_REVIEW: 'rgba(245,158,11,.12)', DONE: 'rgba(34,197,94,.12)' }[gt.task.status] || 'rgba(148,163,184,.12)';
  }

  getPriorityIcon(p: string): string {
    return { LOW: 'fa-arrow-down', MEDIUM: 'fa-minus', HIGH: 'fa-arrow-up', CRITICAL: 'fa-fire' }[p] || 'fa-minus';
  }

  getPriorityClass(p: string): string {
    return { LOW: 'gp--low', MEDIUM: 'gp--med', HIGH: 'gp--high', CRITICAL: 'gp--crit' }[p] || '';
  }

  formatShortDate(date: string): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('fr-TN', { day: '2-digit', month: 'short' });
  }

  getZoomWidth(): string {
    return { compact: '100%', normal: '150%', wide: '250%' }[this.zoomLevel] || '150%';
  }
}