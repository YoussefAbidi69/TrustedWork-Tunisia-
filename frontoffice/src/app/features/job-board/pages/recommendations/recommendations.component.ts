import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { JobBoardService } from '../../services/job-board.service';
import { AuthService } from '../../../../core/services/auth.service';
import { SkillStoreService } from '../../services/skill-store.service';
import { JobOffer, RecommendationRow, SuccessPrediction } from '../../models/job-board.models';
import { accordionExpand, slideUpStagger } from '../../animations/job-board.animations';

@Component({
  selector: 'app-recommendations',
  templateUrl: './recommendations.component.html',
  styleUrls: ['./recommendations.component.scss'],
  animations: [accordionExpand, slideUpStagger]
})
export class RecommendationsComponent implements OnInit, AfterViewInit, OnDestroy {
  loading = true;
  error: string | null = null;
  rows: RecommendationRow[] = [];
  expandedMap: Record<number, boolean> = {};
  visibleMap: Record<number, boolean> = {};
  animatedScores: Record<number, number> = {};
  updatedLabel = 'just now';

  scoreFilter: 'ALL' | '50' | '70' | '85' = 'ALL';
  sortBy: 'MATCH' | 'OPPORTUNITY' | 'RECENT' = 'MATCH';

  // Success Prediction state
  predictionMap: Record<number, SuccessPrediction> = {};
  predictionLoading: Record<number, boolean> = {};
  predictionExpanded: Record<number, boolean> = {};
  private currentUserId = 0;

  private observer?: IntersectionObserver;

  constructor(
    private jobBoard: JobBoardService,
    private auth: AuthService,
    private skillStore: SkillStoreService,
    private router: Router,
    private host: ElementRef<HTMLElement>
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentAuthUser();
    if (user) this.currentUserId = user.userId;
    this.refreshRecommendations();
  }

  ngAfterViewInit(): void {
    this.setupObserver();
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }



