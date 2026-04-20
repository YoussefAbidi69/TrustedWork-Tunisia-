import { Component, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { ProjectApiService } from '../../../services/project-api.service';
import { Project, MLPrediction } from '../../../models/project.models';

export interface ProjectMLEntry {
  project: Project;
  prediction: MLPrediction | null;
  loading: boolean;
  error: boolean;
}

@Component({
  selector: 'app-admin-ml-prediction',
  templateUrl: './admin-ml-prediction.component.html',
  styleUrls: ['./admin-ml-prediction.component.css']
})
export class AdminMlPredictionComponent implements OnInit {

  entries: ProjectMLEntry[] = [];
  filtered: ProjectMLEntry[] = [];
  loading = true;
  analyzing = false;

  filterVerdict = 'ALL';
  filterStatus  = 'ALL';
  verdictList   = ['ALL', 'AT_RISK', 'ON_TIME'];
  statusList    = ['ALL', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];

  constructor(private api: ProjectApiService, private router: Router) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading = true;
    this.api.getAllProjects().subscribe({
      next: projects => {
        this.entries = projects.map(p => ({ project: p, prediction: null, loading: true, error: false }));
        this.applyFilters();
        this.loading = false;

        forkJoin(
          projects.map(p =>
            this.api.predictDeliveryRisk(p.id).pipe(catchError(() => of(null)))
          )
        ).subscribe(preds => {
          preds.forEach((pred, i) => {
            this.entries[i].prediction = pred;
            this.entries[i].loading    = false;
            this.entries[i].error      = pred === null;
          });
          this.applyFilters();
        });
      },
      error: () => this.loading = false
    });
  }

  applyFilters(): void {
    this.filtered = this.entries.filter(e => {
      const ms = this.filterStatus === 'ALL' || e.project.status === this.filterStatus;
      const mv = this.filterVerdict === 'ALL' ||
        (this.filterVerdict === 'AT_RISK'  &&  e.prediction?.willBeLate) ||
        (this.filterVerdict === 'ON_TIME'  && !e.prediction?.willBeLate && e.prediction !== null);
      return ms && mv;
    });
  }

  refreshOne(entry: ProjectMLEntry): void {
    entry.loading = true;
    entry.error   = false;
    this.api.predictDeliveryRisk(entry.project.id).pipe(catchError(() => of(null))).subscribe(pred => {
      entry.prediction = pred;
      entry.loading    = false;
      entry.error      = pred === null;
      this.applyFilters();
    });
  }

  goBack(): void { this.router.navigate(['/admin/projects']); }

  get totalAtRisk(): number  { return this.entries.filter(e => e.prediction?.willBeLate).length; }
  get totalOnTime(): number  { return this.entries.filter(e => e.prediction && !e.prediction.willBeLate).length; }
  get totalLoaded(): number  { return this.entries.filter(e => !e.loading).length; }

  getProbabilityPercent(pred: MLPrediction): number {
    return Math.round(pred.probabilityLate * 100);
  }

  getProbabilityColor(pred: MLPrediction): string {
    const p = this.getProbabilityPercent(pred);
    if (p >= 85) return '#ef4444';
    if (p >= 70) return '#f97316';
    if (p >= 50) return '#f59e0b';
    return '#10b981';
  }

  getTimeVerdict(elapsed: number, done: number): string {
    const gap = elapsed - done;
    if (gap <= 0)    return '✅ Avance dans les délais';
    if (gap < 0.20)  return '⚠️ Légèrement en retard';
    if (gap < 0.40)  return '🔴 Retard important';
    return '🚨 Retard critique';
  }

  getPerfVerdict(score: number): string {
    if (score >= 0.80) return '✅ Fiabilité excellente';
    if (score >= 0.60) return '⚠️ Fiabilité modérée';
    if (score >= 0.40) return '🔴 Fiabilité faible';
    return '🚨 Historique insuffisant';
  }

  getScopeVerdict(ratio: number): string {
    if (ratio <= 0.10) return '✅ Périmètre stable';
    if (ratio <= 0.30) return '⚠️ Légère dérive';
    if (ratio <= 0.60) return '🔴 Dérive notable';
    return '🚨 Dérive critique';
  }

  getBottleneckVerdict(days: number): string {
    if (days <= 1) return '✅ Aucun blocage';
    if (days <= 3) return '⚠️ Blocage ' + Math.round(days) + 'j';
    if (days <= 7) return '🔴 Blocage prolongé';
    return '🚨 Blocage critique';
  }

  getFeatStatus(val: number, val2: number | null, type: string): string {
    let bad = false;
    if (type === 'time')       bad = (val - (val2 ?? 0)) > 0.20;
    if (type === 'perf')       bad = val < 0.60;
    if (type === 'scope')      bad = val > 0.30;
    if (type === 'bottleneck') bad = val > 3;
    return bad ? 'feat-item--warn' : 'feat-item--ok';
  }

  getDotClass(val: number, val2: number | null, type: string): string {
    let level = 0;
    if (type === 'time') {
      const gap = val - (val2 ?? 0);
      level = gap <= 0 ? 0 : gap < 0.20 ? 1 : gap < 0.40 ? 2 : 3;
    }
    if (type === 'perf')       level = val >= 0.80 ? 0 : val >= 0.60 ? 1 : val >= 0.40 ? 2 : 3;
    if (type === 'scope')      level = val <= 0.10 ? 0 : val <= 0.30 ? 1 : val <= 0.60 ? 2 : 3;
    if (type === 'bottleneck') level = val <= 1 ? 0 : val <= 3 ? 1 : val <= 7 ? 2 : 3;
    return ['dot--ok', 'dot--warn', 'dot--bad', 'dot--critical'][level];
  }

  statusLabel(s: string): string {
    return ({ ACTIVE: 'Actif', ON_HOLD: 'En pause', COMPLETED: 'Terminé', CANCELLED: 'Annulé' } as any)[s] || s;
  }
  statusBadge(s: string): string {
    return ({ ACTIVE: 'badge-success', ON_HOLD: 'badge-warning', COMPLETED: 'badge-info', CANCELLED: 'badge-danger' } as any)[s] || '';
  }
}
