import { Component, OnDestroy, OnInit } from '@angular/core';
import { forkJoin, of, Subscription, interval } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { JobBoardService } from '../../services/job-board.service';
import {
  CareerInsightResponse,
  MarketCategory,
  MarketForecast,
  MarketInsight,
  MarketOverview,
  SalaryInsight,
  TrendDirection,
} from '../../models/job-board.models';

@Component({
  selector: 'app-market-insights',
  templateUrl: './market-insights.component.html',
  styleUrls: ['./market-insights.component.scss'],
})
export class MarketInsightsComponent implements OnInit, OnDestroy {
  activeTab: 'overview' | 'skill-trends' | 'salary-intelligence' | 'demand-forecast' | 'career-opportunities' = 'overview';
  loading = true;
  animate = false;
  error: string | null = null;
  lastFetchedIso: string | null = null;

  insights: MarketInsight[] = [];
  overview: MarketOverview = { activeJobPostings: 0, avgCompetition: 0 };
  salaryInsights: SalaryInsight[] = [];
  forecastRows: MarketForecast[] = [];
  categoryRows: MarketCategory[] = [];
  trajectory: CareerInsightResponse | null = null;

  searchTerm = '';
  expandedSkill: string | null = null;
  sortState: Record<string, '' | 'asc' | 'desc'> = {};

  skillRows: MarketInsight[] = [];
  salaryRows: SalaryInsight[] = [];
  forecastViewRows: MarketForecast[] = [];

  localSkills: string[] = [];
  skillMatchRows: Array<{ skill: string; count: number; pct: number }> = [];
  isFreelancer = false;

  private refreshSub?: Subscription;

  constructor(private jobBoard: JobBoardService) {}

