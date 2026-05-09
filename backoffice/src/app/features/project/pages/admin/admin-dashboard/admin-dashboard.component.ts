import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ProjectApiService } from '../../../services/project-api.service';
import { Project, DeliveryRiskSignal } from '../../../models/project.models';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {

  projects: Project[] = [];
  recentProjects: Project[] = [];
  allRisks: DeliveryRiskSignal[] = [];
  loading = true;

  stats = {
    total: 0, active: 0, completed: 0, onHold: 0, cancelled: 0,
    avgCompletion: 0, totalBudget: 0, totalRisks: 0, criticalRisks: 0
  };

  constructor(private api: ProjectApiService, private router: Router) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.api.getAllProjects().subscribe({
      next: projects => {
        this.projects = projects;
        this.computeStats(projects);
        this.recentProjects = [...projects]
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .slice(0, 6);

        // Charger les risques de tous les projets ACTIVE
        const active = projects.filter(p => p.status === 'ACTIVE');
        if (active.length) {
          forkJoin(active.map(p => this.api.getActiveRisks(p.id))).subscribe({
            next: arrays => {
              this.allRisks = arrays.flat();
              this.stats.totalRisks = this.allRisks.length;
              this.stats.criticalRisks = this.allRisks.filter(
                r => r.severity === 'CRITICAL' || r.severity === 'HIGH'
              ).length;
              this.loading = false;
            },
            error: () => this.loading = false
          });
        } else {
          this.loading = false;
        }
      },
      error: () => this.loading = false
    });
  }

  private computeStats(p: Project[]): void {
    this.stats.total = p.length;
    this.stats.active = p.filter(x => x.status === 'ACTIVE').length;
    this.stats.completed = p.filter(x => x.status === 'COMPLETED').length;
    this.stats.onHold = p.filter(x => x.status === 'ON_HOLD').length;
    this.stats.cancelled = p.filter(x => x.status === 'CANCELLED').length;
    this.stats.totalBudget = p.reduce((s, x) => s + (x.budget || 0), 0);
    this.stats.avgCompletion = p.length
      ? Math.round(p.reduce((s, x) => s + x.completionRate, 0) / p.length) : 0;
  }

  nav(path: string): void { this.router.navigate(['/admin/projects/admin', path]); }
  goProject(id: number): void { this.router.navigate(['/admin/projects/admin/detail', id]); }

  // ── Helpers ──
  statusLabel(s: string): string {
    return ({ ACTIVE: 'Active', ON_HOLD: 'On Hold', COMPLETED: 'Completed', CANCELLED: 'Cancelled' } as any)[s] || s;
  }
  statusBadge(s: string): string {
    return ({ ACTIVE: 'badge-success', ON_HOLD: 'badge-warning', COMPLETED: 'badge-info', CANCELLED: 'badge-danger' } as any)[s] || 'badge-muted';
  }
  riskIcon(t: string): string {
    return ({ DELAY_RISK: 'fa-clock', BOTTLENECK: 'fa-traffic-light', INACTIVITY: 'fa-moon', SCOPE_CREEP: 'fa-expand' } as any)[t] || 'fa-triangle-exclamation';
  }
  severityBadge(s: string): string {
    return ({ LOW: 'badge-success', MEDIUM: 'badge-warning', HIGH: 'badge-danger', CRITICAL: 'badge-critical' } as any)[s] || 'badge-muted';
  }
  pct(count: number): number {
    return this.stats.total ? Math.round(count / this.stats.total * 100) : 0;
  }
}
