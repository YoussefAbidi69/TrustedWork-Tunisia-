import { Component, Input } from '@angular/core';
import { CareerSkillSuggestion } from '../../models/job-board.models';

@Component({
  selector: 'app-career-roadmap',
  templateUrl: './career-roadmap.component.html',
  styleUrls: ['./career-roadmap.component.scss']
})
export class CareerRoadmapComponent {
  @Input() suggestions: CareerSkillSuggestion[] = [];
  @Input() totalIncomeBoost = 0;

  readonly miniCirc = 2 * Math.PI * 16;

  trendLabel(s: CareerSkillSuggestion): 'RISING' | 'STABLE' | 'DECLINING' {
    if (s.trendComponent >= 0.55) {
      return 'RISING';
    }
    if (s.trendComponent <= 0.35) {
      return 'DECLINING';
    }
    return 'STABLE';
  }

  ringDash(s: CareerSkillSuggestion): number {
    const pct = Math.min(1, Math.max(0, s.combinedScore));
    return this.miniCirc * (1 - pct);
  }
}
