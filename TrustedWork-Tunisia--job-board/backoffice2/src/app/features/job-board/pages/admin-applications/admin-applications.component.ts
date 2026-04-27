import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AdminJobBoardService } from '../../services/admin-job-board.service';
import { Application } from '../../models/admin-job-board.models';
import { accordionExpand } from '../../animations/admin.animations';

@Component({
  selector: 'app-admin-applications',
  templateUrl: './admin-applications.component.html',
  styleUrls: ['./admin-applications.component.scss'],
  animations: [accordionExpand]
})
export class AdminApplicationsComponent implements OnInit {
  applications: Application[] = [];
  total = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;
  loading = false;
  tableLoading = false;
  error: string | null = null;
  expandedRows = new Set<number>();
  filters: { status: string; minMatchScore: number; page: number; size: number } = {
    status: '',
    minMatchScore: 0,
    page: 0,
    size: 20
  };
  minScoreInput = 0;

  constructor(
    private admin: AdminJobBoardService,
    private router: Router
  ) {}

  /** Counts reflect the current loaded page; total comes from the API. */
  get pageStats(): {
    total: number;
    pending: number;
    shortlisted: number;
    accepted: number;
    rejected: number;
  } {
    let pending = 0;
    let shortlisted = 0;
    let accepted = 0;
    let rejected = 0;
    for (const a of this.applications) {
      if (a.status === 'PENDING') {
        pending++;
      } else if (a.status === 'SHORTLISTED') {
        shortlisted++;
      } else if (a.status === 'ACCEPTED') {
        accepted++;
      } else if (a.status === 'REJECTED' || a.status === 'WITHDRAWN') {
        rejected++;
      }
    }
    return { total: this.total, pending, shortlisted, accepted, rejected };
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    if (!this.applications.length && this.currentPage === 0) {
      this.loading = true;
    } else {
      this.tableLoading = true;
    }
    this.error = null;
    this.admin
      .getAdminApplications({
        status: this.filters.status || undefined,
        page: this.filters.page,
        size: this.filters.size,
        minMatchScore: this.filters.minMatchScore > 0 ? this.filters.minMatchScore : undefined
      })
      .subscribe({
        next: (page) => {
          this.applications = page.content;
          this.total = page.totalElements;
          this.totalPages = page.totalPages;
          this.currentPage = page.number;
          this.loading = false;
          this.tableLoading = false;
        },
        error: (e: Error) => {
          this.error = e.message;
          this.loading = false;
          this.tableLoading = false;
        }
      });
  }

  applyFilters(): void {
    this.filters.minMatchScore = Math.max(0, Math.min(100, Number(this.minScoreInput) || 0));
    this.filters.page = 0;
    this.load();
  }

  onStatusFilter(v: string): void {
    this.filters.status = v;
    this.filters.page = 0;
    this.load();
  }

  onPageChange(p: number): void {
    this.filters.page = p;
    this.load();
  }

  toggleRow(id: number): void {
    if (this.expandedRows.has(id)) {
      this.expandedRows.delete(id);
    } else {
      this.expandedRows.add(id);
    }
    this.expandedRows = new Set(this.expandedRows);
  }

  matchPct(app: Application): number {
    return app.matchScore?.totalScore ?? 0;
  }

  matchBarClass(score: number): string {
    if (score >= 70) {
      return 'match-bar-fill match-high';
    }
    if (score >= 40) {
      return 'match-bar-fill match-mid';
    }
    return 'match-bar-fill match-low';
  }

  statusBadgeClass(st: string): string {
    return `app-${st}`;
  }

  viewJob(jobId: number): void {
    this.router.navigate(['/admin/jobs'], { queryParams: { highlight: String(jobId) } });
  }

  exportCsv(): void {
    const esc = (v: unknown) => `"${String(v ?? '').replace(/"/g, '""')}"`;
    const header = ['id', 'jobOfferId', 'jobTitle', 'freelancerId', 'proposedRate', 'matchScore', 'status', 'appliedAt', 'coverLetter'];
    const lines = [
      header.join(','),
      ...this.applications.map((a) =>
        [
          a.id,
          a.jobOfferId,
          esc(a.jobTitle),
          a.freelancerId,
          a.proposedRate,
          this.matchPct(a).toFixed(1),
          a.status,
          esc(a.appliedAt),
          esc(a.coverLetter)
        ].join(',')
      )
    ];
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `applications-${new Date().toISOString().slice(0, 10)}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  retry(): void {
    this.load();
  }
}
