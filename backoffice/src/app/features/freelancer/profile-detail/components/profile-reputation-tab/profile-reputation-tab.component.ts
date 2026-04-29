import { Component, Input } from '@angular/core';
import { ProfileReview } from '../../../../../core/models/freelancer.model';

interface ReviewViewModel extends ProfileReview {
  clientFullName: string;
  clientInitials: string;
}

@Component({
  selector: 'app-profile-reputation-tab',
  templateUrl: './profile-reputation-tab.component.html',
  styleUrls: ['./profile-reputation-tab.component.css']
})
export class ProfileReputationTabComponent {
  @Input() reviews: ReviewViewModel[] = [];
  @Input() averageRating = 0;

  getStars(rating: number): string[] {
    const stars: string[] = [];
    for (let i = 1; i <= 5; i++) {
      if (i <= Math.floor(rating)) stars.push('fas fa-star');
      else if (i - rating < 1) stars.push('fas fa-star-half-alt');
      else stars.push('far fa-star');
    }
    return stars;
  }

  getRatingLabel(rating: number): string {
    if (rating >= 4.5) return 'Excellent';
    if (rating >= 4) return 'Très bon';
    if (rating >= 3) return 'Bon';
    if (rating >= 2) return 'Moyen';
    return 'Faible';
  }
}