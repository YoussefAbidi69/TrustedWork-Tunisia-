import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GamificationAdminService } from '../services/gamification-admin.service';
import { GrowthProfileDTO } from '../models/engagement.models';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-growth-admin',
  template: `
    <div class="page-header animate-fade-in">
      <div class="header-left">
        <span class="badge-label">Engagement System</span>
        <h1 class="page-title">Growth Profiles</h1>
        <p class="page-subtitle">User XP, Levels &amp; Engagement Analytics</p>
      </div>
      <div class="header-actions">
        <button class="btn-refresh" (click)="loadData()" [disabled]="loading">
          <i class="fas" [class.fa-sync-alt]="!loading" [class.fa-spinner]="loading" [class.fa-spin]="loading"></i>
          {{ loading ? 'Syncing...' : 'Refresh Data' }}
        </button>
      </div>
    </div>

    <div class="glow-line"></div>

    <!-- STATS GRID -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon accent"><i class="fas fa-bolt"></i></div>
        <div class="stat-label">Total XP Distributed</div>
        <div class="stat-val">{{ totalXp | number }}</div>
        <span class="stat-badge accent">Platform Wide</span>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="fas fa-users"></i></div>
        <div class="stat-label">Ranked Members</div>
        <div class="stat-val">{{ profiles.length }}</div>
        <span class="stat-badge green">Active Users</span>
      </div>
      <div class="stat-card">
        <div class="stat-icon warning"><i class="fas fa-crown"></i></div>
        <div class="stat-label">Max Level Reached</div>
        <div class="stat-val">{{ maxLevel }}</div>
        <span class="stat-badge warning">Top Tier</span>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><i class="fas fa-chart-line"></i></div>
        <div class="stat-label">Avg Engagement Score</div>
        <div class="stat-val">{{ getAvgScore() | number:'1.0-0' }}</div>
        <span class="stat-badge purple">Community Avg</span>
      </div>
    </div>

    <!-- MAIN TABLE CARD -->
    <div class="table-card animate-slide-up">
      <div class="card-head">
        <span class="card-head-title">Community Growth Overview</span>
        <span class="card-head-count">{{ profiles.length }} profiles</span>
      </div>
      <div class="table-responsive">
        <table class="growth-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Member</th>
              <th>Level &amp; XP Progress</th>
              <th>XP Points</th>
              <th>Engagement Score</th>
              <th>Churn Risk (ML)</th>
              <th>Efficiency</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let p of profiles; let i = index">
              <td>
                <span class="rank-num">{{ i + 1 }}</span>
              </td>
              <td>
                <div class="user-cell">
                  <div class="user-avatar-placeholder">{{ getUserName(p.userId) | slice:0:1 }}</div>
                  <div>
                    <div class="user-name">{{ getUserName(p.userId) }}</div>
                    <div class="user-sub">User ID #{{ p.userId }}</div>
                  </div>
                </div>
              </td>
              <td>
                <div class="level-xp-col">
                  <div class="level-row">
                    <span class="level-badge">Lvl {{ p.level }}</span>
                    <span class="xp-progress-label">
                      {{ p.xpPoints | number }} / {{ (p.xpPoints + (p.xpToNextLevel || 0)) | number }} XP
                    </span>
                  </div>
                  <div class="xp-bar-track">
                    <div class="xp-bar-fill" [style.width.%]="getXpPercent(p)"></div>
                  </div>
                  <div class="xp-next-label">
                    <i class="fas fa-arrow-up"></i>
                    {{ p.xpToNextLevel || 0 | number }} XP to next level
                  </div>
                </div>
              </td>
              <td>
                <div class="xp-chip">
                  <i class="fas fa-bolt"></i>
                  <span>{{ p.xpPoints | number }}</span>
                </div>
              </td>
              <td>
                <strong class="score-value">{{ p.engagementScore | number }} PTS</strong>
              </td>
              <td>
                <div class="risk-cell" *ngIf="churnMap.get(p.userId) as risk">
                  <span class="risk-badge" [style.background]="getRiskColor(risk)">
                    {{ risk.risk_label }}
                  </span>
                  <small class="risk-pct">{{ risk.churn_probability | number:'1.0-0' }}%</small>
                </div>
                <div *ngIf="!churnMap.has(p.userId)" class="loading-mini">
                  <i class="fas fa-spinner fa-spin"></i>
                </div>
              </td>
              <td>
                <div class="efficiency-bar-wrap">
                  <div class="efficiency-track">
                    <div class="efficiency-fill" [style.width.%]="getScorePercentage(p.engagementScore)"></div>
                  </div>
                  <span class="efficiency-pct">{{ getScorePercentage(p.engagementScore) }}%</span>
                </div>
              </td>
            </tr>
            <tr *ngIf="profiles.length === 0 && !loading">
              <td colspan="6" class="empty-row">
                <i class="fas fa-chart-line" style="opacity:0.3; margin-right:8px;"></i>
                No growth profiles found. Start engaging users to see analytics.
              </td>
            </tr>
            <tr *ngIf="loading">
              <td colspan="6" class="empty-row">
                <i class="fas fa-spinner fa-spin" style="margin-right:8px;"></i> Loading profiles...
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; padding: 20px; }

    .animate-fade-in { animation: fadeIn 0.6s ease-out; }
    .animate-slide-up { animation: slideUp 0.6s ease-out; }
    @keyframes fadeIn  { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

    .page-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; gap: 16px; }
    .header-left  { display: flex; flex-direction: column; gap: 4px; }
    .header-actions { display: flex; align-items: center; }

    .badge-label { display: inline-flex; padding: 3px 10px; border-radius: 99px; font-size: 11px; font-weight: 600; letter-spacing: 0.4px; text-transform: uppercase; background: rgba(99,102,241,0.12); color: #6366F1; margin-bottom: 4px; }
    .page-title    { font-family: 'Space Grotesk', sans-serif; font-size: 22px; font-weight: 700; color: #F1F5F9; line-height: 1.3; margin: 0; }
    .page-subtitle { font-size: 13px; color: #94A3B8; margin: 0; }

    .glow-line { height: 2px; background: linear-gradient(90deg, #6366F1, transparent); border-radius: 99px; margin-bottom: 24px; }

    .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
    .stat-card { background: #161B27; border: 1px solid rgba(255,255,255,0.06); border-radius: 16px; padding: 20px; transition: 0.2s; }
    .stat-card:hover { border-color: rgba(99,102,241,0.3); transform: translateY(-3px); box-shadow: 0 8px 30px rgba(0,0,0,0.4); }
    .stat-icon { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 16px; margin-bottom: 16px; }
    .stat-icon.accent  { background: rgba(99,102,241,0.12); color: #6366F1; }
    .stat-icon.green   { background: rgba(16,185,129,0.12); color: #10B981; }
    .stat-icon.warning { background: rgba(245,158,11,0.12);  color: #F59E0B; }
    .stat-icon.purple  { background: rgba(168,85,247,0.12);  color: #A855F7; }
    .stat-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #4B5563; margin-bottom: 6px; }
    .stat-val   { font-family: 'Space Grotesk', sans-serif; font-size: 28px; font-weight: 800; color: #F1F5F9; line-height: 1; }
    .stat-badge { display: inline-block; font-size: 10px; font-weight: 700; padding: 3px 8px; border-radius: 6px; margin-top: 10px; }
    .stat-badge.accent  { background: rgba(99,102,241,0.10); color: #6366F1; }
    .stat-badge.green   { background: rgba(16,185,129,0.10); color: #10B981; }
    .stat-badge.warning { background: rgba(245,158,11,0.10); color: #F59E0B; }
    .stat-badge.purple  { background: rgba(168,85,247,0.10); color: #A855F7; }

    .btn-refresh { display: flex; align-items: center; gap: 8px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); color: #94A3B8; padding: 9px 16px; border-radius: 10px; font-size: 13px; font-weight: 600; cursor: pointer; transition: 0.2s; font-family: inherit; }
    .btn-refresh:hover:not(:disabled) { background: rgba(255,255,255,0.08); color: #F1F5F9; }
    .btn-refresh:disabled { opacity: 0.5; cursor: not-allowed; }

    .table-card { background: #161B27; border: 1px solid rgba(255,255,255,0.06); border-radius: 20px; overflow: hidden; box-shadow: 0 8px 40px rgba(0,0,0,0.4); }
    .card-head { padding: 20px 24px; border-bottom: 1px solid rgba(255,255,255,0.05); display: flex; align-items: center; justify-content: space-between; }
    .card-head-title { font-family: 'Space Grotesk', sans-serif; font-size: 15px; font-weight: 600; color: #F1F5F9; }
    .card-head-count { background: rgba(99,102,241,0.1); color: #6366F1; padding: 4px 12px; border-radius: 99px; font-size: 11px; font-weight: 700; }

    .growth-table { width: 100%; border-collapse: collapse; font-size: 13px; }
    .growth-table thead th { padding: 12px 20px; text-align: left; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.6px; color: #4B5563; border-bottom: 1px solid rgba(255,255,255,0.05); background: rgba(255,255,255,0.01); }
    .growth-table tbody tr { border-bottom: 1px solid rgba(255,255,255,0.04); transition: 0.2s; }
    .growth-table tbody tr:last-child { border-bottom: none; }
    .growth-table tbody tr:hover { background: rgba(99,102,241,0.04); }
    .growth-table tbody td { padding: 14px 20px; color: #94A3B8; vertical-align: middle; }

    .rank-num { font-family: 'Space Grotesk', sans-serif; font-size: 13px; font-weight: 800; color: #4B5563; }

    .user-cell { display: flex; align-items: center; gap: 12px; }
    .user-avatar-placeholder { width: 36px; height: 36px; border-radius: 50%; background: rgba(99,102,241,0.2); color: #818CF8; font-weight: 800; font-size: 13px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
    .user-name { font-weight: 600; color: #F1F5F9; font-size: 14px; }
    .user-sub  { font-size: 11px; color: #4B5563; margin-top: 2px; }

    /* Level + XP Progress Column */
    .level-xp-col { display: flex; flex-direction: column; gap: 6px; min-width: 200px; }
    .level-row { display: flex; align-items: center; justify-content: space-between; }
    .level-badge { background: rgba(99,102,241,0.1); color: #818CF8; border: 1px solid rgba(99,102,241,0.2); padding: 3px 10px; border-radius: 6px; font-size: 11px; font-weight: 800; }
    .xp-progress-label { font-size: 11px; color: #4B5563; font-weight: 600; }
    .xp-bar-track { width: 100%; height: 6px; background: rgba(255,255,255,0.06); border-radius: 10px; overflow: hidden; }
    .xp-bar-fill { height: 100%; background: linear-gradient(90deg, #6366F1, #A78BFA); border-radius: 10px; transition: width 0.8s cubic-bezier(0.34, 1.56, 0.64, 1); }
    .xp-next-label { font-size: 10px; color: #4B5563; display: flex; align-items: center; gap: 4px; }
    .xp-next-label i { color: #6366F1; font-size: 8px; }

    .xp-chip { display: flex; align-items: center; gap: 6px; background: rgba(16,185,129,0.1); color: #10B981; padding: 4px 10px; border-radius: 20px; font-weight: 700; width: fit-content; }
    .xp-chip i { font-size: 10px; }

    .score-value { color: #F1F5F9; font-weight: 700; font-size: 14px; }

    .efficiency-bar-wrap { display: flex; align-items: center; gap: 10px; }
    .efficiency-track { width: 90px; height: 6px; background: rgba(255,255,255,0.06); border-radius: 10px; overflow: hidden; }
    .efficiency-fill  { height: 100%; background: linear-gradient(90deg, #6366F1, #8B5CF6); border-radius: 10px; transition: width 0.5s ease; }
    .efficiency-pct   { font-size: 12px; font-weight: 700; color: #94A3B8; min-width: 32px; }

    .risk-cell { display: flex; flex-direction: column; gap: 4px; }
    .risk-badge { font-size: 10px; font-weight: 800; color: white; padding: 2px 8px; border-radius: 4px; text-transform: uppercase; width: fit-content; }
    .risk-pct { font-size: 11px; color: #64748B; font-weight: 600; }
    .loading-mini { color: #475569; font-size: 12px; }

    .empty-row { text-align: center; padding: 60px; color: #4B5563; font-size: 14px; }
  `]
})
export class GrowthAdminComponent implements OnInit {
  profiles: GrowthProfileDTO[] = [];
  userMap: Map<number, string> = new Map();
  churnMap: Map<number, any> = new Map();
  loading = false;

