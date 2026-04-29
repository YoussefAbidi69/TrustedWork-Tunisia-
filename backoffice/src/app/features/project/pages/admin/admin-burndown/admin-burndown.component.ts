import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProjectApiService } from '../../../services/project-api.service';
import { Project, BurndownChart, BurndownPoint, BurndownStatus } from '../../../models/project.models';

@Component({
  selector: 'app-admin-burndown',
  templateUrl: './admin-burndown.component.html',
  styleUrls: ['./admin-burndown.component.css']
})
export class AdminBurndownComponent implements OnInit {

  projects: Project[] = [];
  selectedId: number | null = null;
  chart: BurndownChart | null = null;
  loadingProjects = true;
  loadingChart = false;

  // SVG chart viewport constants
  readonly VW = 800;   // viewBox total width
  readonly VH = 300;   // viewBox total height
  readonly OX = 55;    // chart origin X (left pad for Y labels)
  readonly OY = 15;    // chart origin Y (top pad)
  readonly CW = 720;   // chart draw width  (OX → OX+CW)
  readonly CH = 235;   // chart draw height (OY → OY+CH)

  constructor(private api: ProjectApiService, private router: Router) {}

  ngOnInit(): void { this.loadProjects(); }

  loadProjects(): void {
    this.loadingProjects = true;
    this.api.getAllProjects().subscribe({
      next: projects => {
        this.projects = projects;
        this.loadingProjects = false;
        const first = projects.find(p => p.status === 'ACTIVE') || projects[0];
        if (first) this.selectProject(first.id);
      },
      error: () => this.loadingProjects = false
    });
  }

  selectProject(id: number): void {
    if (this.selectedId === id && this.chart) return;
    this.selectedId = id;
    this.chart = null;
    this.loadingChart = true;
    this.api.getBurndownChart(id).subscribe({
      next: data => { this.chart = data; this.loadingChart = false; },
      error: () => this.loadingChart = false
    });
  }

  refresh(): void {
    if (this.selectedId) {
      this.chart = null;
      this.loadingChart = true;
      this.api.getBurndownChart(this.selectedId).subscribe({
        next: data => { this.chart = data; this.loadingChart = false; },
        error: () => this.loadingChart = false
      });
    }
  }

  get selectedProject(): Project | undefined {
    return this.projects.find(p => p.id === this.selectedId);
  }

  goBack(): void { this.router.navigate(['/admin/projects']); }

  // ─── Date Helpers ────────────────────────────────────────────────────────

  /** Jackson may serialize LocalDate as "2024-01-15" or [2024,1,15] */
  private toMs(d: string | number[] | null): number {
    if (!d) return Date.now();
    if (Array.isArray(d)) return new Date(d[0] as number, (d[1] as number) - 1, d[2] as number).getTime();
    return new Date(d as string).getTime();
  }

