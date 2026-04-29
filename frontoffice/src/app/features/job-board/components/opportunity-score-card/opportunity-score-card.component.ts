import { Component, Input, OnInit } from '@angular/core';

export interface OpportunityScoreView {
  total: number;
  budget: number | null;
  demand: number | null;
  competition: number | null;
}

@Component({
  selector: 'app-opportunity-score-card',
  templateUrl: './opportunity-score-card.component.html',
  styleUrls: ['./opportunity-score-card.component.scss']
})
export class OpportunityScoreCardComponent implements OnInit {
  @Input() opportunity: OpportunityScoreView = {
    total: 0,
    budget: null,
    demand: null,
    competition: null
  };
  @Input() size: 'full' | 'compact' = 'full';

  label = '';
  labelClass = '';
  glow = 12;

  ngOnInit(): void {
    const t = this.opportunity.total;
    if (t <= 30) {
      this.label = 'Low Opportunity';
      this.labelClass = 'muted';
      this.glow = 4;
    } else if (t <= 60) {
      this.label = 'Moderate Opportunity';
      this.labelClass = 'info';
      this.glow = 10;
    } else if (t <= 80) {
      this.label = 'Strong Opportunity';
      this.labelClass = 'success';
      this.glow = 18;
    } else {
      this.label = '🔥 Hot Opportunity';
      this.labelClass = 'hot';
      this.glow = 28;
    }
  }
}
