import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-performance-dashboard',
  template: `
    <div class="empty-state">
      <i class="fas fa-chart-line"></i>
      <h3>Tableau de Bord de Performance</h3>
      <p>Les indicateurs de performance et statistiques de l'agence (ID: {{agencyId}}) seront bientôt disponibles.</p>
    </div>
  `,
  styles: [`
    .empty-state {
      padding: 4rem 2rem;
      text-align: center;
      background: #fdfdfd;
      border: 2px dashed #eee;
      border-radius: var(--radius-lg);
      color: #999;
    }
    .empty-state i { font-size: 3rem; margin-bottom: 1rem; color: #ddd; }
    .empty-state h3 { color: #555; margin-bottom: 0.5rem; }
  `]
})
export class PerformanceDashboardComponent {
  @Input() agencyId!: number;
}
