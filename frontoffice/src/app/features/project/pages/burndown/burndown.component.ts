import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectApiService } from '../../services/project-api.service';
import { Project, BurndownChart, BurndownPoint, BurndownStatus } from '../../models/project.models';

@Component({
  selector: 'app-burndown',
  templateUrl: './burndown.component.html',
  styleUrls: ['./burndown.component.css']
})
export class BurndownComponent implements OnInit {

  projectId!: number;
  project!: Project;
  chart: BurndownChart | null = null;
  loading = true;
  loadingChart = false;
  error = '';

  // SVG viewport constants
  readonly VW = 800;
  readonly VH = 300;
  readonly OX = 55;
  readonly OY = 15;
  readonly CW = 720;
  readonly CH = 235;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: ProjectApiService
  ) {}

  ngOnInit(): void {
    this.projectId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadData();
  }

  private loadData(): void {
    this.loading = true;
    this.api.getProjectById(this.projectId).subscribe({
      next: (p) => {
        this.project = p;
        this.loadChart();
      },
      error: () => {
        this.error = 'Impossible de charger le projet.';
        this.loading = false;
      }
    });
  }

  private loadChart(): void {
    this.loadingChart = true;
    this.api.getBurndownChart(this.projectId).subscribe({
      next: (data) => {
        this.chart = data;
        this.loading = false;
        this.loadingChart = false;
      },
      error: () => {
        this.loading = false;
        this.loadingChart = false;
      }
    });
  }

  refresh(): void {
    this.chart = null;
    this.loadingChart = true;
    this.api.getBurndownChart(this.projectId).subscribe({
      next: (data) => { this.chart = data; this.loadingChart = false; },
      error: () => { this.loadingChart = false; }
    });
  }

  goBack(): void {
    this.router.navigate(['/app/projects', this.projectId]);
  }

  // ─── Date Helpers ────────────────────────────────────────────────

  private toMs(d: string | number[] | null): number {
    if (!d) return Date.now();
    if (Array.isArray(d)) return new Date(d[0] as number, (d[1] as number) - 1, d[2] as number).getTime();
    return new Date(d as string).getTime();
  }

  formatDate(d: string | number[] | null): string {
    if (!d) return '—';
    return new Date(this.toMs(d)).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatShort(d: string | number[] | null): string {
    if (!d) return '';
    const dt = new Date(this.toMs(d));
    return `${dt.getDate()}/${dt.getMonth() + 1}`;
  }

  // ─── SVG Coordinate Helpers ──────────────────────────────────────

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

  // ─── Status Helpers ───────────────────────────────────────────────

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
      EN_AVANCE:       'En avance',
      DANS_LES_DELAIS: 'Dans les délais',
      EN_RETARD:       'En retard',
      CRITIQUE:        'Critique'
    } as any)[s] || s;
  }

  statusIcon(s: BurndownStatus | string): string {
    return ({
      EN_AVANCE:       'fa-circle-check',
      DANS_LES_DELAIS: 'fa-circle-info',
      EN_RETARD:       'fa-triangle-exclamation',
      CRITIQUE:        'fa-circle-xmark'
    } as any)[s] || 'fa-circle';
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

  get completionPct(): number {
    if (!this.chart || this.chart.totalTaches === 0) return 0;
    return Math.round((this.chart.tachesCompletees / this.chart.totalTaches) * 100);
  }
}
