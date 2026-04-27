import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { JobBoardService } from '../../services/job-board.service';
import { AuthService } from '../../../../core/services/auth.service';
import { JobOffer, RecommendationRow } from '../../models/job-board.models';
import { accordionExpand } from '../../animations/job-board.animations';

@Component({
  selector: 'app-recommendations',
  templateUrl: './recommendations.component.html',
  styleUrls: ['./recommendations.component.scss'],
  animations: [accordionExpand]
})
export class RecommendationsComponent implements OnInit {
  loading = true;
  error: string | null = null;
  rows: RecommendationRow[] = [];
  expandedMap: Record<number, boolean> = {};
  mySkills: string[] = [];

  constructor(
    private jobBoard: JobBoardService,
    private auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.refreshRecommendations();
  }

  refreshRecommendations(): void {
    const uid = this.auth.getCurrentAuthUser()?.userId;
    if (!uid) {
      this.error = 'Missing profile.';
      this.loading = false;
      return;
    }
    this.loading = true;
    this.error = null;
    this.jobBoard.getRecommendations(uid, this.mySkills).subscribe({
      next: (r) => {
        this.enrichRows(r);
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load recommendations.';
      }
    });
  }

  retry(): void {
    this.refreshRecommendations();
  }

  private enrichRows(rows: RecommendationRow[]): void {
    const missing = [...new Set(rows.filter((row) => !row.job).map((row) => row.jobOfferId))];
    if (!missing.length) {
      this.rows = rows;
      this.loading = false;
      return;
    }
    forkJoin(
      missing.map((id) =>
        this.jobBoard.getJobById(id).pipe(
          map((j) => j as JobOffer | undefined),
          catchError(() => of(undefined))
        )
      )
    ).subscribe((jobs: (JobOffer | undefined)[]) => {
      const byId = new Map<number, JobOffer>();
      missing.forEach((id, i) => {
        const j = jobs[i];
        if (j) {
          byId.set(id, j);
        }
      });
      this.rows = rows.map((row) => {
        const j = byId.get(row.jobOfferId);
        return j ? { ...row, job: j } : row;
      });
      this.loading = false;
    });
  }

  initials(job: JobOffer): string {
    const t = (job.category || job.title).replace(/[^a-zA-Z0-9]/g, '').slice(0, 2);
    return t ? t.toUpperCase() : '—';
  }

  highlights(job: JobOffer): string[] {
    const h: string[] = [];
    if (job.budgetMax >= 2000) {
      h.push('High-budget client');
    }
    if (job.remote) {
      h.push('Remote-friendly');
    }
    if ((job.opportunityScore ?? 0) >= 70) {
      h.push('High opportunity score');
    }
    if ((job.applicationCount ?? 0) < 5) {
      h.push('Low competition');
    }
    if ((job.extractedSkills?.length ?? 0) >= 5) {
      h.push('Skill-rich mission');
    }
    if (job.budgetMax - job.budgetMin < 200) {
      h.push('Fixed-scope mission');
    }
    return h.slice(0, 3);
  }

  statusFor(job: JobOffer): { label: string; cls: string } {
    if (job.status === 'FLAGGED') {
      return { label: 'Flagged', cls: 'status-flagged' };
    }
    if (job.status !== 'PUBLISHED') {
      return { label: 'Recommended', cls: 'status-recommended' };
    }
    const pub = job.publishedAt ? new Date(job.publishedAt) : null;
    const now = new Date();
    const hours = pub ? (now.getTime() - pub.getTime()) / 36e5 : 999;
    if ((job.opportunityScore ?? 0) > 75) {
      return { label: 'Featured', cls: 'status-featured' };
    }
    if (pub && pub.toDateString() === now.toDateString()) {
      return { label: 'Hot', cls: 'status-hot' };
    }
    if (hours < 48) {
      return { label: 'New', cls: 'status-new' };
    }
    return { label: 'Recommended', cls: 'status-recommended' };
  }

  rankBadge(index: number): { label: string; cls: string } {
    if (index === 0) {
      return { label: '🥇 Best Match', cls: 'status-featured' };
    }
    if (index === 1) {
      return { label: '🥈 Second', cls: 'status-hot' };
    }
    if (index === 2) {
      return { label: '🥉 Third', cls: 'status-new' };
    }
    return { label: '#' + (index + 1), cls: '' };
  }

  toggleExpand(jobId: number): void {
    this.expandedMap[jobId] = !this.expandedMap[jobId];
    this.expandedMap = { ...this.expandedMap };
  }

  isExpanded(jobId: number): boolean {
    return !!this.expandedMap[jobId];
  }

  addSkill(ev: Event): void {
    const e = ev.target as HTMLInputElement;
    const v = e.value.trim();
    if (!v || this.mySkills.includes(v)) {
      return;
    }
    this.mySkills = [...this.mySkills, v];
    e.value = '';
  }

  removeSkill(s: string): void {
    this.mySkills = this.mySkills.filter((x) => x !== s);
  }

  openJob(jobId: number): void {
    void this.router.navigate(['/app/job-board/marketplace'], { queryParams: { job: jobId } });
  }

  trackByRow(_i: number, row: RecommendationRow): number {
    return row.jobOfferId;
  }

  trackBySkill(_i: number, s: string): string {
    return s;
  }

  trackByText(i: number, t: string): string {
    return `${i}-${t}`;
  }

  durationLabel(job: JobOffer): string {
    return job.durationDays != null ? `${job.durationDays} days` : '—';
  }
}
