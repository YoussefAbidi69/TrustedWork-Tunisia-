import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Router } from '@angular/router';
import { ProjectApiService } from '../../../services/project-api.service';
import { Project, ProgressReport } from '../../../models/project.models';

@Component({
  selector: 'app-admin-reports',
  templateUrl: './admin-reports.component.html',
  styleUrls: ['./admin-reports.component.css']
})
export class AdminReportsComponent implements OnInit {

  projects: Project[] = [];
  reports: { project: Project; report: ProgressReport | null }[] = [];
  loading = true;

  constructor(private api: ProjectApiService, private router: Router) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading = true;
    this.api.getAllProjects().subscribe({
      next: projects => {
        this.projects = projects;
        if (!projects.length) { this.reports = []; this.loading = false; return; }
        forkJoin(projects.map(p => this.api.generateReport(p.id))).subscribe({
          next: reps => {
            this.reports = projects.map((p, i) => ({ project: p, report: reps[i] || null }));
            this.loading = false;
          },
          error: () => {
            this.reports = projects.map(p => ({ project: p, report: null }));
            this.loading = false;
          }
        });
      },
      error: () => this.loading = false
    });
  }

  exportPdf(projectId: number, title: string): void {
    this.api.exportPdf(projectId).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `rapport_${title.replace(/\s+/g, '_')}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  exportCsv(projectId: number, title: string): void {
    this.api.exportCsv(projectId).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `taches_${title.replace(/\s+/g, '_')}.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    });
  }

  goBack(): void { this.router.navigate(['/admin/projects']); }

  statusBadge(s: string): string {
    return ({ ACTIVE: 'badge-success', ON_HOLD: 'badge-warning', COMPLETED: 'badge-info', CANCELLED: 'badge-danger' } as any)[s] || 'badge-muted';
  }
  statusLabel(s: string): string {
    return ({ ACTIVE: 'Active', ON_HOLD: 'On Hold', COMPLETED: 'Completed', CANCELLED: 'Cancelled' } as any)[s] || s;
  }
}
