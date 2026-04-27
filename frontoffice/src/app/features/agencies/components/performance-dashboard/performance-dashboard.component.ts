import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { AgencyService } from '../../services/agency.service';
import { AuthService } from '../../../../core/services/auth.service';
import { AgencyAnalytics } from '../../../../core/models/agency.model';

@Component({
  selector: 'app-performance-dashboard',
  template: `
    <div class="analytics-container" *ngIf="!loading; else loadingState">
      
      <!-- KPI CARDS -->
      <div class="kpi-grid">
        <div class="kpi-card terracotta fade-in">
          <div class="kpi-icon"><i class="fas fa-tasks"></i></div>
          <div class="kpi-content">
            <span class="kpi-label">Total des tâches</span>
            <h2 class="kpi-value">{{ analytics?.totalTasks || 0 }}</h2>
          </div>
        </div>

        <div class="kpi-card success fade-in">
          <div class="kpi-icon"><i class="fas fa-check-double"></i></div>
          <div class="kpi-content">
            <span class="kpi-label">Tâches terminées</span>
            <h2 class="kpi-value">{{ analytics?.completedTasks || 0 }}</h2>
          </div>
        </div>

        <div class="kpi-card danger fade-in">
          <div class="kpi-icon"><i class="fas fa-times-circle"></i></div>
          <div class="kpi-content">
            <span class="kpi-label">Tâches annulées</span>
            <h2 class="kpi-value">{{ analytics?.cancelledTasks || 0 }}</h2>
          </div>
        </div>

        <div class="kpi-card warning fade-in">
          <div class="kpi-icon"><i class="fas fa-stopwatch"></i></div>
          <div class="kpi-content">
            <span class="kpi-label">Délai moyen (jours)</span>
            <h2 class="kpi-value">{{ (analytics?.averageTaskDays || 0) | number:'1.1-1' }}</h2>
          </div>
        </div>
      </div>

      <div class="analytics-main-grid">
        <!-- RANKING SECTION -->
        <div class="ranking-card fade-in">
          <div class="card-header">
            <h3>Top 10 Membres</h3>
          </div>
          <div class="table-scroll">
            <table class="ranking-table">
              <thead>
                <tr>
                  <th>Rang</th>
                  <th>Membre</th>
                  <th>Complétion Moy.</th>
                  <th>Terminées</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let m of analytics?.topMembers; let i = index">
                  <td class="rank-col">
                    <span class="rank-badge" [class.top3]="i < 3">{{ i + 1 }}</span>
                  </td>
                  <td class="member-col">
                    <div class="member-info">
                      <div class="avatar">{{ m.fullName.charAt(0) }}</div>
                      <span>{{ m.fullName }}</span>
                    </div>
                  </td>
                  <td>
                    <div class="score-progress">
                        <div class="progress-bg">
                            <div class="progress-bar" [style.width.%]="m.averageCompletionScore"></div>
                        </div>
                        <span class="score-text">{{ m.averageCompletionScore | number:'1.0-0' }}%</span>
                    </div>
                  </td>
                  <td class="count-col text-center">{{ m.completedTaskCount }}</td>
                </tr>
                <tr *ngIf="!analytics?.topMembers?.length">
                  <td colspan="4" class="empty-row">Aucun classement disponible</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

    </div>

    <ng-template #loadingState>
      <div class="loading-box">
        <div class="spinner"></div>
        <p>Génération des statistiques...</p>
      </div>
    </ng-template>
  `,
  styles: [`
    .analytics-container { padding: 1rem 0; display: flex; flex-direction: column; gap: 2rem; }
    
    /* KPI CARDS */
    .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; }
    .kpi-card { 
      background: white; padding: 1.5rem; border-radius: 16px; display: flex; align-items: center; gap: 1.25rem;
      box-shadow: 0 4px 15px rgba(0,0,0,0.04); transition: transform 0.3s ease; border-left: 5px solid #eee;
    }
    .kpi-card:hover { transform: translateY(-5px); }
    .kpi-icon { width: 50px; height: 50px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 1.5rem; }
    
    .terracotta { border-left-color: #e06666; } .terracotta .kpi-icon { background: rgba(224, 102, 102, 0.1); color: #e06666; }
    .success { border-left-color: #48bb78; } .success .kpi-icon { background: rgba(72, 187, 120, 0.1); color: #48bb78; }
    .danger { border-left-color: #f56565; } .danger .kpi-icon { background: rgba(245, 101, 101, 0.1); color: #f56565; }
    .warning { border-left-color: #ecc94b; } .warning .kpi-icon { background: rgba(236, 201, 75, 0.1); color: #ecc94b; }
    
    .kpi-label { color: #888; font-size: 0.85rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .kpi-value { margin: 0; font-size: 1.75rem; font-weight: 800; color: #333; }

    /* MAIN GRID */
    .analytics-main-grid { display: grid; grid-template-columns: 1fr; gap: 1.5rem; }

    .ranking-card { 
        background: white; border-radius: 20px; padding: 1.5rem; 
        box-shadow: 0 4px 20px rgba(0,0,0,0.03); border: 1px solid #f0f0f0;
    }
    .card-header { margin-bottom: 1.5rem; }
    .card-header h3 { margin: 0; font-size: 1.25rem; font-weight: 700; color: #1a202c; }

    /* RANKING TABLE */
    .table-scroll { overflow-x: auto; }
    .ranking-table { width: 100%; border-collapse: separate; border-spacing: 0 8px; }
    .ranking-table th { text-align: left; padding: 0 1rem; color: #888; font-size: 0.85rem; font-weight: 600; padding-bottom: 1rem; }
    .ranking-table td { padding: 1rem; background: #fafafa; }
    .ranking-table tr td:first-child { border-radius: 12px 0 0 12px; }
    .ranking-table tr td:last-child { border-radius: 0 12px 12px 0; }
    
    .rank-col { width: 60px; }
    .rank-badge { width: 30px; height: 30px; border-radius: 50%; background: #eee; display: flex; align-items: center; justify-content: center; font-weight: 700; color: #666; }
    .rank-badge.top3 { background: var(--terracotta, #e06666); color: white; }

    .member-info { display: flex; align-items: center; gap: 1rem; font-weight: 600; color: #333; }
    .avatar { width: 32px; height: 32px; border-radius: 50%; background: #e2e8f0; display: flex; align-items: center; justify-content: center; font-size: 0.85rem; }

    .score-progress { display: flex; align-items: center; gap: 1rem; }
    .progress-bg { flex: 1; height: 6px; background: #eee; border-radius: 10px; overflow: hidden; }
    .progress-bar { height: 100%; background: linear-gradient(90deg, #e06666, #ff8a80); border-radius: 10px; }
    .score-text { font-size: 0.85rem; font-weight: 700; color: #e06666; width: 40px; }

    /* LOADING */
    .loading-box { padding: 4rem; text-align: center; color: #666; }
    .spinner { border: 4px solid #f3f3f3; border-top: 4px solid #e06666; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 0 auto 1rem; }
    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
    .fade-in { animation: fadeIn 0.5s ease forwards; }
  `]
})
export class PerformanceDashboardComponent implements OnInit, OnChanges {
  @Input() agencyId!: number;

  analytics: AgencyAnalytics | null = null;
  loading = true;

  constructor(
    private agencyService: AgencyService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadAnalytics();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['agencyId'] && !changes['agencyId'].firstChange) {
      this.loadAnalytics();
    }
  }

  loadAnalytics(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user || !this.agencyId) return;

    this.loading = true;
    this.agencyService.getAgencyAnalytics(this.agencyId, user.userId).subscribe({
      next: (data) => {
        this.analytics = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Analytics load error:', err);
        this.loading = false;
      }
    });
  }
}