  totalXp = 0;
  maxLevel = 0;
  topScore = 0;

  private USER_API = 'http://localhost:8081/api';

  constructor(
    private growthService: GamificationAdminService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.growthService.getProfiles().subscribe({
      next: (data) => {
        this.profiles = data.sort((a, b) => (b.engagementScore || 0) - (a.engagementScore || 0));
        this.calculateStats();
        this.resolveNames();
        this.fetchChurnPredictions();
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load growth profiles', err);
        this.loading = false;
      }
    });
  }

  calculateStats(): void {
    this.totalXp = this.profiles.reduce((acc, p) => acc + (p.xpPoints || 0), 0);
    this.maxLevel = Math.max(...this.profiles.map(p => p.level || 1), 0);
    this.topScore = Math.max(...this.profiles.map(p => p.engagementScore || 0), 0);
  }

  getAvgScore(): number {
    if (this.profiles.length === 0) return 0;
    const total = this.profiles.reduce((acc, p) => acc + (p.engagementScore || 0), 0);
    return total / this.profiles.length;
  }

  resolveNames(): void {
    if (this.profiles.length === 0) return;
    const requests = this.profiles.map(p =>
      this.http.get<any>(`${this.USER_API}/identity/users/${p.userId}`).pipe(
        catchError(() => of(null))
      )
    );
    forkJoin(requests).subscribe(users => {
      users.forEach((user, index) => {
        if (user) {
          this.userMap.set(this.profiles[index].userId, `${user.firstName} ${user.lastName}`);
        }
      });
    });
  }

  getUserName(userId: number): string {
    return this.userMap.get(userId) || `Member #${userId}`;
  }

  getXpPercent(p: GrowthProfileDTO): number {
    const total = (p.xpPoints || 0) + (p.xpToNextLevel || 0);
    if (total === 0) return 0;
    return Math.min(100, Math.round(((p.xpPoints || 0) / total) * 100));
  }

  getScorePercentage(score: number): number {
    if (this.topScore === 0) return 0;
    if (!score) return 0;
    return Math.min(100, Math.max(1, Math.round((score / this.topScore) * 100)));
  }

  fetchChurnPredictions(): void {
    this.profiles.forEach(p => {
      this.growthService.getChurnPrediction(p.userId).subscribe({
        next: (res) => this.churnMap.set(p.userId, res),
        error: () => this.churnMap.set(p.userId, { risk_label: 'UNKNOWN', churn_probability: 0 })
      });
    });
  }

  getRiskColor(risk: any): string {
    const colors: any = {
      'HIGH': '#ef4444',
      'MEDIUM': '#f59e0b',
      'LOW': '#10b981',
      'UNKNOWN': '#64748b'
    };
    return colors[risk.risk_label] || '#64748b';
  }
}
