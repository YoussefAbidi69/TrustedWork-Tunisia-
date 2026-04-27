import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AdminJobBoardService } from '../../services/admin-job-board.service';
import { JobOffer, OfferFlag, PlatformStatsDto } from '../../models/admin-job-board.models';
import { accordionExpand } from '../../animations/admin.animations';

@Component({
  selector: 'app-admin-fraud',
  templateUrl: './admin-fraud.component.html',
  styleUrls: ['./admin-fraud.component.scss'],
  animations: [accordionExpand]
})
export class AdminFraudComponent implements OnInit {
  flaggedJobs: JobOffer[] = [];
  highRiskJobs: JobOffer[] = [];
  autoFlaggedToday: JobOffer[] = [];
  stats: PlatformStatsDto | null = null;
  loading = false;
  error: string | null = null;
  expandedRows = new Set<number>();
  jobFlags = new Map<number, OfferFlag[]>();
  selectedRows = new Set<number>();
  actionLoading = new Map<number, string>();
  deleteConfirmJob: JobOffer | null = null;
  showDeleteConfirm = false;
  showBulkRemoveConfirm = false;

  constructor(private admin: AdminJobBoardService) {}

  get avgRiskScore(): number {
    if (!this.flaggedJobs.length) {
      return 0;
    }
    const sum = this.flaggedJobs.reduce((a, j) => a + j.fraudRiskScore, 0);
    return (sum / this.flaggedJobs.length) * 100;
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.error = null;
    forkJoin({
      jobs: this.admin.getFlaggedJobs(0, 500),
      st: this.admin.getPlatformStats()
    }).subscribe({
      next: ({ jobs, st }) => {
        this.flaggedJobs = jobs.sort((a, b) => b.fraudRiskScore - a.fraudRiskScore);
        this.recomputeDerived();
        this.stats = st;
        this.loading = false;
        for (const j of this.highRiskJobs) {
          this.loadFlags(j.id);
        }
      },
      error: (e: Error) => {
        this.error = e.message;
        this.loading = false;
      }
    });
  }

  loadFlags(jobId: number): void {
    if (this.jobFlags.has(jobId)) {
      return;
    }
    this.admin.getJobFlags(jobId).subscribe({
      next: (f) => this.jobFlags.set(jobId, f),
      error: () => this.jobFlags.set(jobId, [])
    });
  }

  private recomputeDerived(): void {
    this.highRiskJobs = this.flaggedJobs.filter((j) => j.fraudRiskScore > 0.6);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    this.autoFlaggedToday = this.flaggedJobs.filter((j) => {
      const c = j.createdAt ? new Date(j.createdAt) : null;
      return c != null && !Number.isNaN(c.getTime()) && c >= today;
    });
  }

  toggleRow(id: number): void {
    if (this.expandedRows.has(id)) {
      this.expandedRows.delete(id);
    } else {
      this.expandedRows.add(id);
      this.loadFlags(id);
    }
    this.expandedRows = new Set(this.expandedRows);
  }

  signalPreview(job: JobOffer): OfferFlag[] {
    const list = this.jobFlags.get(job.id);
    return list ? list.slice(0, 3) : [];
  }

  signalMoreCount(job: JobOffer): number {
    const list = this.jobFlags.get(job.id);
    if (!list || list.length <= 3) {
      return 0;
    }
    return list.length - 3;
  }

  isActionLoading(id: number, key: string): boolean {
    return this.actionLoading.get(id) === key;
  }

  onApprove(job: JobOffer): void {
    this.actionLoading.set(job.id, 'approve');
    this.admin.unflagJob(job.id).subscribe({
      next: () => {
        this.flaggedJobs = this.flaggedJobs.filter((j) => j.id !== job.id);
        this.jobFlags.delete(job.id);
        this.selectedRows.delete(job.id);
        this.selectedRows = new Set(this.selectedRows);
        this.actionLoading.delete(job.id);
        this.recomputeDerived();
        this.admin.getPlatformStats().subscribe({ next: (s) => (this.stats = s) });
      },
      error: () => {
        this.actionLoading.delete(job.id);
        this.reload();
      }
    });
  }

