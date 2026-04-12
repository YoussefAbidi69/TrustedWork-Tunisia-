import { Component } from '@angular/core';

type ReviewSentiment = 'Positive' | 'Neutral' | 'Flagged';

interface ReviewItem {
  id: number;
  clientName: string;
  clientRole: string;
  projectName: string;
  rating: number;
  sentiment: ReviewSentiment;
  date: string;
  comment: string;
  verified: boolean;
}

@Component({
  selector: 'app-reviews',
  templateUrl: './reviews.component.html',
  styleUrls: ['./reviews.component.css']
})
export class ReviewsComponent {
  selectedFilter: 'All' | ReviewSentiment = 'All';
  currentPage = 1;
  readonly pageSize = 4;

  readonly stats = {
    averageRating: 4.7,
    totalReviews: 38,
    positiveRate: 89,
    verifiedRate: 94
  };

  reviews: ReviewItem[] = [
    {
      id: 1,
      clientName: 'Sarah Ben Amor',
      clientRole: 'Startup Founder',
      projectName: 'Marketplace MVP',
      rating: 5,
      sentiment: 'Positive',
      date: '12 Mar 2026',
      comment: 'Excellent communication, strong ownership, and very clean execution. The project was delivered with a real product mindset.',
      verified: true
    },
    {
      id: 2,
      clientName: 'Karim Trabelsi',
      clientRole: 'Product Manager',
      projectName: 'Cloud Migration',
      rating: 5,
      sentiment: 'Positive',
      date: '28 Feb 2026',
      comment: 'Very professional collaboration. The architecture decisions were clear and the delivery quality was premium from start to finish.',
      verified: true
    },
    {
      id: 3,
      clientName: 'Ines Jlassi',
      clientRole: 'Operations Lead',
      projectName: 'Internal Dashboard',
      rating: 4,
      sentiment: 'Neutral',
      date: '09 Feb 2026',
      comment: 'The result was solid and reliable. Some refinements were needed on the UX side, but the final delivery matched expectations well.',
      verified: true
    },
    {
      id: 4,
      clientName: 'Youssef Gharbi',
      clientRole: 'CEO',
      projectName: 'SaaS Platform',
      rating: 5,
      sentiment: 'Positive',
      date: '22 Jan 2026',
      comment: 'One of the best freelance experiences we had. Structured process, excellent engineering quality, and strong follow-up.',
      verified: true
    },
    {
      id: 5,
      clientName: 'Nour Haddad',
      clientRole: 'Tech Lead',
      projectName: 'API Refactor',
      rating: 3,
      sentiment: 'Flagged',
      date: '10 Jan 2026',
      comment: 'Good technical result overall, but the scope clarification at kickoff could have been more precise.',
      verified: false
    },
    {
      id: 6,
      clientName: 'Amine Zoghlami',
      clientRole: 'Founder',
      projectName: 'Freelance Platform',
      rating: 5,
      sentiment: 'Positive',
      date: '02 Jan 2026',
      comment: 'Excellent ownership and strong technical leadership. I would definitely collaborate again on future product work.',
      verified: true
    },
    {
      id: 7,
      clientName: 'Meriem Khelifi',
      clientRole: 'Marketing Director',
      projectName: 'Landing Page Redesign',
      rating: 4,
      sentiment: 'Positive',
      date: '18 Dec 2025',
      comment: 'Very clean work and great responsiveness. The visual quality and implementation speed were impressive.',
      verified: true
    },
    {
      id: 8,
      clientName: 'Walid Ben Salem',
      clientRole: 'CTO',
      projectName: 'DevOps Automation',
      rating: 4,
      sentiment: 'Neutral',
      date: '03 Dec 2025',
      comment: 'Strong technical skills and reliable output. A bit more business-oriented communication would make the experience even better.',
      verified: true
    }
  ];

  get filteredReviews(): ReviewItem[] {
    if (this.selectedFilter === 'All') {
      return this.reviews;
    }

    return this.reviews.filter(review => review.sentiment === this.selectedFilter);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredReviews.length / this.pageSize));
  }

  get paginatedReviews(): ReviewItem[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredReviews.slice(start, start + this.pageSize);
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, index) => index + 1);
  }

  setFilter(filter: 'All' | ReviewSentiment): void {
    this.selectedFilter = filter;
    this.currentPage = 1;
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) {
      return;
    }

    this.currentPage = page;
  }

  getStars(rating: number): number[] {
    return Array.from({ length: rating }, (_, index) => index);
  }

  getEmptyStars(rating: number): number[] {
    return Array.from({ length: 5 - rating }, (_, index) => index);
  }

  getSentimentClass(sentiment: ReviewSentiment): string {
    switch (sentiment) {
      case 'Positive':
        return 'sentiment-positive';
      case 'Neutral':
        return 'sentiment-neutral';
      case 'Flagged':
        return 'sentiment-flagged';
      default:
        return '';
    }
  }

  trackByReviewId(index: number, review: ReviewItem): number {
    return review.id;
  }
}