  get filteredRows(): RecommendationRow[] {
    let rows = [...this.rows];
    if (this.scoreFilter !== 'ALL') {
      const min = Number(this.scoreFilter);
      rows = rows.filter((r) => (r.matchScore || 0) >= min);
    }
    if (this.sortBy === 'OPPORTUNITY') {
      rows.sort((a, b) => (b.opportunityScore || 0) - (a.opportunityScore || 0));
    } else if (this.sortBy === 'RECENT') {
      rows.sort((a, b) => ((b.freshnessScore ?? b.freshnessFactor ?? 0) - (a.freshnessScore ?? a.freshnessFactor ?? 0)));
    } else {
      rows.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0));
    }
    return rows;
  }

  refreshRecommendations(): void {
    const uid = this.auth.getCurrentAuthUser()?.userId;
    if (!uid) {
      this.error = 'Missing profile.';
      this.loading = false;
      return;
    }
    this.jobBoard.getRecommendations(uid, []).subscribe({
      next: (r) => this.enrichRows(r),
      error: () => {
        this.loading = false;
        this.error = 'Unable to load recommendations.';
      }
    });
  }

  retry(): void {
    this.refreshRecommendations();
  }



  rankBadge(index: number): string {
    if (index === 0) return '🥇';
    if (index === 1) return '🥈';
    if (index === 2) return '🥉';
    return `#${index + 1}`;
  }

  matchPct(row: RecommendationRow): number {
    return Math.max(0, Math.min(100, Math.round(row.matchScore || 0)));
  }

  oppPct(row: RecommendationRow): number {
    return Math.max(0, Math.min(100, Math.round(row.opportunityScore || 0)));
  }

  freshPct(row: RecommendationRow): number {
    const raw = row.freshnessScore ?? row.freshnessFactor ?? 0;
    // Backend returns freshness on 0-1 scale (e.g. 0.2, 0.5, 1.0)
    const scaled = raw <= 1 ? raw * 100 : raw;
    return Math.max(0, Math.min(100, Math.round(scaled)));
  }

  strokeOffset(row: RecommendationRow): number {
    const c = 2 * Math.PI * 38;
    const pct = this.visibleMap[row.jobOfferId] ? this.matchPct(row) : 0;
    return c - (pct / 100) * c;
  }

  overlapSkills(row: RecommendationRow): string[] {
    return row.topMatchingSkills || [];
  }

  missingSkills(job?: JobOffer, row?: RecommendationRow): string[] {
    if (!job?.requiredSkills?.length) return [];
    const matched = (row?.topMatchingSkills || []).map(s => s.toLowerCase());
    return job.requiredSkills.filter((s) => !matched.includes(s.toLowerCase()));
  }

  hasSkills(job?: JobOffer, row?: RecommendationRow): string[] {
    if (!job?.requiredSkills?.length) return [];
    const matched = (row?.topMatchingSkills || []).map(s => s.toLowerCase());
    return job.requiredSkills.filter((s) => matched.includes(s.toLowerCase()));
  }

  toggleExpand(jobId: number): void {
    this.expandedMap[jobId] = !this.expandedMap[jobId];
    this.expandedMap = { ...this.expandedMap };
  }

  isExpanded(jobId: number): boolean {
    return !!this.expandedMap[jobId];
  }

  whyMatchText(row: RecommendationRow): string {
    const overlap = this.overlapSkills(row).length;
    const opp = this.oppPct(row);
    const fresh = this.freshPct(row);
    return `You match ${overlap} core skills. Opportunity score is ${opp}% and freshness is ${fresh}%, so this role ranks high for timing and compatibility.`;
  }

  /* ── Success Prediction ── */

  togglePrediction(jobId: number): void {
    this.predictionExpanded[jobId] = !this.predictionExpanded[jobId];
    this.predictionExpanded = { ...this.predictionExpanded };
    // Fetch if not already loaded
    if (this.predictionExpanded[jobId] && !this.predictionMap[jobId] && !this.predictionLoading[jobId]) {
      this.loadPrediction(jobId);
    }
  }

  isPredictionExpanded(jobId: number): boolean {
    return !!this.predictionExpanded[jobId];
  }

  private loadPrediction(jobId: number): void {
    if (!this.currentUserId) return;
    this.predictionLoading[jobId] = true;
    this.predictionLoading = { ...this.predictionLoading };

    const skills = this.skillStore.getSkills();
    this.jobBoard.postSuccessPrediction({
      jobOfferId: jobId,
      freelancerId: this.currentUserId,
      freelancerSkills: skills
    }).pipe(
      catchError(() => {
        // Fallback: try the inline data from the recommendation row itself
        const row = this.rows.find(r => r.jobOfferId === jobId);
        if (row?.successProbability != null) {
          return of({
            probability: row.successProbability,
            confidenceLabel: row.confidence || 'MEDIUM'
          } as SuccessPrediction);
        }
        return of(null);
      })
    ).subscribe(pred => {
      this.predictionLoading[jobId] = false;
      this.predictionLoading = { ...this.predictionLoading };
      if (pred) {
        this.predictionMap[jobId] = pred;
        this.predictionMap = { ...this.predictionMap };
      }
    });
  }

  predictionPct(jobId: number): number {
    const p = this.predictionMap[jobId];
    if (!p) return 0;
    // Backend returns probability on 0-1 scale (e.g. 0.72 = 72%)
    const raw = p.probability;
    const scaled = raw <= 1 ? raw * 100 : raw;
    return Math.max(0, Math.min(100, Math.round(scaled)));
  }

  predictionColor(jobId: number): string {
    const pct = this.predictionPct(jobId);
    if (pct >= 70) return '#2d8b67';
    if (pct >= 40) return '#c4841d';
    return '#c0392b';
  }

  predictionGaugeOffset(jobId: number): number {
    const c = 2 * Math.PI * 38;
    const pct = this.predictionPct(jobId);
    return c - (pct / 100) * c;
  }

  confidenceClass(label: string | undefined): string {
    switch ((label || '').toUpperCase()) {
      case 'HIGH': return 'confidence-high';
      case 'MEDIUM': return 'confidence-medium';
      case 'LOW': return 'confidence-low';
      default: return 'confidence-medium';
    }
  }

  /** Quick inline probability from the row itself (before detail fetch) */
  inlineProbability(row: RecommendationRow): number {
    const raw = row.successProbability ?? 0;
    // Backend returns probability on 0-1 scale (e.g. 0.72 = 72%)
    const scaled = raw <= 1 ? raw * 100 : raw;
    return Math.max(0, Math.min(100, Math.round(scaled)));
  }

  inlineConfidence(row: RecommendationRow): string {
    return row.confidence || 'N/A';
  }

  inlineProbColor(row: RecommendationRow): string {
    const pct = this.inlineProbability(row);
    if (pct >= 70) return '#2d8b67';
    if (pct >= 40) return '#c4841d';
    return '#c0392b';
  }

  openJob(jobId: number): void {
    void this.router.navigate(['/app/job-board/marketplace'], { queryParams: { job: jobId } });
  }

  quickApply(jobId: number): void {
    void this.router.navigate(['/app/job-board/marketplace'], { queryParams: { job: jobId } });
  }

  save(_row: RecommendationRow, event: Event): void {
    event.stopPropagation();
  }

  trackByRow(_i: number, row: RecommendationRow): number {
    return row.jobOfferId;
  }



  private enrichRows(rows: RecommendationRow[]): void {
    const missing = [...new Set(rows.filter((row) => !row.job).map((row) => row.jobOfferId))];
    if (!missing.length) {
      this.rows = rows;
      this.loading = false;
      this.afterRowsRendered();
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
        if (j) byId.set(id, j);
      });
      this.rows = rows.map((row) => ({ ...row, job: row.job || byId.get(row.jobOfferId) }));
      this.loading = false;
      this.afterRowsRendered();
    });
  }

  private setupObserver(): void {
    this.observer?.disconnect();
    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const id = Number((entry.target as HTMLElement).dataset['jobid']);
          if (!Number.isFinite(id)) continue;
          if (entry.isIntersecting) {
            this.triggerCardAnimation(id);
          }
        }
      },
      { threshold: 0.15 }
    );
    const cards = this.host.nativeElement.querySelectorAll<HTMLElement>('.rec-card-custom');
    cards.forEach((c) => this.observer?.observe(c));

    // Immediately trigger animation for cards already visible in the viewport
    setTimeout(() => {
      cards.forEach((c) => {
        const rect = c.getBoundingClientRect();
        if (rect.top < window.innerHeight && rect.bottom > 0) {
          const id = Number(c.dataset['jobid']);
          if (Number.isFinite(id)) {
            this.triggerCardAnimation(id);
          }
        }
      });
    }, 200);
  }

  private triggerCardAnimation(id: number): void {
    if (this.visibleMap[id]) return;
    this.visibleMap[id] = true;
    this.visibleMap = { ...this.visibleMap };
    const row = this.rows.find((r) => r.jobOfferId === id);
    if (row) {
      this.animateScore(id, this.matchPct(row));
    }
  }

  private animateScore(id: number, target: number): void {
    const duration = 900; // ms
    const startTime = performance.now();
    
    const step = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      // easeOutQuart
      const ease = 1 - Math.pow(1 - progress, 4);
      
      this.animatedScores[id] = Math.round(ease * target);
      this.animatedScores = { ...this.animatedScores };
      
      if (progress < 1) {
        requestAnimationFrame(step);
      }
    };
    
    requestAnimationFrame(step);
  }

  private afterRowsRendered(): void {
    this.updatedLabel = 'just now';
    setTimeout(() => this.setupObserver(), 100);
  }


}
