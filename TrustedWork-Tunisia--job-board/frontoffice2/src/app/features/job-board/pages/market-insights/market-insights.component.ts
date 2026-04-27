import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription, interval, of } from 'rxjs';
import { catchError, finalize, startWith, switchMap, take } from 'rxjs/operators';
import { JobBoardService } from '../../services/job-board.service';
import { MarketInsight, TrendDirection } from '../../models/job-board.models';
import { trigger, transition, style, animate } from '@angular/animations';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';

@Component({
  selector: 'app-market-insights',
  templateUrl: './market-insights.component.html',
  styleUrls: ['./market-insights.component.scss'],
  animations: [
    trigger('cardIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'scale(0.96)' }),
        animate('0.45s ease-out', style({ opacity: 1, transform: 'scale(1)' }))
      ])
    ])
  ]
})
export class MarketInsightsComponent implements OnInit, OnDestroy {
  loading = true;
  error: string | null = null;
  insights: MarketInsight[] = [];
  displayInsights: MarketInsight[] = [];
  hottestSkill: MarketInsight | null = null;
  fastestRising: MarketInsight | null = null;
  mostDeclining: MarketInsight | null = null;
  aiSummary: string = '';
  get lastUpdated() { return this.lastFetchedIso; }
  lastFetchedIso: string | null = null;
  sortKey: 'skill' | 'count' | 'trend' | 'changePercent' = 'count';
  sortDir: 'asc' | 'desc' = 'desc';
  insightsEmptyGenerating = false;
  /** First time we see an empty snapshot, trigger refresh + polling once per page visit. */
  private autoEmptyFlowStarted = false;
  private sub?: Subscription;
  private pollSub?: Subscription;
  private timeAgo = new TimeAgoPipe();

  constructor(private jobBoard: JobBoardService) {}

  ngOnInit(): void {
    this.sub = interval(300_000)
      .pipe(
        startWith(0),
        switchMap(() => {
          const silent = this.insights.length > 0;
          if (!silent) {
            this.loading = true;
          }
          return this.jobBoard.getMarketInsights();
        })
      )
      .subscribe({
        next: (rows) => {
          this.insights = rows;
          this.lastFetchedIso = new Date().toISOString();
          this.computeHighlights(rows);
          this.applySort();
          this.loading = false;
          this.error = null;
          if (!rows.length && !this.autoEmptyFlowStarted) {
            this.beginEmptyRefreshFlow();
          }
          if (rows.length) {
            this.insightsEmptyGenerating = false;
            this.stopPoll();
          }
        },
        error: () => {
          this.loading = false;
          this.error = 'Unable to load market intelligence.';
        }
      });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.stopPoll();
  }

  refreshMarketData(): void {
    this.error = null;
    this.loading = true;
    this.jobBoard.getMarketInsights().subscribe({
      next: (rows) => {
        this.insights = rows;
        this.lastFetchedIso = new Date().toISOString();
        this.computeHighlights(rows);
        this.applySort();
        this.generateAiSummary();
        this.loading = false;
        if (!rows.length && !this.autoEmptyFlowStarted) {
          this.beginEmptyRefreshFlow();
        }
      },
      error: () => {
        this.loading = false;
        this.error = 'Unable to load market intelligence.';
      }
    });
  }

  private beginEmptyRefreshFlow(): void {
    this.autoEmptyFlowStarted = true;
    this.insightsEmptyGenerating = true;
    this.jobBoard
      .refreshMarketInsights()
      .pipe(catchError(() => of([])))
      .subscribe(() => this.startPolling());
  }

  private startPolling(): void {
    this.stopPoll();
    this.pollSub = interval(3000)
      .pipe(
        take(20),
        switchMap(() => this.jobBoard.getMarketInsights()),
        finalize(() => {
          if (!this.insights.length) {
            this.insightsEmptyGenerating = false;
          }
        })
      )
      .subscribe((rows) => {
        if (rows.length) {
          this.insights = rows;
          this.lastFetchedIso = new Date().toISOString();
          this.computeHighlights(rows);
          this.applySort();
          this.generateAiSummary();
          this.insightsEmptyGenerating = false;
          this.stopPoll();
        }
      });
  }

