import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Router } from '@angular/router';
import { ProjectApiService } from '../../../services/project-api.service';
import { Project, DeliveryRiskSignal } from '../../../models/project.models';

@Component({
  selector: 'app-admin-risks',
  templateUrl: './admin-risks.component.html',
  styleUrls: ['./admin-risks.component.css']
})
export class AdminRisksComponent implements OnInit {

  projects: Project[] = [];
  allRisks: DeliveryRiskSignal[] = [];
  filtered: DeliveryRiskSignal[] = [];
  loading = true;
  analyzing = false;

  filterSeverity = 'ALL';
  filterType = 'ALL';
  severityList = ['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
  typeList = ['ALL', 'DELAY_RISK', 'BOTTLENECK', 'INACTIVITY', 'SCOPE_CREEP'];

  constructor(private api: ProjectApiService, private router: Router) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading = true;
    this.api.getAllProjects().subscribe({
      next: projects => {
        this.projects = projects;
        const active = projects.filter(p => p.status === 'ACTIVE');
        if (!active.length) { this.allRisks = []; this.filtered = []; this.loading = false; return; }
        forkJoin(active.map(p => this.api.getActiveRisks(p.id))).subscribe({
          next: arrays => {
            this.allRisks = arrays.flat();
            this.applyFilters();
            this.loading = false;
          },
          error: () => this.loading = false
        });
      },
      error: () => this.loading = false
    });
  }

  applyFilters(): void {
    this.filtered = this.allRisks.filter(r => {
      const ms = this.filterSeverity === 'ALL' || r.severity === this.filterSeverity;
      const mt = this.filterType === 'ALL' || r.riskType === this.filterType;
      return ms && mt;
    });
  }

  analyzeAll(): void {
    this.analyzing = true;
    const active = this.projects.filter(p => p.status === 'ACTIVE');
    if (!active.length) { this.analyzing = false; return; }
    forkJoin(active.map(p => this.api.analyzeRisks(p.id))).subscribe({
      next: arrays => {
        this.allRisks = arrays.flat();
        this.applyFilters();
        this.analyzing = false;
      },
      error: () => this.analyzing = false
    });
  }

  resolveRisk(id: number): void {
    this.api.resolveRisk(id).subscribe({
      next: () => {
        this.allRisks = this.allRisks.filter(r => r.id !== id);
        this.applyFilters();
      }
    });
  }

  goBack(): void { this.router.navigate(['/admin/projects']); }
  projectTitle(id: number): string { return this.projects.find(p => p.id === id)?.title || `Projet #${id}`; }

  riskIcon(t: string): string {
    return ({ DELAY_RISK: 'fa-clock', BOTTLENECK: 'fa-traffic-light', INACTIVITY: 'fa-moon', SCOPE_CREEP: 'fa-expand' } as any)[t] || 'fa-triangle-exclamation';
  }
  riskLabel(t: string): string {
    return ({ DELAY_RISK: 'Retard', BOTTLENECK: 'Goulot', INACTIVITY: 'Inactivité', SCOPE_CREEP: 'Scope Creep' } as any)[t] || t;
  }
  severityBadge(s: string): string {
    return ({ LOW: 'badge-success', MEDIUM: 'badge-warning', HIGH: 'badge-danger', CRITICAL: 'badge-critical' } as any)[s] || 'badge-muted';
  }
}