  ngOnInit(): void {
    this.loadDashboard();
    this.refreshSub = interval(300000).subscribe(() => this.loadDashboard(true));
    setTimeout(() => (this.animate = true), 100);
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  switchTab(tab: typeof this.activeTab): void {
    this.activeTab = tab;
  }

  refreshMarketData(): void {
    this.loadDashboard();
  }

  get hottestSkill(): MarketInsight | null {
    return [...this.insights].sort((a, b) => b.count - a.count)[0] || null;
  }

  get fastestRising(): MarketInsight | null {
    return [...this.insights]
      .filter((i) => i.trend === 'RISING')
      .sort((a, b) => (b.changePercent || 0) - (a.changePercent || 0))[0] || null;
  }

  get topBarSkills(): MarketInsight[] {
    return [...this.insights].sort((a, b) => b.count - a.count).slice(0, 20);
  }

  get risingSkills(): MarketInsight[] {
    return this.insights.filter((i) => i.trend === 'RISING').slice(0, 12);
  }

  get decliningSkills(): MarketInsight[] {
    return this.insights.filter((i) => i.trend === 'DECLINING').slice(0, 12);
  }

  get stableSkills(): MarketInsight[] {
    return this.insights.filter((i) => i.trend === 'STABLE').slice(0, 12);
  }

  get filteredSkillRows(): MarketInsight[] {
    const q = this.searchTerm.trim().toLowerCase();
    if (!q) return this.skillRows;
    return this.skillRows.filter((row) => row.skill.toLowerCase().includes(q));
  }

  sortCycle(column: string, target: 'skills' | 'salary' | 'forecast'): void {
    const current = this.sortState[column] || '';
    const next: '' | 'asc' | 'desc' = current === '' ? 'asc' : current === 'asc' ? 'desc' : '';
    this.sortState = {};
    this.sortState[column] = next;

    if (target === 'skills') this.applySkillSort();
    if (target === 'salary') this.applySalarySort();
    if (target === 'forecast') this.applyForecastSort();
  }

  toggleSkillDetails(skill: string): void {
    this.expandedSkill = this.expandedSkill === skill ? null : skill;
  }

  exportCsv(): void {
    const header = ['Skill', 'This Month', 'Last Month', 'Change', 'Trend', 'Job Count', 'Avg Budget'];
    const body = this.filteredSkillRows.map((r) => [
      r.skill,
      r.count,
      r.lastPeriodCount || 0,
      `${r.changePercent || 0}`,
      r.trend,
      r.count,
      this.avgBudgetForSkill(r.skill),
    ]);
    const csv = [header, ...body]
      .map((row) => row.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(','))
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'market-insights.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  trendClass(trend: TrendDirection): string {
    if (trend === 'RISING') return 'trend-up';
    if (trend === 'DECLINING') return 'trend-down';
    return 'trend-stable';
  }

  confidenceClass(c: string): string {
    if (c === 'HIGH') return 'badge-success';
    if (c === 'MEDIUM') return 'badge-warning';
    return 'badge-danger';
  }

  salaryBand(rate: number): string {
    if (rate > 50) return 'salary-good';
    if (rate >= 25) return 'salary-mid';
    return 'salary-low';
  }

  demandDelta(current: number, projected: number): number {
    return projected - current;
  }

  demandDeltaText(current: number, projected: number): string {
    const d = projected - current;
    return `${d > 0 ? '+' : ''}${d}`;
  }

  barWidthPct(value: number): number {
    const max = this.topBarSkills[0]?.count || 1;
    return Math.max(2, Math.round((value / max) * 100));
  }

  sparkPath(row: MarketInsight): string {
    const a = Math.max(1, row.lastPeriodCount || 0);
    const b = Math.max(1, Math.round(a + (row.changePercent || 0)));
    const c = Math.max(1, row.count);
    const max = Math.max(a, b, c);
    const y = (v: number) => 40 - Math.round((v / max) * 34);
    return `M0 ${y(a)} L50 ${y(b)} L100 ${y(c)}`;
  }

  avgBudgetForSkill(skill: string): number {
    const s = this.salaryInsights.find((x) => x.skill === skill);
    return Math.round(s?.avgProposedRate || 0);
  }

  topCategoriesForSkill(): string[] {
    return this.categoryRows.slice(0, 3).map((c) => c.category);
  }

  cooccurrenceSkills(skill: string): string[] {
    return this.topBarSkills.filter((s) => s.skill !== skill).slice(0, 3).map((s) => s.skill);
  }

  goToCategory(category: string): void {
    window.location.href = `/jobs?category=${encodeURIComponent(category)}`;
  }

  trackBySkill(_idx: number, row: MarketInsight): string {
    return row.skill;
  }

  private loadDashboard(silent = false): void {
    if (!silent) this.loading = true;
    this.error = null;
    this.isFreelancer = (localStorage.getItem('role') || '').toUpperCase() === 'FREELANCER';
    this.readLocalSkills();

    forkJoin({
      insights: this.jobBoard.getMarketInsights(),
      overview: this.jobBoard.getMarketOverview(),
      salary: this.jobBoard.getSalaryInsights(),
      forecast: this.jobBoard.getMarketForecast(),
      categories: this.jobBoard.getMarketCategories(this.localSkills),
      trajectory: this.isFreelancer
        ? this.jobBoard.getCareerTrajectory(this.localSkills).pipe(catchError(() => of(null)))
        : of(null),
    }).subscribe({
      next: (res) => {
        this.insights = res.insights || [];
        this.overview = res.overview || this.overview;
        this.salaryInsights = res.salary || [];
        this.forecastRows = res.forecast || [];
        this.categoryRows = res.categories || [];
        this.trajectory = res.trajectory as CareerInsightResponse | null;
        this.lastFetchedIso = new Date().toISOString();
        this.applySkillSort();
        this.applySalarySort();
        this.applyForecastSort();
        this.buildSkillMatch();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load market analytics dashboard.';
      },
    });
  }

  private readLocalSkills(): void {
    const userId = localStorage.getItem('userId') || localStorage.getItem('id') || '';
    const raw = localStorage.getItem(`jb_skills_${userId}`) || '[]';
    try {
      const arr = JSON.parse(raw);
      this.localSkills = Array.isArray(arr) ? arr.map((x) => String(x)) : [];
    } catch {
      this.localSkills = [];
    }
  }

  private buildSkillMatch(): void {
    const total = Math.max(1, this.overview.activeJobPostings || 1);
    this.skillMatchRows = this.localSkills
      .map((skill) => {
        const row = this.insights.find((i) => i.skill.toLowerCase() === skill.toLowerCase());
        const count = row?.count || 0;
        return { skill, count, pct: Math.round((count / total) * 100) };
      })
      .sort((a, b) => b.pct - a.pct);
  }

  private applySkillSort(): void {
    const column = Object.keys(this.sortState)[0];
    const dir = this.sortState[column];
    const rows = [...this.insights];
    if (!column || !dir) {
      this.skillRows = rows.sort((a, b) => b.count - a.count);
      return;
    }
    const f = dir === 'asc' ? 1 : -1;
    this.skillRows = rows.sort((a, b) => {
      if (column === 'skill') return a.skill.localeCompare(b.skill) * f;
      if (column === 'thisMonth') return (a.count - b.count) * f;
      if (column === 'lastMonth') return ((a.lastPeriodCount || 0) - (b.lastPeriodCount || 0)) * f;
      if (column === 'change') return ((a.changePercent || 0) - (b.changePercent || 0)) * f;
      if (column === 'jobCount') return (a.count - b.count) * f;
      if (column === 'avgBudget') return (this.avgBudgetForSkill(a.skill) - this.avgBudgetForSkill(b.skill)) * f;
      return 0;
    });
  }

  private applySalarySort(): void {
    const dir = this.sortState['salaryMedian'] || 'desc';
    const f = dir === 'asc' ? 1 : -1;
    this.salaryRows = [...this.salaryInsights].sort((a, b) => (a.medianRate - b.medianRate) * f);
  }

  private applyForecastSort(): void {
    const dir = this.sortState['forecast'] || '';
    if (!dir) {
      this.forecastViewRows = [...this.forecastRows];
      return;
    }
    const f = dir === 'asc' ? 1 : -1;
    this.forecastViewRows = [...this.forecastRows].sort((a, b) => (a.forecastIn3Months - b.forecastIn3Months) * f);
  }
}
