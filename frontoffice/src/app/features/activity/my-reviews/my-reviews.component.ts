import { Component } from '@angular/core';

type ReviewStatus = 'Published' | 'Pending' | 'Needs response';

interface ReviewItem {
  id: number;
  client: string;
  mission: string;
  rating: number;
  date: string;
  status: ReviewStatus;
  comment: string;
  tags: string[];
}

@Component({
  selector: 'app-my-reviews',
  templateUrl: './my-reviews.component.html',
  styleUrls: ['./my-reviews.component.css']
})
export class MyReviewsComponent {
  isLoading = false;

  stats = [
    { label: 'Total reviews', value: '18', helper: 'Collected from verified missions' },
    { label: 'Average rating', value: '4.8/5', helper: 'Excellent client satisfaction' },
    { label: 'Response rate', value: '92%', helper: 'Fast and professional follow-up' }
  ];

  reviews: ReviewItem[] = [
    {
      id: 1,
      client: 'Nexora Studio',
      mission: 'Angular dashboard redesign',
      rating: 5,
      date: '12 Apr 2026',
      status: 'Published',
      comment:
        'Excellent collaboration. Very strong UI decisions, clean delivery and proactive communication during the whole sprint.',
      tags: ['UI/UX', 'Angular', 'Communication']
    },
    {
      id: 2,
      client: 'Atlas CloudOps',
      mission: 'Spring Boot integration support',
      rating: 4,
      date: '08 Apr 2026',
      status: 'Needs response',
      comment:
        'Solid technical contribution and good reliability. A short clarification round is still expected on final documentation.',
      tags: ['Backend', 'API', 'Documentation']
    },
    {
      id: 3,
      client: 'Craftlane',
      mission: 'Design system setup',
      rating: 5,
      date: '01 Apr 2026',
      status: 'Pending',
      comment:
        'Review is under platform moderation before publication due to final validation workflow.',
      tags: ['Design System', 'Product Design']
    }
  ];

  get publishedReviews(): number {
    return this.reviews.filter(review => review.status === 'Published').length;
  }

  get averageRating(): number {
    if (!this.reviews.length) {
      return 0;
    }

    const total = this.reviews.reduce((sum, review) => sum + review.rating, 0);
    return +(total / this.reviews.length).toFixed(1);
  }

  getStars(rating: number): number[] {
    return Array(rating).fill(0);
  }

  getEmptyStars(rating: number): number[] {
    return Array(5 - rating).fill(0);
  }

  getStatusClass(status: ReviewStatus): string {
    switch (status) {
      case 'Published':
        return 'status-published';
      case 'Pending':
        return 'status-pending';
      case 'Needs response':
        return 'status-response';
      default:
        return '';
    }
  }

  trackByReview(index: number, item: ReviewItem): number {
    return item.id;
  }

  trackByText(index: number, item: string): string {
    return item;
  }
}