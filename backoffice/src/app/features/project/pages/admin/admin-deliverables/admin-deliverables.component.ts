import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Router } from '@angular/router';
import { ProjectApiService } from '../../../services/project-api.service';
import { Project, Deliverable, DeliverableStatus } from '../../../models/project.models';

@Component({
  selector: 'app-admin-deliverables',
  templateUrl: './admin-deliverables.component.html',
  styleUrls: ['./admin-deliverables.component.css']
})
export class AdminDeliverablesComponent implements OnInit {

  projects: Project[] = [];
  allDeliverables: Deliverable[] = [];
  filtered: Deliverable[] = [];
  loading = true;

  searchQuery = '';
  filterStatus = 'ALL';
  statusList = ['ALL', 'SUBMITTED', 'APPROVED', 'REJECTED'];

  // Review modal
  showReview = false;
  reviewTarget: Deliverable | null = null;
  reviewStatus: DeliverableStatus = 'APPROVED';
  reviewComment = '';

  // Delete modal
  showDelete = false;
  deleteTarget: Deliverable | null = null;

  constructor(private api: ProjectApiService, private router: Router) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading = true;
    this.api.getAllProjects().subscribe({
      next: projects => {
        this.projects = projects;
        if (!projects.length) { this.loading = false; return; }
        forkJoin(projects.map(p => this.api.getDeliverablesByProjectId(p.id))).subscribe({
          next: arrays => {
            this.allDeliverables = arrays.flat();
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
    const q = this.searchQuery.trim().toLowerCase();
    this.filtered = this.allDeliverables.filter(d => {
      const matchSearch = !q || d.title.toLowerCase().includes(q);
      const matchStatus = this.filterStatus === 'ALL' || d.status === this.filterStatus;
      return matchSearch && matchStatus;
    });
  }

  openReview(d: Deliverable): void {
    this.reviewTarget = d;
    this.reviewStatus = 'APPROVED';
    this.reviewComment = '';
    this.showReview = true;
  }

  submitReview(): void {
    if (!this.reviewTarget) return;
    this.api.reviewDeliverable(this.reviewTarget.id, this.reviewStatus, this.reviewComment).subscribe({
      next: updated => {
        const i = this.allDeliverables.findIndex(x => x.id === updated.id);
        if (i !== -1) this.allDeliverables[i] = updated;
        this.applyFilters();
        this.showReview = false;
      }
    });
  }

  openDelete(d: Deliverable): void { this.deleteTarget = d; this.showDelete = true; }
  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.api.deleteDeliverable(this.deleteTarget.id).subscribe({
      next: () => {
        this.allDeliverables = this.allDeliverables.filter(x => x.id !== this.deleteTarget!.id);
        this.applyFilters(); this.showDelete = false;
      }
    });
  }

  goBack(): void { this.router.navigate(['/admin/projects']); }
  projectTitle(id: number): string { return this.projects.find(p => p.id === id)?.title || `#${id}`; }

  statusBadge(s: string): string {
    return ({ SUBMITTED: 'badge-warning', APPROVED: 'badge-success', REJECTED: 'badge-danger' } as any)[s] || 'badge-muted';
  }
  statusLabel(s: string): string {
    return ({ SUBMITTED: 'En attente', APPROVED: 'Approuvé', REJECTED: 'Rejeté' } as any)[s] || s;
  }
}
