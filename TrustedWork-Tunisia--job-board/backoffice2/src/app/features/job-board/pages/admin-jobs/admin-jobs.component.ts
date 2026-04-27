import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, Subject, Subscription } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { AdminJobBoardService } from '../../services/admin-job-board.service';
import {
  AdminJobFilters,
  JobOffer,
  JobOfferAdminUpdatePayload,
  OfferFlag,
  PlatformStatsDto
} from '../../models/admin-job-board.models';
import { accordionExpand } from '../../animations/admin.animations';

@Component({
  selector: 'app-admin-jobs',
  templateUrl: './admin-jobs.component.html',
  styleUrls: ['./admin-jobs.component.scss'],
  animations: [accordionExpand]
})
export class AdminJobsComponent implements OnInit, OnDestroy {
  jobs: JobOffer[] = [];
  total = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 15;
  stats: PlatformStatsDto | null = null;
  loading = false;
  tableLoading = false;
  error: string | null = null;
  filters: AdminJobFilters = { page: 0, size: 15 };
  statusSelect = '';
  categorySelect = '';
  searchInput = '';
  sortColumn = 'createdAt';
  sortDir: 'asc' | 'desc' = 'desc';
  expandedRows = new Set<number>();
  selectedRows = new Set<number>();
  jobFlags = new Map<number, OfferFlag[]>();
  categories: string[] = [];
  /** job id → action key: flag | unflag | close | delete */
  actionLoading = new Map<number, string>();
  editingJob: JobOffer | null = null;
  skillsEditText = '';
  showEditModal = false;
  deleteConfirmJob: JobOffer | null = null;
  showDeleteConfirm = false;
  showBulkDeleteConfirm = false;
  highlightJobId: number | null = null;

  private search$ = new Subject<string>();
  private searchSub?: Subscription;
  private categoriesAccum = new Set<string>();

