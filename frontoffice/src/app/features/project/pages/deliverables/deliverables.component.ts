import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectApiService } from '../../services/project-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  Project, Deliverable, DeliverableStatus,
  CreateDeliverableDTO, Task
} from '../../models/project.models';

@Component({
  selector: 'app-deliverables',
  templateUrl: './deliverables.component.html',
  styleUrls: ['./deliverables.component.css']
})
export class DeliverablesComponent implements OnInit {

  projectId!: number;
  project!: Project;
  deliverables: Deliverable[] = [];
  tasks: Task[] = [];
  loading = true;
  error = '';

  // Filtre
  activeFilter: 'ALL' | DeliverableStatus = 'ALL';

  // Modal soumission
  showSubmitModal = false;
  newDeliverable: CreateDeliverableDTO = { title: '', description: '', fileUrl: '', taskId: undefined };

  // Modal review
  showReviewModal = false;
  reviewingDeliverable: Deliverable | null = null;
  reviewStatus: DeliverableStatus = 'APPROVED';
  reviewComment = '';

  // Détail
  showDetailModal = false;
  selectedDeliverable: Deliverable | null = null;

  // Rôle
  currentUserId = 0;
  isClient = false;
  isFreelancer = false;
  isAdmin = false;
currentRole = '';
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectApi: ProjectApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
  this.projectId = Number(this.route.snapshot.paramMap.get('id'));
  const user = this.authService.getCurrentAuthUser();
  this.currentUserId = user?.userId || 0;
  this.currentRole = user?.role?.toUpperCase() || '';
  this.loadData();
  }

  private loadData(): void {
  this.loading = true;

this.projectApi.getProjectById(this.projectId).subscribe({
  next: (p) => {
    this.project = p;
    // ✅ Rôle basé sur le JWT
    this.isClient = this.currentRole === 'CLIENT';
    this.isFreelancer = this.currentRole === 'FREELANCER';
    this.isAdmin = this.currentRole === 'ADMIN';
  }
});

    this.projectApi.getTasksByProjectId(this.projectId).subscribe({
      next: (tasks) => this.tasks = tasks
    });

    this.projectApi.getDeliverablesByProjectId(this.projectId).subscribe({
      next: (data) => {
        this.deliverables = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les livrables.';
        this.loading = false;
      }
    });
  }

  // ══════════════════════════════════════
  // FILTRES
  // ══════════════════════════════════════

  get filteredDeliverables(): Deliverable[] {
    if (this.activeFilter === 'ALL') return this.deliverables;
    return this.deliverables.filter(d => d.status === this.activeFilter);
  }

  setFilter(filter: 'ALL' | DeliverableStatus): void {
    this.activeFilter = filter;
  }

  get countByStatus(): Record<string, number> {
    return {
      ALL: this.deliverables.length,
      SUBMITTED: this.deliverables.filter(d => d.status === 'SUBMITTED').length,
      APPROVED: this.deliverables.filter(d => d.status === 'APPROVED').length,
      REJECTED: this.deliverables.filter(d => d.status === 'REJECTED').length
    };
  }

  // ══════════════════════════════════════
  // SOUMISSION (Freelancer)
  // ══════════════════════════════════════

  openSubmitModal(): void {
    this.newDeliverable = { title: '', description: '', fileUrl: '', taskId: undefined };
    this.showSubmitModal = true;
  }

  closeSubmitModal(): void {
    this.showSubmitModal = false;
  }

  submitDeliverable(): void {
    if (!this.newDeliverable.title.trim() || !this.newDeliverable.fileUrl.trim()) return;

    this.projectApi.submitDeliverable(this.projectId, this.newDeliverable).subscribe({
      next: (created) => {
        this.deliverables.unshift(created);
        this.showSubmitModal = false;
      }
    });
  }

  // ══════════════════════════════════════
  // REVIEW (Client)
  // ══════════════════════════════════════

  openReviewModal(deliverable: Deliverable): void {
    this.reviewingDeliverable = deliverable;
    this.reviewStatus = 'APPROVED';
    this.reviewComment = '';
    this.showReviewModal = true;
  }

  closeReviewModal(): void {
    this.showReviewModal = false;
    this.reviewingDeliverable = null;
  }

  submitReview(): void {
    if (!this.reviewingDeliverable) return;

    this.projectApi.reviewDeliverable(
      this.reviewingDeliverable.id,
      this.reviewStatus,
      this.reviewComment
    ).subscribe({
      next: (updated) => {
        const idx = this.deliverables.findIndex(d => d.id === updated.id);
        if (idx >= 0) this.deliverables[idx] = updated;
        this.showReviewModal = false;
        this.reviewingDeliverable = null;
      }
    });
  }

  // ══════════════════════════════════════
  // DÉTAIL
  // ══════════════════════════════════════

  openDetail(deliverable: Deliverable): void {
    this.selectedDeliverable = deliverable;
    this.showDetailModal = true;
  }

  closeDetail(): void {
    this.showDetailModal = false;
    this.selectedDeliverable = null;
  }

  // ══════════════════════════════════════
  // SUPPRESSION
  // ══════════════════════════════════════

  deleteDeliverable(deliverable: Deliverable, event: Event): void {
    event.stopPropagation();
    if (!confirm('Supprimer le livrable "' + deliverable.title + '" ?')) return;

    this.projectApi.deleteDeliverable(deliverable.id).subscribe({
      next: () => {
        this.deliverables = this.deliverables.filter(d => d.id !== deliverable.id);
      }
    });
  }

  // ══════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════

  goBack(): void {
    this.router.navigate(['/app/projects', this.projectId]);
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      SUBMITTED: 'En attente', APPROVED: 'Approuvé', REJECTED: 'Rejeté'
    };
    return map[status] || status;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      SUBMITTED: 'st--pending', APPROVED: 'st--approved', REJECTED: 'st--rejected'
    };
    return map[status] || '';
  }

  getStatusIcon(status: string): string {
    const map: Record<string, string> = {
      SUBMITTED: 'fa-hourglass-half', APPROVED: 'fa-circle-check', REJECTED: 'fa-circle-xmark'
    };
    return map[status] || 'fa-file';
  }

  getTaskTitle(taskId: number | null): string {
    if (!taskId) return 'Aucune tâche liée';
    const task = this.tasks.find(t => t.id === taskId);
    return task ? task.title : 'Tâche #' + taskId;
  }

  getFileIcon(url: string): string {
    if (!url) return 'fa-file';
    if (url.includes('figma')) return 'fa-pen-nib';
    if (url.includes('github')) return 'fa-code-branch';
    if (url.includes('drive.google')) return 'fa-google-drive';
    if (url.includes('dropbox')) return 'fa-dropbox';
    if (url.endsWith('.pdf')) return 'fa-file-pdf';
    if (url.endsWith('.zip') || url.endsWith('.rar')) return 'fa-file-zipper';
    return 'fa-link';
  }

  formatDate(date: string | null): string {
    if (!date) return '—';
    return new Date(date).toLocaleDateString('fr-TN', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }
}