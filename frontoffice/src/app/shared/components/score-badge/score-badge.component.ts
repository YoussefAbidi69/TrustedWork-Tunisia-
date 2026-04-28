import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-score-badge',
  templateUrl: './score-badge.component.html',
  styleUrls: ['./score-badge.component.css']
})
export class ScoreBadgeComponent {
  @Input() score!: number;
  @Input() breakdown: any;

  showTooltip = false;

  get formattedScore(): number {
    return Math.round(this.score * 100);
  }

  get colorClass(): string {
    const s = this.formattedScore;
    if (s >= 80) return 'green';
    if (s >= 60) return 'blue';
    if (s >= 40) return 'yellow';
    return 'gray';
  }
}
