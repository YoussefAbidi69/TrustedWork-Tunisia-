import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProjectApiService } from '../../../services/project-api.service';
import { Project, ProjectStatus, CreateProjectDTO } from '../../../models/project.models';

@Component({
  selector: 'app-admin-projects',
  templateUrl: './admin-projects.component.html',
  styleUrls: ['./admin-projects.component.css']
})
export class AdminProjectsComponent implements OnInit {

  projects: Project[] = [];
  filtered: Project[] = [];
  loading = true;

  searchQuery = '';
  selectedStatus = 'ALL';
  statuses = ['ALL', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];

  // Modal create
  showCreate = false;
  form: CreateProjectDTO = this.emptyForm();

  // Modal status
  showStatusModal = false;
  targetProject: Project | null = null;
  newStatus: ProjectStatus = 'ACTIVE';

  // Modal delete
  showDelete = false;
  deleteTarget: Project | null = null;

  constructor(private api: ProjectApiService, private router: Router) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading = true;
    this.api.getAllProjects().subscribe({
      next: data => { this.projects = data; this.applyFilters(); this.loading = false; },
      error: () => this.loading = false
    });
  }

  applyFilters(): void {
    const q = this.searchQuery.trim().toLowerCase();
    this.filtered = this.projects.filter(p => {
      const matchSearch = !q || p.title.toLowerCase().includes(q) || p.description?.toLowerCase().includes(q) || p.id.toString().includes(q);
      const matchStatus = this.selectedStatus === 'ALL' || p.status === this.selectedStatus;
      return matchSearch && matchStatus;
    });
  }

  onStatusFilter(s: string): void { this.selectedStatus = s; this.applyFilters(); }

  // ── Create ──
  emptyForm(): CreateProjectDTO {
    return { title: '', description: '', contractId: 0, clientId: 0, freelancerId: 0, startDate: '', endDate: '', budget: 0 };
  }
  openCreate(): void { this.form = this.emptyForm(); this.showCreate = true; }
  submitCreate(): void {
    if (!this.form.title.trim()) return;
    this.api.createProject(this.form).subscribe({
      next: p => { this.projects.unshift(p); this.applyFilters(); this.showCreate = false; }
    });
  }

  // ── Status change ──
  openStatus(p: Project): void { this.targetProject = p; this.newStatus = p.status; this.showStatusModal = true; }
  submitStatus(): void {
    if (!this.targetProject) return;
    this.api.updateProjectStatus(this.targetProject.id, this.newStatus).subscribe({
      next: updated => {
        const i = this.projects.findIndex(x => x.id === updated.id);
        if (i !== -1) this.projects[i] = updated;
        this.applyFilters(); this.showStatusModal = false;
      }
    });
  }

  // ── Delete ──
  openDelete(p: Project): void { this.deleteTarget = p; this.showDelete = true; }
  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.api.deleteProject(this.deleteTarget.id).subscribe({
      next: () => {
        this.projects = this.projects.filter(x => x.id !== this.deleteTarget!.id);
        this.applyFilters(); this.showDelete = false;
      }
    });
  }

  goDetail(id: number): void { this.router.navigate(['/admin/projects/admin/detail', id]); }
  goBack(): void { this.router.navigate(['/admin/projects']); }

  statusLabel(s: string): string {
    return ({ ACTIVE: 'Active', ON_HOLD: 'On Hold', COMPLETED: 'Completed', CANCELLED: 'Cancelled' } as any)[s] || s;
  }
  statusBadge(s: string): string {
    return ({ ACTIVE: 'badge-success', ON_HOLD: 'badge-warning', COMPLETED: 'badge-info', CANCELLED: 'badge-danger' } as any)[s] || 'badge-muted';
  }
}
