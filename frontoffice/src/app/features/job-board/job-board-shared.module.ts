import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from '../../shared/shared.module';
import { CountUpDirective } from './directives/count-up.directive';
import { SkillTagComponent } from './components/skill-tag/skill-tag.component';
import { StatusBadgeComponent } from './components/status-badge/status-badge.component';
import { FraudShieldComponent } from './components/fraud-shield/fraud-shield.component';
import { OpportunityScoreCardComponent } from './components/opportunity-score-card/opportunity-score-card.component';
import { MatchScoreBadgeComponent } from './components/match-score-badge/match-score-badge.component';
import { SuccessPredictionGaugeComponent } from './components/success-prediction-gauge/success-prediction-gauge.component';
import { MarketChartComponent } from './components/market-chart/market-chart.component';
import { CareerRoadmapComponent } from './components/career-roadmap/career-roadmap.component';
import { JobCardComponent } from './components/job-card/job-card.component';


/** Presentational job-board components (no routing) — import into other feature modules. */
@NgModule({
  declarations: [
    SkillTagComponent,
    StatusBadgeComponent,
    FraudShieldComponent,
    OpportunityScoreCardComponent,
    MatchScoreBadgeComponent,
    SuccessPredictionGaugeComponent,
    MarketChartComponent,
    CareerRoadmapComponent,
    JobCardComponent
  ],
  imports: [CommonModule, FormsModule, SharedModule, CountUpDirective],
  exports: [
    SkillTagComponent,
    StatusBadgeComponent,
    FraudShieldComponent,
    OpportunityScoreCardComponent,
    MatchScoreBadgeComponent,
    SuccessPredictionGaugeComponent,
    MarketChartComponent,
    CareerRoadmapComponent,
    JobCardComponent,
    CountUpDirective
  ]
})
export class JobBoardSharedModule {}
