import { Component, EventEmitter, Input, Output } from '@angular/core';
import { JobOffer } from '../../models/job-board.models';
import { OpportunityScoreView } from '../opportunity-score-card/opportunity-score-card.component';

@Component({
  selector: 'app-job-card',
  templateUrl: './job-card.component.html',
  styleUrls: ['./job-card.component.scss']
})
export class JobCardComponent {
  @Input({ required: true }) job!: JobOffer;
  @Input() showFraudShield = true;
  @Input() showOpportunityScore = true;
  @Output() cardClick = new EventEmitter<JobOffer>();

  opportunityView(job: JobOffer): OpportunityScoreView {
    return {
      total: job.opportunityScore,
      budget: job.opportunityBudgetComponent,
      demand: job.opportunityDemandComponent,
      competition: job.opportunityCompetitionComponent
    };
  }

  skillsPreview(job: JobOffer): string[] {
    const merged = [...(job.requiredSkills || []), ...(job.extractedSkills || [])];
    return merged.filter((s, i) => merged.indexOf(s) === i).slice(0, 6);
  }

  emit(): void {
    this.cardClick.emit(this.job);
  }
}