  constructor(
    private admin: AdminJobBoardService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const h = params.get('highlight');
      this.highlightJobId = h != null && h !== '' ? +h : null;
      if (this.highlightJobId && !Number.isNaN(this.highlightJobId)) {
        setTimeout(() => this.scrollToHighlight(), 400);
      }
    });
    this.searchSub = this.search$.pipe(debounceTime(400)).subscribe((term) => {
      this.filters.search = term.trim() || undefined;
      this.filters.page = 0;
      this.loadJobs();
    });
    this.loading = true;
    this.loadJobs(true);
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  private scrollToHighlight(): void {
    if (this.highlightJobId == null) {
      return;
    }
    const el = document.getElementById(`job-row-${this.highlightJobId}`);
    el?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  loadJobs(initial = false): void {
    if (initial) {
      this.loading = true;
    } else {
      this.tableLoading = true;
    }
    this.error = null;
    const f: AdminJobFilters = {
      ...this.filters,
      size: this.pageSize,
      status: this.statusSelect || undefined,
      category: this.categorySelect || undefined,
      sort: `${this.sortColumn},${this.sortDir}`
    };
    forkJoin({
      page: this.admin.getAdminJobs(f),
      st: this.admin.getPlatformStats()
    }).subscribe({
      next: ({ page, st }) => {
        this.jobs = page.content ?? [];
        this.total = page.totalElements;
        this.totalPages = page.totalPages;
        this.currentPage = page.number;
        this.stats = st;
        for (const j of this.jobs) {
          if (j.category) {
            this.categoriesAccum.add(j.category);
          }
        }
        this.categories = [...this.categoriesAccum].sort();
        this.loading = false;
        this.tableLoading = false;
        setTimeout(() => this.scrollToHighlight(), 50);
      },
      error: (e: Error) => {
        this.error = e.message;
        this.loading = false;
        this.tableLoading = false;
      }
    });
  }

  onSearchInput(v: string): void {
    this.searchInput = v;
    this.search$.next(v);
  }

  onStatusFilter(v: string): void {
    this.statusSelect = v;
    this.filters.page = 0;
    this.loadJobs();
  }

  onCategoryFilter(v: string): void {
    this.categorySelect = v;
    this.filters.page = 0;
    this.loadJobs();
  }

  onPageChange(page: number): void {
    this.filters.page = page;
    this.loadJobs();
  }

  onSort(column: string): void {
    if (this.sortColumn === column) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDir = column === 'title' || column === 'category' ? 'asc' : 'desc';
    }
    this.filters.page = 0;
    this.loadJobs();
  }

  sortIndicator(col: string): string {
    if (this.sortColumn !== col) {
      return '';
    }
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  toggleRow(job: JobOffer): void {
    if (this.expandedRows.has(job.id)) {
      this.expandedRows.delete(job.id);
    } else {
      this.expandedRows.add(job.id);
      if (!this.jobFlags.has(job.id)) {
        this.admin.getJobFlags(job.id).subscribe({
          next: (flags) => this.jobFlags.set(job.id, flags),
          error: () => this.jobFlags.set(job.id, [])
        });
      }
    }
    this.expandedRows = new Set(this.expandedRows);
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
    return this.jobs.length > 0 && this.jobs.every((j) => this.selectedRows.has(j.id));
  }

  toggleSelectAll(): void {
    if (this.allPageSelected()) {
      for (const j of this.jobs) {
        this.selectedRows.delete(j.id);
      }
    } else {
      for (const j of this.jobs) {
        this.selectedRows.add(j.id);
      }
    }
    this.selectedRows = new Set(this.selectedRows);
  }

  clearSelection(): void {
    this.selectedRows.clear();
    this.selectedRows = new Set();
  }

  isActionLoading(id: number, action: string): boolean {
    return this.actionLoading.get(id) === action;
  }

  onFlag(job: JobOffer): void {
    const prev = { ...job };
    this.actionLoading.set(job.id, 'flag');
    job.status = 'FLAGGED';
    this.admin.flagJob(job.id).subscribe({
      next: (dto) => {
        Object.assign(job, dto);
        this.actionLoading.delete(job.id);
        this.admin.getPlatformStats().subscribe({ next: (s) => (this.stats = s) });
      },
      error: () => {
        Object.assign(job, prev);
        this.actionLoading.delete(job.id);
        this.error = 'Flag failed';
      }
    });
  }

  onUnflag(job: JobOffer): void {
    const prev = { ...job };
    this.actionLoading.set(job.id, 'unflag');
    job.status = 'PUBLISHED';
    job.fraudRiskScore = 0;
    this.admin.unflagJob(job.id).subscribe({
      next: (dto) => {
        Object.assign(job, dto);
        this.jobFlags.delete(job.id);
        this.actionLoading.delete(job.id);
        this.admin.getPlatformStats().subscribe({ next: (s) => (this.stats = s) });
      },
      error: () => {
        Object.assign(job, prev);
        this.actionLoading.delete(job.id);
        this.error = 'Unflag failed';
      }
    });
  }

  onForceClose(job: JobOffer): void {
    if (!window.confirm('Force-close this job? It will be marked CLOSED.')) {
      return;
    }
    const prev = { ...job };
    this.actionLoading.set(job.id, 'close');
    job.status = 'CLOSED';
    this.admin.forceCloseJob(job.id).subscribe({
      next: (dto) => {
        Object.assign(job, dto);
        this.actionLoading.delete(job.id);
        this.admin.getPlatformStats().subscribe({ next: (s) => (this.stats = s) });
      },
      error: () => {
        Object.assign(job, prev);
        this.actionLoading.delete(job.id);
        this.error = 'Close failed';
      }
    });
  }

  openEditModal(job: JobOffer): void {
    this.editingJob = { ...job, requiredSkills: [...(job.requiredSkills ?? [])] };
    this.skillsEditText = (job.requiredSkills ?? []).join(', ');
    this.showEditModal = true;
  }

  onEditSave(): void {
    if (!this.editingJob) {
      return;
    }
    const j = this.editingJob;
    const skills = this.skillsEditText
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    const payload: JobOfferAdminUpdatePayload = {
      title: (j.title ?? '').trim(),
      description: (j.description ?? '').trim(),
      category: (j.category ?? '').trim(),
      requiredSkills: skills,
      budgetMin: Number(j.budgetMin),
      budgetMax: Number(j.budgetMax),
      durationDays: j.durationDays ?? undefined,
      location: j.location?.trim() || undefined,
      remote: !!j.remote,
      expiresAt: j.expiresAt ?? undefined
    };
    this.admin.editJob(j.id, payload).subscribe({
      next: (dto) => {
        const idx = this.jobs.findIndex((x) => x.id === dto.id);
        if (idx >= 0) {
          this.jobs[idx] = dto;
        }
        this.showEditModal = false;
        this.editingJob = null;
        this.admin.getPlatformStats().subscribe({ next: (s) => (this.stats = s) });
      },
      error: (e: Error) => {
        this.error = e.message;
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
        this.jobs = this.jobs.filter((j) => j.id !== job.id);
        this.selectedRows.delete(job.id);
        this.selectedRows = new Set(this.selectedRows);
        this.actionLoading.delete(job.id);
        this.loadJobs();
        this.admin.getPlatformStats().subscribe({ next: (s) => (this.stats = s) });
      },
      error: (e: Error) => {
        this.actionLoading.delete(job.id);
        this.error = e.message;
      }
    });
  }

  bulkFlag(): void {
    const ids = [...this.selectedRows];
    if (!ids.length) {
      return;
    }
    forkJoin(ids.map((id) => this.admin.flagJob(id))).subscribe({
      next: () => {
        this.clearSelection();
        this.loadJobs();
      },
      error: () => this.loadJobs()
    });
  }

  bulkClose(): void {
    const ids = [...this.selectedRows];
    if (!ids.length || !window.confirm(`Force-close ${ids.length} job(s)?`)) {
      return;
    }
    forkJoin(ids.map((id) => this.admin.forceCloseJob(id))).subscribe({
      next: () => {
        this.clearSelection();
        this.loadJobs();
      },
      error: () => this.loadJobs()
    });
  }

  openBulkDeleteConfirm(): void {
    if (this.selectedRows.size > 0) {
      this.showBulkDeleteConfirm = true;
    }
  }

  onBulkDeleteConfirmed(): void {
    const ids = [...this.selectedRows];
    this.showBulkDeleteConfirm = false;
    if (!ids.length) {
      return;
    }
    forkJoin(ids.map((id) => this.admin.deleteJob(id))).subscribe({
      next: () => {
        this.clearSelection();
        this.loadJobs();
      },
      error: () => this.loadJobs()
    });
  }

  retry(): void {
    this.loadJobs();
  }

  opportunityLabel(score: number): string {
    if (score >= 70) {
      return 'Strong';
    }
    if (score >= 40) {
      return 'Fair';
    }
    return 'Low';
  }

  rowFlagged(job: JobOffer): boolean {
    return job.status === 'FLAGGED';
  }
}
