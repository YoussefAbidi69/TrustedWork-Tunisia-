import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of, Subject, Subscription } from 'rxjs';
import { catchError, debounceTime } from 'rxjs/operators';
import { JobBoardService } from '../../services/job-board.service';
import {
  FraudSignalDto,
  JobApplication,
  JobFilters,
  JobOffer,
  MatchFreelancerRow,
  Page,
  RecommendationRow,
  SuccessPrediction
} from '../../models/job-board.models';
import { AuthService } from '../../../../core/services/auth.service';
import { fadeIn, slideDownForm } from '../../animations/job-board.animations';

type CardStatusLabel = 'Featured' | 'Hot' | 'New' | 'Recommended' | 'Flagged';

export interface FeedJobView {
  id: number;
  title: string;
  category: string;
  initials: string;
  location: string;
  isRemote: boolean;
  description: string;
  requiredSkills: string[];
  extractedSkills: string[];
  highlights: string[];
  budgetMin: number;
  budgetMax: number;
  duration: string;
  experienceLabel: string;
  applicationCount: number;
  publishedAt: string | null;
  opportunityScore: number;
  fraudRiskScore: number;
  statusLabel: CardStatusLabel;
  statusClass: string;
  matchScore: number | null;
  verified: boolean;
  raw: JobOffer;
}

interface OpportunityStat {
  label: string;
  value: string;
  caption: string;
}

interface MarketSignal {
  label: string;
  value: string;
  trend: 'up' | 'neutral';
}

export interface HighlightDetail {
  title: string;
  description: string;
}

@Component({
  selector: 'app-marketplace',
  templateUrl: './marketplace.component.html',
  styleUrls: ['./marketplace.component.scss'],
  animations: [slideDownForm, fadeIn]
})
export class MarketplaceComponent implements OnInit, OnDestroy {
  heroStats: OpportunityStat[] = [
    { label: 'Open opportunities', value: '—', caption: 'Total published roles matching your filters' },
    { label: 'Avg. profile match', value: '—', caption: 'Average of match scores for the current page' },
    { label: 'Market signals', value: '—', caption: 'Top skills by demand on the job board' }
  ];

  marketSignals: MarketSignal[] = [];

  categoryFilters: string[] = ['All'];
  selectedCategory = 'All';
  readonly levelFilters = ['All levels', 'Senior', 'Mid', 'Junior'];
  selectedLevel = 'All levels';
  featuredOnly = false;
  sortFilter: 'LATEST' | 'MATCH' | 'PAY' = 'LATEST';

  apiLoading = true;
  apiError: string | null = null;
  feedJobs: FeedJobView[] = [];
  page: Page<JobOffer> | null = null;
  pageIndex = 0;
  readonly pageSize = 20;

  selectedJob: FeedJobView | null = null;
  selectedDetail: JobOffer | null = null;
  detailLoading = false;
  panelMatchScore: number | null = null;
  prediction: SuccessPrediction | null = null;
  myApplications: JobApplication[] = [];
  myApplicationsError: string | null = null;

  savedJobs = new Set<number>();
  applyFormOpen = false;
  applyForm: FormGroup;
  submitting = false;
  submitError: string | null = null;
  submitSuccess = false;
  generatingCoverLetter = false;
  showEnhancementUndo = false;
  previousCoverLetter = '';
  livePrediction: SuccessPrediction | null = null;
  /** Job IDs the current user has already applied to (synced with API + optimistic updates). */
  appliedJobIds = new Set<number>();
  mySkills: string[] = [];

  filterCategory = '';
  filterSkill = '';
  filterRemoteOnly = false;
  private filter$ = new Subject<void>();
  private filterSub?: Subscription;
  private pendingSelectJobId: number | null = null;

