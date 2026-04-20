import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import {
  ProfileReview,
  ProfileReviewSummary
} from '../../../core/models/freelancer.model';

@Component({
  selector: 'app-reviews',
  templateUrl: './reviews.component.html',
  styleUrls: ['./reviews.component.css']
})
export class ReviewsComponent implements OnInit {

  reviews: ProfileReview[] = [];
  reviewSummary: ProfileReviewSummary | null = null;

  averageRating = 0;
  latestRating: number | null = null;

  isLoading = false;
  isReplying = false;

  errorMessage = '';
  successMessage = '';

  activeReplyReviewId: number | null = null;
  replyDraft = '';

  canReply = true;

  profileId = 1;
  freelancerUserId = 1;

  clientNames: Record<number, string> = {};
  clientInitialsMap: Record<number, string> = {};

  constructor(private profileService: FreelancerProfileService) {}

  ngOnInit(): void {
    this.loadReviewsData();
  }

  loadReviewsData(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.profileService.getReviews(this.profileId).subscribe({
      next: (reviews) => {
        this.reviews = reviews ?? [];
        this.resolveClientNames();
        this.latestRating = this.reviews.length > 0 ? this.reviews[0].rating : null;
        this.loadAverageRating();
        this.loadReviewSummary();
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement des avis.';
        this.isLoading = false;
      }
    });
  }

  loadAverageRating(): void {
    this.profileService.getAverageRating(this.profileId).subscribe({
      next: (average) => {
        this.averageRating = average ?? 0;
      },
      error: () => {
        this.averageRating = 0;
      }
    });
  }

  loadReviewSummary(): void {
    this.profileService.getReviewSummary(this.profileId).subscribe({
      next: (summary) => {
        this.reviewSummary = summary;
        this.isLoading = false;
      },
      error: () => {
        this.reviewSummary = null;
        this.isLoading = false;
      }
    });
  }

  private resolveClientNames(): void {
    const uniqueClientIds = [...new Set(this.reviews.map(review => review.clientId))];

    uniqueClientIds.forEach(clientId => {
      if (this.clientNames[clientId]) {
        return;
      }

      this.profileService.getUserIdentity(clientId).subscribe({
        next: (user: any) => {
          const fullName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim();
          const finalName = fullName || `Client #${clientId}`;

          this.clientNames[clientId] = finalName;
          this.clientInitialsMap[clientId] = this.buildInitials(finalName);
        },
        error: () => {
          const fallbackName = `Client #${clientId}`;
          this.clientNames[clientId] = fallbackName;
          this.clientInitialsMap[clientId] = this.buildInitials(fallbackName);
        }
      });
    });
  }

  toggleReply(reviewId: number): void {
    if (this.activeReplyReviewId === reviewId) {
      this.activeReplyReviewId = null;
      this.replyDraft = '';
      return;
    }

    this.activeReplyReviewId = reviewId;
    this.replyDraft = '';
  }

  submitReply(review: ProfileReview): void {
    const reply = this.replyDraft.trim();

    if (!reply) {
      return;
    }

    this.isReplying = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.profileService.replyToReview(
      review.id,
      this.freelancerUserId,
      { reply }
    ).subscribe({
      next: (updatedReview) => {
        const index = this.reviews.findIndex(r => r.id === review.id);

        if (index !== -1) {
          this.reviews[index] = {
            ...this.reviews[index],
            freelancerReply: updatedReview.freelancerReply,
            updatedAt: updatedReview.updatedAt
          };
        }

        this.successMessage = 'Réponse publiée avec succès.';
        this.activeReplyReviewId = null;
        this.replyDraft = '';
        this.isReplying = false;
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la publication de la réponse.';
        this.isReplying = false;
      }
    });
  }

  getStarArray(rating: number): ('full' | 'empty')[] {
    const rounded = Math.round(rating);
    const stars: ('full' | 'empty')[] = [];

    for (let i = 1; i <= 5; i++) {
      stars.push(i <= rounded ? 'full' : 'empty');
    }

    return stars;
  }

  getRatingLabel(rating: number): string {
    if (rating >= 4.5) return 'Excellent';
    if (rating >= 4) return 'Très bon';
    if (rating >= 3) return 'Bon';
    if (rating >= 2) return 'Moyen';
    if (rating > 0) return 'Faible';
    return 'Aucune note';
  }

  getDistributionPercentage(count: number): number {
    const total = this.reviewSummary?.totalReviews ?? 0;

    if (total <= 0) {
      return 0;
    }

    return (count / total) * 100;
  }

  getTotalReviews(): number {
    return this.reviewSummary?.totalReviews ?? this.reviews.length;
  }

  getClientDisplayName(review: ProfileReview): string {
    return this.clientNames[review.clientId] || 'Client';
  }

  getClientInitials(review: ProfileReview): string {
    return this.clientInitialsMap[review.clientId] || 'CL';
  }

  private buildInitials(fullName: string): string {
    return fullName
      .split(' ')
      .filter(part => !!part.trim())
      .slice(0, 2)
      .map(part => part.charAt(0).toUpperCase())
      .join('');
  }

  trackByReview(index: number, review: ProfileReview): number {
    return review.id;
  }
}