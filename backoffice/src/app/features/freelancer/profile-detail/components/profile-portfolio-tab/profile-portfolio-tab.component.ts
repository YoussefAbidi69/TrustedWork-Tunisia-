import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PortfolioItem } from '../../../../../core/models/freelancer.model';

@Component({
  selector: 'app-profile-portfolio-tab',
  templateUrl: './profile-portfolio-tab.component.html',
  styleUrls: ['./profile-portfolio-tab.component.css']
})
export class ProfilePortfolioTabComponent {
  @Input() portfolio: PortfolioItem[] = [];
  @Input() pinnedPortfolio: PortfolioItem[] = [];
  @Input() regularPortfolio: PortfolioItem[] = [];
  @Input() pinnedPortfolioCount = 0;

  @Output() pin = new EventEmitter<number>();
  @Output() unpin = new EventEmitter<number>();
  @Output() delete = new EventEmitter<number>();

  getTechArray(technologies: string | undefined | null): string[] {
    if (!technologies) return [];
    return technologies
      .split(',')
      .map(t => t.trim())
      .filter(Boolean);
  }

  getProjectScoreClass(score: number | undefined | null): string {
    const value = score ?? 0;
    if (value >= 80) return 'badge-success';
    if (value >= 60) return 'badge-warning';
    return 'badge-danger';
  }

  getProjectScoreLabel(score: number | undefined | null): string {
    const value = score ?? 0;
    if (value >= 100) return 'Excellent';
    if (value >= 80) return 'Très bon';
    if (value >= 60) return 'Bon';
    if (value >= 40) return 'Moyen';
    return 'À compléter';
  }
}