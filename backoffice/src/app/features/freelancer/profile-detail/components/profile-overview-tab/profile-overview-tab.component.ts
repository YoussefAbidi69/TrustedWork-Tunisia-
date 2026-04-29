import { Component, Input } from '@angular/core';
import {
  CompletenessResponse,
  FreelancerProfile,
  ProfileReview
} from '../../../../../core/models/freelancer.model';

@Component({
  selector: 'app-profile-overview-tab',
  templateUrl: './profile-overview-tab.component.html',
  styleUrls: ['./profile-overview-tab.component.css']
})
export class ProfileOverviewTabComponent {
  @Input() profile: FreelancerProfile | null = null;
  @Input() profileOwnerName = '';
  @Input() averageRating = 0;
  @Input() reviewsCount = 0;
  @Input() skillsCount = 0;
  @Input() certificationsCount = 0;
  @Input() completeness: CompletenessResponse | null = null;

  getStars(rating: number): string[] {
    const stars: string[] = [];
    for (let i = 1; i <= 5; i++) {
      if (i <= Math.floor(rating)) stars.push('fas fa-star');
      else if (i - rating < 1) stars.push('fas fa-star-half-alt');
      else stars.push('far fa-star');
    }
    return stars;
  }

  getScoreClass(score: number): string {
    if (score >= 80) return 'text-success';
    if (score >= 50) return 'text-warning';
    return 'text-danger';
  }

  getStatusBadge(status: string): string {
    switch (status) {
      case 'AVAILABLE': return 'badge-success';
      case 'BUSY': return 'badge-warning';
      case 'ON_VACATION': return 'badge-danger';
      default: return 'badge-muted';
    }
  }

  getVisibilityLabel(value: string): string {
    switch (value) {
      case 'PUBLIC': return 'Public';
      case 'PRIVATE': return 'Privé';
      case 'CONNECTIONS_ONLY': return 'Connexions uniquement';
      default: return value || '—';
    }
  }

  getProjectTypeLabel(value: string): string {
    switch (value) {
      case 'SHORT_TERM': return 'Court terme';
      case 'LONG_TERM': return 'Long terme';
      case 'BOTH': return 'Les deux';
      default: return value || '—';
    }
  }
}