  private stopPoll(): void {
    this.pollSub?.unsubscribe();
    this.pollSub = undefined;
  }

  private computeHighlights(rows: MarketInsight[]): void {
    if (!rows.length) {
      this.hottestSkill = null;
      this.fastestRising = null;
      this.mostDeclining = null;
      return;
    }
    this.hottestSkill = [...rows].sort((a, b) => b.count - a.count)[0];
    const risers = rows.filter((r) => r.trend === 'RISING');
    this.fastestRising =
      risers.sort((a, b) => (b.changePercent ?? 0) - (a.changePercent ?? 0))[0] ||
      risers.sort((a, b) => b.count - a.count)[0] ||
      this.hottestSkill;
    const fallers = rows.filter((r) => r.trend === 'DECLINING');
    this.mostDeclining =
      fallers.sort((a, b) => (a.changePercent ?? 0) - (b.changePercent ?? 0))[0] ||
      fallers.sort((a, b) => a.count - b.count)[0] ||
      fallers[0] ||
      rows[rows.length - 1];
  }

  lastUpdatedLabel(): string {
    return this.lastFetchedIso ? this.timeAgo.transform(this.lastFetchedIso) : '—';
  }

  private generateAiSummary(): void {
    if (!this.hottestSkill && !this.fastestRising) {
      this.aiSummary = 'Not enough market data is available at the moment to form a conclusive summary.';
      return;
    }
    const hottest = this.hottestSkill?.skill || 'technology';
    const rising = this.fastestRising?.skill;
    const declining = this.mostDeclining?.skill;
    
    let summary = `The freelance market is currently dominated by high demand for ${hottest}. `;
    if (rising) {
      summary += `We are observing a massive upward trend in ${rising}, presenting a highly lucrative opportunity for early adopters. `;
    }
    if (declining) {
      summary += `Conversely, demand for ${declining} is cooling down. Consider diversifying your skill set if this is your primary domain.`;
    }
    this.aiSummary = summary;
  }

  sort(key: any): void {
    if (this.sortKey === key) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDir = key === 'skill' ? 'asc' : 'desc';
    }
    this.applySort();
  }

  private applySort(): void {
    if (!this.insights.length) {
      this.displayInsights = [];
      return;
    }
    const dir = this.sortDir === 'asc' ? 1 : -1;
    const rows = [...this.insights];
    rows.sort((a, b) => {
      let cmp = 0;
      if (this.sortKey === 'skill') {
        cmp = a.skill.localeCompare(b.skill);
      } else if (this.sortKey === 'count') {
        cmp = a.count - b.count;
      } else if (this.sortKey === 'changePercent') {
        cmp = (a.changePercent ?? 0) - (b.changePercent ?? 0);
      } else {
        cmp = a.trend.localeCompare(b.trend);
      }
      return cmp * dir;
    });
    this.displayInsights = rows;
  }

  exportCsv(): void {
    // Basic CSV mock
    console.log('Exporting CSV...');
  }

  changeClass(val: number): string {
    return val > 0 ? 'val-pos' : val < 0 ? 'val-neg' : 'val-neu';
  }

  trendClass(t: TrendDirection): string {
    if (t === 'RISING') {
      return 'job-status status-featured';
    }
    if (t === 'DECLINING') {
      return 'job-status status-hot';
    }
    return 'job-status status-recommended';
  }

  changeDisplay(row: MarketInsight): string {
    const cp = row.changePercent;
    if (cp == null || Number.isNaN(cp)) {
      return '—';
    }
    if (cp === 0 && row.trend === 'STABLE') {
      return 'Stable';
    }
    if (cp === 0 && row.trend === 'RISING') {
      return '↑ New';
    }
    const sign = cp > 0 ? '+' : '';
    return `${sign}${cp.toFixed(1)}%`;
  }
}