  onRemove(job: JobOffer): void {
    if (!window.confirm('Force-close this flagged job?')) {
      return;
    }
    this.actionLoading.set(job.id, 'remove');
    this.admin.forceCloseJob(job.id).subscribe({
      next: () => {
        this.flaggedJobs = this.flaggedJobs.filter((j) => j.id !== job.id);
        this.jobFlags.delete(job.id);
        this.selectedRows.delete(job.id);
        this.selectedRows = new Set(this.selectedRows);
        this.actionLoading.delete(job.id);
        this.recomputeDerived();
        this.admin.getPlatformStats().subscribe({ next: (s) => (this.stats = s) });
      },
      error: () => {
        this.actionLoading.delete(job.id);
        this.reload();
      }
    });
  }

  openDeleteConfirm(job: JobOffer): void {
    this.deleteConfirmJob = job;
    this.showDeleteConfirm = true;
  }

  onDeleteConfirmed(): void {
    const job = this.deleteConfirmJob;
    if (!job) {
      return;
    }
    this.showDeleteConfirm = false;
    this.deleteConfirmJob = null;
    this.actionLoading.set(job.id, 'delete');
    this.admin.deleteJob(job.id).subscribe({
      next: () => {
        this.flaggedJobs = this.flaggedJobs.filter((j) => j.id !== job.id);
        this.jobFlags.delete(job.id);
        this.selectedRows.delete(job.id);
        this.selectedRows = new Set(this.selectedRows);
        this.actionLoading.delete(job.id);
        this.recomputeDerived();
        this.admin.getPlatformStats().subscribe({ next: (s) => (this.stats = s) });
      },
      error: () => {
        this.actionLoading.delete(job.id);
        this.reload();
      }
    });
  }

  toggleSelectRow(id: number): void {
    if (this.selectedRows.has(id)) {
      this.selectedRows.delete(id);
    } else {
      this.selectedRows.add(id);
    }
    this.selectedRows = new Set(this.selectedRows);
  }

  allPageSelected(): boolean {
    return this.flaggedJobs.length > 0 && this.flaggedJobs.every((j) => this.selectedRows.has(j.id));
  }

  toggleSelectAll(): void {
    if (this.allPageSelected()) {
      for (const j of this.flaggedJobs) {
        this.selectedRows.delete(j.id);
      }
    } else {
      for (const j of this.flaggedJobs) {
        this.selectedRows.add(j.id);
      }
    }
    this.selectedRows = new Set(this.selectedRows);
  }

  bulkApprove(): void {
    const ids = [...this.selectedRows];
    if (!ids.length) {
      return;
    }
    forkJoin(ids.map((id) => this.admin.unflagJob(id))).subscribe({
      next: () => {
        this.clearSelection();
        this.reload();
      },
      error: () => this.reload()
    });
  }

  openBulkRemove(): void {
    if (this.selectedRows.size > 0) {
      this.showBulkRemoveConfirm = true;
    }
  }

  bulkRemoveConfirmed(): void {
    const ids = [...this.selectedRows];
    this.showBulkRemoveConfirm = false;
    if (!ids.length) {
      return;
    }
    forkJoin(ids.map((id) => this.admin.forceCloseJob(id))).subscribe({
      next: () => {
        this.clearSelection();
        this.reload();
      },
      error: () => this.reload()
    });
  }

  clearSelection(): void {
    this.selectedRows.clear();
    this.selectedRows = new Set();
  }

  riskBarClass(score: number): string {
    if (score < 0.3) {
      return 'risk-bar-fill risk-bar-fill--low';
    }
    if (score < 0.6) {
      return 'risk-bar-fill risk-bar-fill--mod';
    }
    return 'risk-bar-fill risk-bar-fill--high';
  }

  riskLabel(score: number): string {
    if (score < 0.3) {
      return 'Low Risk';
    }
    if (score < 0.6) {
      return 'Moderate Risk';
    }
    return 'High Risk';
  }
}
