import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { MatchScoreBreakdown } from '../../models/job-board.models';

@Component({
  selector: 'app-match-score-badge',
  templateUrl: './match-score-badge.component.html',
  styleUrls: ['./match-score-badge.component.scss']
})
export class MatchScoreBadgeComponent implements OnChanges {
  @Input() matchScore: MatchScoreBreakdown | null = null;
  @Input() size: 'full' | 'compact' = 'full';

  total = 0;
  readonly circumference = 2 * Math.PI * 42;
  dashOffset = 2 * Math.PI * 42;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['matchScore']) {
      this.total = this.matchScore?.totalScore ?? 0;
      const pct = Math.min(100, Math.max(0, this.total)) / 100;
      this.dashOffset = this.circumference * (1 - pct);
    }
  }

  dims(): { key: string; label: string; value: number }[] {
    const m = this.matchScore;
    if (!m) {
      return [];
    }
    return [
      { key: 'sk', label: 'Skill Match', value: m.skillMatch },
      { key: 'rep', label: 'Reputation', value: m.reputation },
      { key: 'succ', label: 'Success Rate', value: m.successRate },
      { key: 'bud', label: 'Budget Fit', value: m.budgetFit },
      { key: 'av', label: 'Availability', value: m.availability }
    ];
  }

  ringColor(): string {
    if (this.total <= 40) {
      return 'var(--danger)';
    }
    if (this.total <= 70) {
      return 'var(--gold)';
    }
    return 'var(--olive)';
  }
}