  formatDate(d: string | number[] | null): string {
    if (!d) return '—';
    const dt = new Date(this.toMs(d));
    return dt.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatShort(d: string | number[] | null): string {
    if (!d) return '';
    const dt = new Date(this.toMs(d));
    return `${dt.getDate()}/${dt.getMonth() + 1}`;
  }

  // ─── SVG Coordinate Helpers ──────────────────────────────────────────────

  x(date: string | number[]): number {
    if (!this.chart) return this.OX;
    const start = this.toMs(this.chart.startDate);
    const end   = this.toMs(this.chart.endDate);
    const cur   = this.toMs(date);
    if (end === start) return this.OX;
    const ratio = Math.min(1, Math.max(0, (cur - start) / (end - start)));
    return this.OX + ratio * this.CW;
  }

  y(tasks: number): number {
    if (!this.chart || this.chart.totalTaches === 0) return this.OY + this.CH;
    const ratio = Math.min(1, Math.max(0, tasks / this.chart.totalTaches));
    return this.OY + this.CH - ratio * this.CH;
  }

  get todayX(): number {
    if (!this.chart) return -1;
    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    return this.x(todayStr);
  }

  get idealPolyline(): string {
    if (!this.chart?.courbeIdeale.length) return '';
    return this.chart.courbeIdeale
      .map((p: BurndownPoint) => `${this.x(p.date).toFixed(1)},${this.y(p.tachesRestantes).toFixed(1)}`)
      .join(' ');
  }

  get realPolyline(): string {
    if (!this.chart?.courbeReelle.length) return '';
    return this.chart.courbeReelle
      .map((p: BurndownPoint) => `${this.x(p.date).toFixed(1)},${this.y(p.tachesRestantes).toFixed(1)}`)
      .join(' ');
  }

  /** Closed polygon for the fill below the real curve */
  get realAreaPolygon(): string {
    if (!this.chart?.courbeReelle.length) return '';
    const pts = this.chart.courbeReelle
      .map((p: BurndownPoint) => `${this.x(p.date).toFixed(1)},${this.y(p.tachesRestantes).toFixed(1)}`)
      .join(' ');
    const first = this.chart.courbeReelle[0];
    const last  = this.chart.courbeReelle[this.chart.courbeReelle.length - 1];
    const bottom = (this.OY + this.CH).toFixed(1);
    return `${this.x(first.date).toFixed(1)},${bottom} ${pts} ${this.x(last.date).toFixed(1)},${bottom}`;
  }

  get yLabels(): { y: number; value: number }[] {
    if (!this.chart) return [];
    const steps = 5;
    return Array.from({ length: steps + 1 }, (_, i) => ({
      y: this.OY + (this.CH * i / steps),
      value: Math.round(this.chart!.totalTaches * (1 - i / steps))
    }));
  }

  get xLabels(): { x: number; label: string }[] {
    if (!this.chart?.courbeIdeale.length) return [];
    const pts = this.chart.courbeIdeale;
    const maxLabels = 7;
    const step = Math.max(1, Math.floor(pts.length / maxLabels));
    const result: { x: number; label: string }[] = [];
    for (let i = 0; i < pts.length; i++) {
      if (i % step === 0 || i === pts.length - 1) {
        result.push({ x: this.x(pts[i].date), label: this.formatShort(pts[i].date) });
      }
    }
    return result;
  }

  get gridLines(): number[] {
    const steps = 5;
    return Array.from({ length: steps + 1 }, (_, i) => this.OY + (this.CH * i / steps));
  }

  // ─── Status & Badge Helpers ───────────────────────────────────────────────

  statusColor(s: BurndownStatus | string): string {
    return ({
      EN_AVANCE:       '#10b981',
      DANS_LES_DELAIS: '#3b82f6',
      EN_RETARD:       '#f59e0b',
      CRITIQUE:        '#ef4444'
    } as any)[s] || '#6366f1';
  }

  statusLabel(s: BurndownStatus | string): string {
    return ({
      EN_AVANCE:       '🟢 En avance',
      DANS_LES_DELAIS: '🔵 Dans les délais',
      EN_RETARD:       '🟠 En retard',
      CRITIQUE:        '🔴 Critique'
    } as any)[s] || s;
  }

  statusBadge(s: BurndownStatus | string): string {
    return ({
      EN_AVANCE:       'badge-success',
      DANS_LES_DELAIS: 'badge-info',
      EN_RETARD:       'badge-warning',
      CRITIQUE:        'badge-critical'
    } as any)[s] || '';
  }

  projectStatusBadge(s: string): string {
    return ({ ACTIVE: 'badge-success', ON_HOLD: 'badge-warning', COMPLETED: 'badge-info', CANCELLED: 'badge-danger' } as any)[s] || '';
  }

  velociteLabel(v: number): string {
    if (v === 0) return '—';
    return v.toFixed(2) + ' tâches/j';
  }

  retardLabel(r: number): string {
    if (r === 0) return 'Aucun';
    if (r < 0)   return `${Math.abs(r)} j d'avance`;
    return `${r} j de retard`;
  }

  retardClass(r: number): string {
    if (r <= 0)  return 'metric-ok';
    if (r <= 5)  return 'metric-warn';
    return 'metric-danger';
  }
}
