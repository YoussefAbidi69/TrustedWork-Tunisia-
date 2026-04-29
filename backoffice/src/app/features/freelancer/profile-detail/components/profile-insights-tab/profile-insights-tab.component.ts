import { Component, Input } from '@angular/core';
import {
  CareerPathResponse,
  CompletenessResponse,
  SkillGapRecommendation,
  SkillGapResponse
} from '../../../../../core/models/freelancer.model';

@Component({
  selector: 'app-profile-insights-tab',
  templateUrl: './profile-insights-tab.component.html',
  styleUrls: ['./profile-insights-tab.component.css']
})
export class ProfileInsightsTabComponent {
  @Input() skillGapDiagnostic: SkillGapResponse | null = null;
  @Input() skillGapRecommendations: SkillGapRecommendation | null = null;
  @Input() completeness: CompletenessResponse | null = null;
  @Input() careerPath: CareerPathResponse | null = null;

  getScoreClass(score: number): string {
    if (score >= 80) return 'text-success';
    if (score >= 50) return 'text-warning';
    return 'text-danger';
  }
}