  constructor(
    private jobBoard: JobBoardService,
    private auth: AuthService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.applyForm = this.fb.group({
      coverLetter: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(8000)]],
      proposedRate: [0, [Validators.required, Validators.min(0.01)]]
    });
  }

  ngOnInit(): void {
    const uid = this.auth.getCurrentAuthUser()?.userId;
    const rawSkills = uid != null ? localStorage.getItem(`jb_skills_${uid}`) : null;
    if (rawSkills) {
      try {
        const parsed = JSON.parse(rawSkills);
        this.mySkills = Array.isArray(parsed) ? parsed.map((s) => String(s)) : [];
      } catch {
        this.mySkills = [];
      }
    }

    const qJob = this.route.snapshot.queryParamMap.get('job');
    if (qJob) {
      const n = Number(qJob);
      if (Number.isFinite(n)) {
        this.pendingSelectJobId = n;
      }
    }
    this.route.queryParamMap.subscribe((pm) => {
      const j = pm.get('job');
      const n = j ? Number(j) : NaN;
      if (Number.isFinite(n)) {
        this.pendingSelectJobId = n;
        this.trySelectPendingJob();
      }
    });

    const role = this.auth.getCurrentAuthUser()?.role?.toUpperCase();
    if (role === 'FREELANCER') {
      this.loadMyApplications();
    }
    this.jobBoard.getMarketInsights().subscribe({
      next: (rows) => {
        this.marketSignals = (rows || []).slice(0, 3).map((r) => ({
          label: r.skill,
          value: `${r.count} roles`,
          trend: r.trend === 'RISING' ? 'up' : r.trend === 'DECLINING' ? 'neutral' : 'neutral'
        }));
        this.heroStats[2].value = rows?.length ? `${rows.length} skills tracked` : '—';
      },
      error: () => {
        this.marketSignals = [];
        this.heroStats[2].value = '—';
      }
    });
    this.filterSub = this.filter$.pipe(debounceTime(400)).subscribe(() => this.loadPage(0));
    this.loadPage(0);
  }

  loadMyApplications(): void {
    this.myApplicationsError = null;
    this.jobBoard.getMyApplications().subscribe({
      next: (a) => {
        this.myApplications = a;
        this.appliedJobIds = new Set(a.map((x) => x.jobOfferId));
      },
      error: () => {
        this.myApplications = [];
        this.appliedJobIds = new Set();
        this.myApplicationsError = 'Unable to load your applications.';
      }
    });
  }

  retryLoadJobs(): void {
    this.loadPage(this.pageIndex);
  }

  ngOnDestroy(): void {
    this.filterSub?.unsubscribe();
  }

  onFilterChange(): void {
    this.filter$.next();
  }

  loadPage(idx: number): void {
    this.pageIndex = idx;
    this.apiLoading = true;
    this.apiError = null;
    const skills = this.filterSkill
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
    const f: JobFilters = {
      category: this.filterCategory.trim() || undefined,
      skills: skills.length ? skills : undefined,
      remote: this.filterRemoteOnly ? true : undefined
    };
    this.jobBoard.getJobs(f, this.pageIndex, this.pageSize).subscribe({
      next: (p) => {
        this.page = p;
        this.rebuildCategoryFilters(p.content);
        this.enrichAndSet(p.content);
      },
      error: () => {
        this.apiLoading = false;
        this.apiError = 'Unable to load jobs. Please check your connection.';
      }
    });
  }

  private rebuildCategoryFilters(offers: JobOffer[]): void {
    const set = new Set<string>(['All']);
    for (const o of offers) {
      if (o.category?.trim()) {
        set.add(o.category.trim());
      }
    }
    this.categoryFilters = [...set];
  }

  private enrichAndSet(offers: JobOffer[]): void {
    const uid = this.auth.getCurrentAuthUser()?.userId;
    const role = this.auth.getCurrentAuthUser()?.role?.toUpperCase();
    if (!offers.length) {
      this.feedJobs = [];
      this.apiLoading = false;
      this.syncHeroStats();
      this.trySelectPendingJob();
      return;
    }
    const rec$ =
      uid != null
        ? this.jobBoard.getRecommendations(uid, []).pipe(catchError(() => of([] as RecommendationRow[])))
        : of([] as RecommendationRow[]);

    rec$.subscribe((recs) => {
      const recMap = new Map(recs.map((r) => [r.jobOfferId, r.matchScore]));
      this.feedJobs = offers.map((j) => this.mapOffer(j, uid != null ? recMap.get(j.id) ?? null : null));
      this.finishLoad();
    });
  }

  private finishLoad(): void {
    this.apiLoading = false;
    this.syncHeroStats();
    if (!this.feedJobs.length) {
      this.selectedJob = null;
      this.clearDetail();
      return;
    }
    this.trySelectPendingJob();
    if (!this.selectedJob) {
      this.selectJob(this.feedJobs[0], false);
    }
  }

  private trySelectPendingJob(): void {
    if (this.pendingSelectJobId == null) {
      return;
    }
    const found = this.feedJobs.find((j) => j.id === this.pendingSelectJobId);
    if (found) {
      this.selectJob(found, false);
      this.pendingSelectJobId = null;
      return;
    }
    // Deep link may point to a job not present in the current page.
    // Load it explicitly so Apply works for direct URL navigation.
    if (!this.apiLoading) {
      const id = this.pendingSelectJobId;
      this.pendingSelectJobId = null;
      this.detailLoading = true;
      this.jobBoard.getJobById(id).subscribe({
        next: (detail) => {
          const view = this.mapOffer(detail, null);
          this.selectJob(view, false);
          // selectJob loads detail again; keep current detail to avoid flicker
          this.selectedDetail = detail;
          this.detailLoading = false;
        },
        error: () => {
          this.detailLoading = false;
        }
      });
    }
  }

  private syncHeroStats(): void {
    const n = this.page?.totalElements ?? this.feedJobs.length;
    this.heroStats[0].value = String(n);
    const scores = this.feedJobs.map((j) => j.matchScore).filter((x): x is number => x != null);
    if (scores.length) {
      const avg = Math.round(scores.reduce((a, b) => a + b, 0) / scores.length);
      this.heroStats[1].value = `${avg}%`;
    } else {
      this.heroStats[1].value = '—';
    }
  }

  private mapOffer(job: JobOffer, match: number | null): FeedJobView {
    const initials = this.initialsFrom(job.category || job.title);
    const loc = job.location?.trim() || (job.remote ? 'Remote' : 'Location TBD');
    const st = this.cardStatus(job);
    return {
      id: job.id,
      title: job.title,
      category: job.category,
      initials,
      location: loc,
      isRemote: job.remote,
      description: job.description || '',
      requiredSkills: job.requiredSkills || [],
      extractedSkills: job.extractedSkills || [],
      highlights: this.getHighlights(job),
      budgetMin: job.budgetMin,
      budgetMax: job.budgetMax,
      duration: job.durationDays != null ? `${job.durationDays} days` : 'Flexible',
      experienceLabel: '—',
      applicationCount: job.applicationCount ?? 0,
      publishedAt: job.publishedAt,
      opportunityScore: job.opportunityScore ?? 0,
      fraudRiskScore: job.fraudRiskScore ?? 0,
      statusLabel: st.label,
      statusClass: st.css,
      matchScore: match,
      verified: (job.fraudRiskScore ?? 0) < 0.35,
      raw: job
    };
  }

  private initialsFrom(text: string): string {
    const t = text.replace(/[^a-zA-Z0-9]/g, '').slice(0, 2);
    return t ? t.toUpperCase() : '—';
  }

  getHighlights(job: JobOffer): string[] {
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

  getHighlightDescription(label: string): string {
    const map: Record<string, string> = {
      'High-budget client': 'This job offers above-average compensation.',
      'Remote-friendly': 'Work from anywhere — no relocation required.',
      'High opportunity score': 'AI rates this as a strong market opportunity.',
      'Low competition': 'Few proposals so far — good time to apply.',
      'Skill-rich mission': 'Diverse skill requirements for portfolio growth.',
      'Fixed-scope mission': 'Well-defined deliverables and clear expectations.'
    };
    return map[label] ?? '';
  }

  private cardStatus(job: JobOffer): { label: CardStatusLabel; css: string } {
    if (job.status === 'FLAGGED') {
      return { label: 'Flagged', css: 'status-flagged' };
    }
    if (job.status !== 'PUBLISHED') {
      return { label: 'Recommended', css: 'status-recommended' };
    }
    const pub = job.publishedAt ? new Date(job.publishedAt) : null;
    const now = new Date();
    const hours = pub ? (now.getTime() - pub.getTime()) / 36e5 : 999;
    if ((job.opportunityScore ?? 0) > 75) {
      return { label: 'Featured', css: 'status-featured' };
    }
    if (pub && pub.toDateString() === now.toDateString()) {
      return { label: 'Hot', css: 'status-hot' };
    }
    if (hours < 48) {
      return { label: 'New', css: 'status-new' };
    }
    return { label: 'Recommended', css: 'status-recommended' };
  }

  get displayJobs(): FeedJobView[] {
    let list = this.feedJobs;
    if (this.featuredOnly) {
      list = list.filter((j) => j.statusLabel === 'Featured' || (j.raw.opportunityScore ?? 0) > 75);
    }
    if (this.selectedCategory && this.selectedCategory !== 'All') {
      list = list.filter((j) => j.category === this.selectedCategory);
    }
    
    list = [...list];
    if (this.sortFilter === 'MATCH') {
      list.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0));
    } else if (this.sortFilter === 'PAY') {
      list.sort((a, b) => b.budgetMax - a.budgetMax);
    } else {
      list.sort((a, b) => b.id - a.id);
    }
    
    return list;
  }

  get featuredCount(): number {
    return this.displayJobs.filter((j) => j.statusLabel === 'Featured' || (j.raw.opportunityScore ?? 0) > 75).length;
  }

  get verifiedCount(): number {
    return this.displayJobs.filter((j) => j.verified).length;
  }

  onCategoryChange(ev: Event): void {
    const v = (ev.target as HTMLSelectElement).value;
    this.filterCategory = v;
    this.selectedCategory = v ? v : 'All';
    this.loadPage(0);
  }

  onSkillSearch(ev: Event): void {
    this.filterSkill = (ev.target as HTMLInputElement).value;
    this.onFilterChange();
  }

  onRemoteToggle(ev: Event): void {
    this.filterRemoteOnly = (ev.target as HTMLInputElement).checked;
    this.loadPage(0);
  }

  selectCategory(cat: string): void {
    this.selectedCategory = cat;
  }

  selectLevel(level: string): void {
    this.selectedLevel = level;
  }

  toggleFeaturedOnly(): void {
    this.featuredOnly = !this.featuredOnly;
  }

  selectJob(job: FeedJobView, updateUrl = true): void {
    this.selectedJob = job;
    this.detailLoading = true;
    this.panelMatchScore = job.matchScore;
    this.prediction = null;
    this.applyFormOpen = false;
    this.submitError = null;
    this.submitSuccess = false;
    this.livePrediction = null;
    const uid = this.auth.getCurrentAuthUser()?.userId;
    if (updateUrl) {
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { job: job.id },
        queryParamsHandling: 'merge',
        replaceUrl: true
      });
    }
    this.jobBoard.getJobById(job.id).subscribe({
      next: (detail) => {
        this.selectedDetail = detail;
        this.detailLoading = false;
        if (this.auth.getCurrentAuthUser()?.role?.toUpperCase() === 'FREELANCER' && uid != null) {
          this.panelMatchScore = job.matchScore ?? null;
          this.jobBoard
            .postSuccessPrediction({ jobOfferId: job.id, freelancerId: uid, freelancerSkills: this.mySkills })
            .pipe(catchError(() => of<SuccessPrediction | null>(null)))
            .subscribe((p) => {
              this.prediction = p;
            });
        }
      },
      error: () => {
        this.detailLoading = false;
      }
    });
  }

  clearDetail(): void {
    this.selectedDetail = null;
    this.prediction = null;
    this.panelMatchScore = null;
  }

  hasApplied(): boolean {
    const id = this.selectedJob?.id ?? this.selectedDetail?.id;
    return id != null ? this.appliedJobIds.has(id) : false;
  }

  get existingApplication(): JobApplication | undefined {
    const id = this.selectedJob?.id ?? this.selectedDetail?.id;
    if (id == null) {
      return undefined;
    }
    return this.myApplications.find((a) => a.jobOfferId === id);
  }

  openApplyForm(): void {
    this.applyFormOpen = true;
    const j = this.selectedDetail;
    if (j) {
      const mid = (j.budgetMin + j.budgetMax) / 2;
      this.applyForm.patchValue({ proposedRate: mid, coverLetter: '' });
    }
  }

  closeApplyForm(): void {
    this.applyFormOpen = false;
    this.submitError = null;
    this.showEnhancementUndo = false;
    this.previousCoverLetter = '';
  }

  generateAiCoverLetter(): void {
    const job = this.selectedDetail;
    if (!job) return;

    const user = this.auth.getCurrentAuthUser();
    const freelancerName = user?.email ? user.email.split('@')[0] : 'Pro Freelancer';

    this.generatingCoverLetter = true;
    this.showEnhancementUndo = false;
    this.previousCoverLetter = String(this.applyForm.get('coverLetter')?.value || '');
    this.applyForm.get('coverLetter')?.disable();

    this.jobBoard.generateCoverLetter({
      jobTitle: job.title,
      jobDescription: job.description,
      freelancerName: freelancerName,
      skills: this.mySkills,
      bio: '',
      pastProjects: ''
    }).subscribe({
      next: (res) => {
        this.applyForm.patchValue({ coverLetter: res.coverLetter });
        this.showEnhancementUndo = true;
        this.applyForm.get('coverLetter')?.enable();
        this.generatingCoverLetter = false;
      },
      error: () => {
        this.applyForm.get('coverLetter')?.enable();
        this.generatingCoverLetter = false;
      }
    });
  }

  undoAiEnhancement(): void {
    this.applyForm.patchValue({ coverLetter: this.previousCoverLetter || '' });
    this.showEnhancementUndo = false;
  }

  submitApplication(): void {
    const jobId = this.selectedJob?.id ?? this.selectedDetail?.id;
    if (!jobId) {
      return;
    }
    this.applyForm.markAllAsTouched();
    if (this.applyForm.invalid) {
      return;
    }
    this.submitting = true;
    this.submitError = null;
    this.submitSuccess = false;
    const v = this.applyForm.getRawValue();
    const uid = this.auth.getCurrentAuthUser()?.userId;
    this.jobBoard
      .submitApplication({
        jobOfferId: jobId,
        coverLetter: v.coverLetter,
        proposedRate: Number(v.proposedRate),
        freelancerSkills: this.mySkills
      })
      .subscribe({
        next: (app) => {
          this.submitting = false;
          this.myApplications = [app, ...this.myApplications.filter((x) => x.jobOfferId !== app.jobOfferId)];
          this.appliedJobIds.add(jobId);
          const card = this.feedJobs.find((j) => j.id === jobId);
          if (card) {
            card.applicationCount = (card.applicationCount ?? 0) + 1;
            if (card.raw) {
              card.raw.applicationCount = card.applicationCount;
            }
          }
          this.applyFormOpen = false;
          this.submitSuccess = true;
          alert('Application submitted successfully.');
          if (uid != null) {
            this.jobBoard
              .getSuccessPrediction({
                jobOfferId: jobId,
                freelancerId: uid,
                freelancerSkills: this.mySkills
              })
              .subscribe({
                next: (pred) => {
                  this.livePrediction = pred;
                },
                error: () => {}
              });
          }
          this.jobBoard.getMyApplications().subscribe({
            next: (apps) => {
              this.myApplications = apps;
              this.appliedJobIds = new Set(apps.map((x) => x.jobOfferId));
            },
            error: () => {}
          });
        },
        error: (err: { status?: number; error?: { message?: string }; userMessage?: string }) => {
          if (err.status === 200 || err.status === 201) {
            this.submitting = false;
            const vLocal = this.applyForm.getRawValue();
            const localApp = this.createLocalApplicationRecord(jobId, String(vLocal.coverLetter || ''), Number(vLocal.proposedRate || 0));
            if (localApp) {
              this.myApplications = [localApp, ...this.myApplications.filter((x) => x.jobOfferId !== jobId)];
            }
            this.appliedJobIds.add(jobId);
            const card = this.feedJobs.find((j) => j.id === jobId);
            if (card) {
              card.applicationCount = (card.applicationCount ?? 0) + 1;
              if (card.raw) {
                card.raw.applicationCount = card.applicationCount;
              }
            }
            this.applyFormOpen = false;
            this.submitSuccess = true;
            alert('Application submitted successfully.');
            this.jobBoard.getMyApplications().subscribe({
              next: (apps) => {
                this.myApplications = apps;
                this.appliedJobIds = new Set(apps.map((x) => x.jobOfferId));
              },
              error: () => {}
            });
            return;
          }
          this.submitting = false;
          if (err.status === 409) {
            this.submitError = 'You have already applied to this job.';
            if (this.selectedJob) {
              this.appliedJobIds.add(this.selectedJob.id);
            }
          } else if (err.status === 403) {
            this.submitError = 'Only freelancers can apply to jobs.';
          } else if (err.status === 400) {
            this.submitError =
              err.error?.message ?? 'Please check your application details.';
          } else if (err.status === 503) {
            this.submitError =
              err.userMessage ??
              'User directory service is unavailable. Ensure the account service is running.';
          } else {
            this.submitError = err.userMessage ?? 'Something went wrong. Please try again.';
          }
        }
      });
  }

  private createLocalApplicationRecord(jobId: number, coverLetter: string, proposedRate: number): JobApplication | null {
    const selected = this.selectedDetail;
    if (!selected) return null;
    return {
      id: Date.now(),
      jobOfferId: jobId,
      jobTitle: selected.title,
      jobStatus: selected.status,
      freelancerId: this.auth.getCurrentAuthUser()?.userId || 0,
      coverLetter,
      proposedRate,
      status: 'PENDING',
      appliedAt: new Date().toISOString(),
      matchScore: null,
      successProbability: null,
      predictionConfidence: null,
    };
  }

  addSkill(ev: Event): void {
    ev.preventDefault();
    const e = ev.target as HTMLInputElement;
    const v = e.value.trim();
    if (!v) {
      return;
    }
    if (!this.mySkills.includes(v)) {
      this.mySkills = [...this.mySkills, v];
    }
    e.value = '';
  }

  removeSkill(s: string): void {
    this.mySkills = this.mySkills.filter((x) => x !== s);
  }

  fraudLabel(): string {
    const s = this.selectedDetail?.fraudRiskScore ?? 0;
    if (s < 0.3) {
      return 'Verified — low risk';
    }
    if (s <= 0.6) {
      return 'Moderate risk';
    }
    return 'High risk — review';
  }

  fraudShieldClass(): string {
    const s = this.selectedDetail?.fraudRiskScore ?? 0;
    if (s < 0.3) {
      return 'fraud-shield fraud-shield--ok';
    }
    if (s <= 0.6) {
      return 'fraud-shield fraud-shield--warn';
    }
    return 'fraud-shield fraud-shield--bad';
  }

  fraudShieldLabel(): string {
    return this.fraudLabel();
  }

  fraudSignalsView(): { signalName: string; signalWeight: number }[] {
    const raw = this.selectedDetail?.fraudSignals || [];
    return raw.map((sig: FraudSignalDto) => ({
      signalName: sig.message || sig.code,
      signalWeight: sig.weight
    }));
  }

  opportunityLabel(): string {
    const o = this.selectedDetail?.opportunityScore ?? 0;
    if (o >= 75) {
      return 'Excellent opportunity';
    }
    if (o >= 50) {
      return 'Solid opportunity';
    }
    return 'Average opportunity';
  }

  highlightDetails(): HighlightDetail[] {
    const job = this.selectedDetail ?? this.selectedJob?.raw;
    if (!job) {
      return [];
    }
    return this.getHighlights(job).map((h) => ({
      title: h,
      description: this.getHighlightDescription(h)
    }));
  }

  get descriptionParagraphs(): string[] {
    return (this.selectedDetail?.description ?? '')
      .split('\n')
      .map((p) => p.trim())
      .filter((p) => p.length > 0);
  }

  isFreelancer(): boolean {
    return this.auth.getCurrentAuthUser()?.role?.toUpperCase() === 'FREELANCER';
  }

  toggleSave(jobId: number): void {
    if (this.savedJobs.has(jobId)) {
      this.savedJobs.delete(jobId);
    } else {
      this.savedJobs.add(jobId);
    }
    this.savedJobs = new Set(this.savedJobs);
  }

  isSaved(jobId: number): boolean {
    return this.savedJobs.has(jobId);
  }

  saveJob(): void {
    if (this.selectedJob) {
      this.toggleSave(this.selectedJob.id);
    }
  }

  scrollToFeed(): void {
    document.querySelector('.jobs-layout')?.scrollIntoView({ behavior: 'smooth' });
  }

  goPage(delta: number): void {
    const max = this.page?.totalPages ?? 1;
    const next = Math.min(max - 1, Math.max(0, this.pageIndex + delta));
    if (next !== this.pageIndex) {
      this.loadPage(next);
    }
  }

  trackByJobId(_i: number, item: FeedJobView): number {
    return item.id;
  }

  trackByStat(_index: number, item: OpportunityStat): string {
    return item.label;
  }

  trackBySignal(_index: number, item: MarketSignal): string {
    return item.label;
  }

  trackByText(index: number, item: string): string {
    return `${index}-${item}`;
  }

  trackBySkill(_i: number, item: string): string {
    return item;
  }
}
