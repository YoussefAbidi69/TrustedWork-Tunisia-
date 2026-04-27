import { Component, OnDestroy, OnInit } from '@angular/core';
import { forkJoin, interval, Subscription } from 'rxjs';
import { AdminJobBoardService } from '../../services/admin-job-board.service';
import { MarketInsightResponse, PlatformStatsDto } from '../../models/admin-job-board.models';

@Component({
  selector: 'app-admin-analytics',
  templateUrl: './admin-analytics.component.html',
  styleUrls: ['./admin-analytics.component.scss']
})
export class AdminAnalyticsComponent implements OnInit, OnDestroy {
  insights: MarketInsightResponse[] = [];
  stats: PlatformStatsDto | null = null;
  loading = false;
  error: string | null = null;
  sortColumn: 'skillName' | 'count' | 'trend' | 'changePercent' | 'lastPeriodCount' = 'count';
  sortDir: 'asc' | 'desc' = 'desc';
  lastUpdated: Date | null = null;
  refreshing = false;
  private refreshSub?: Subscription;

  constructor(private admin: AdminJobBoardService) {}

  get hottestSkill(): MarketInsightResponse | null {
    if (!this.insights.length) {
      return null;
    }
    return [...this.insights].sort((a, b) => b.count - a.count)[0];
  }

  get fastestRising(): MarketInsightResponse | null {
    const rising = this.insights.filter((i) => i.trend === 'RISING');
    if (!rising.length) {
      return null;
    }
    return [...rising].sort((a, b) => b.changePercent - a.changePercent)[0];
  }

  get tableRows(): MarketInsightResponse[] {
    const col = this.sortColumn;
    const dir = this.sortDir === 'asc' ? 1 : -1;
    return [...this.insights].sort((a, b) => {
      const va = (a as any)[col];
      const vb = (b as any)[col];
      if (va < vb) {
        return -1 * dir;
      }
      if (va > vb) {
        return 1 * dir;
      }
      return 0;
    });
  }

  get barMax(): number {
    const top = this.insights[0]?.count ?? 1;
    return Math.max(1, top);
  }

  ngOnInit(): void {
    this.load();
    this.refreshSub = interval(600000).subscribe(() => this.loadInsightsOnly());
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    forkJoin({
      ins: this.admin.getMarketAnalytics(),
      stats: this.admin.getPlatformStats()
    }).subscribe({
      next: ({ ins, stats }) => {
        this.insights = ins;
        this.stats = stats;
        this.lastUpdated = new Date();
        this.loading = false;
        if (!ins.length) {
          this.triggerRefresh();
        }
      },
      error: (e: Error) => {
        this.error = e.message;
        this.loading = false;
      }
    });
  }

  loadInsightsOnly(): void {
    this.admin.getMarketAnalytics().subscribe({
      next: (ins) => {
        this.insights = ins;
        this.lastUpdated = new Date();
      },
      error: () => {}
    });
  }

  triggerRefresh(): void {
    this.refreshing = true;
    this.admin.refreshMarketData().subscribe({
      next: () => {
        setTimeout(() => {
          this.loadInsightsOnly();
          this.admin.getPlatformStats().subscribe({
            next: (s) => (this.stats = s),
            error: () => {}
          });
          this.refreshing = false;
        }, 2000);
      },
      error: () => {
        this.refreshing = false;
      }
    });
  }

  sortBy(col: typeof this.sortColumn): void {
    if (this.sortColumn === col) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = col;
      this.sortDir = 'desc';
    }
  }

  trendBadgeClass(t: string): string {
    if (t === 'RISING') {
      return 'status-badge--success';
    }
    if (t === 'DECLINING') {
      return 'status-badge--danger';
    }
    return 'status-badge--muted';
  }

  changeDisplay(row: MarketInsightResponse): string {
    if (row.trend === 'STABLE') {
      return 'Stable';
    }
    if (row.changePercent === 0) {
      return '↑ New';
    }
    const sign = row.changePercent > 0 ? '+' : '';
    return `${sign}${row.changePercent.toFixed(1)}%`;
  }

  sortIndicator(col: typeof this.sortColumn): string {
    if (this.sortColumn !== col) {
      return '';
    }
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  changeClass(row: MarketInsightResponse): string {
    if (row.changePercent > 0) {
      return 'text-success';
    }
    if (row.changePercent < 0) {
      return 'text-danger';
    }
    return 'text-muted';
  }

  barColor(row: MarketInsightResponse): string {
    if (row.trend === 'RISING') {
      return 'var(--success)';
    }
    if (row.trend === 'DECLINING') {
      return 'var(--error)';
    }
    return 'var(--primary)';
  }

  exportCsv(): void {
    const rows = [
      ['Skill', 'Count', 'Trend', 'Change%', 'Last Period Count'],
      ...this.tableRows.map((r) => [
        r.skillName,
        String(r.count),
        r.trend,
        r.changePercent.toFixed(1),
        String(r.lastPeriodCount)
      ])
    ];
    const csv = rows.map((r) => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `market-insights-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  avgMatchClass(): string {
    const v = this.stats?.avgMatchScore ?? 0;
    if (v > 70) {
      return 'text-success';
    }
    if (v >= 40) {
      return 'text-warning';
    }
    return 'text-danger';
  }
}
