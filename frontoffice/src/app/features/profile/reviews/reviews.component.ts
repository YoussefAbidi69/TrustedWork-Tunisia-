import { Component, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
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
  isHiding = false;

  errorMessage = '';
  successMessage = '';

  activeReplyReviewId: number | null = null;
  replyDraft = '';

  canReply = true;

  // Chargés dynamiquement depuis le profil de l'utilisateur connecté
  profileId: number | null = null;
  freelancerUserId: number | null = null;

  clientNames: Record<number, string> = {};
  clientInitialsMap: Record<number, string> = {};

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const authUser = this.authService.getCurrentAuthUser();
    if (!authUser?.userId) {
      this.errorMessage = 'Utilisateur non connecté.';
      return;
    }

    this.freelancerUserId = authUser.userId;

    // Charger le profil freelancer pour obtenir le profileId réel
    this.isLoading = true;
    this.profileService.getProfileByUserId(authUser.userId).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: (profile: any) => {
        if (!profile?.id) {
          this.isLoading = false;
          this.errorMessage = 'Profil freelancer introuvable.';
          return;
        }
        this.profileId = profile.id;
        this.loadReviewsData();
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'Impossible de charger le profil.';
      }
    });
  }

  loadReviewsData(): void {
    if (!this.profileId) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    forkJoin({
      reviews: this.profileService.getReviews(this.profileId).pipe(catchError(() => of([]))),
      average: this.profileService.getAverageRating(this.profileId).pipe(catchError(() => of(0))),
      summary: this.profileService.getReviewSummary(this.profileId).pipe(catchError(() => of(null)))
    }).subscribe({
      next: ({ reviews, average, summary }) => {
        // Le propriétaire voit TOUS ses avis (VISIBLE, FLAGGED, HIDDEN)
        this.reviews = reviews ?? [];
        this.averageRating = average ?? 0;
        this.reviewSummary = summary;
        this.latestRating = this.reviews.length > 0 ? this.reviews[0].rating : null;
        this.resolveClientNames();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement des avis.';
        this.isLoading = false;
      }
    });
  }

  // ── Avis suspects : le propriétaire peut masquer ──────────────

  /**
   * Masquer un avis flaggé ou indésirable.
   * Le rating est recalculé côté backend automatiquement (excluant les HIDDEN).
   */
  hideReview(review: ProfileReview): void {
    if (!confirm('Masquer cet avis de votre profil public ?')) return;

    this.isHiding = true;
    this.profileService.hideReview(review.id).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: () => {
        // Mise à jour locale
        const r = this.reviews.find(x => x.id === review.id);
        if (r) r.status = 'HIDDEN';

        this.isHiding = false;
        this.successMessage = 'Avis masqué. Il n\'apparaîtra plus sur votre profil public.';
        // Recharger la moyenne (le backend l'exclut des HIDDEN)
        if (this.profileId) {
          this.profileService.getAverageRating(this.profileId)
            .pipe(catchError(() => of(this.averageRating)))
            .subscribe(avg => this.averageRating = avg ?? 0);
        }
        this.autoClear();
      },
      error: () => {
        this.isHiding = false;
        this.errorMessage = 'Impossible de masquer cet avis.';
        this.autoClear();
      }
    });
  }

  restoreReview(review: ProfileReview): void {
    this.profileService.restoreReview(review.id).pipe(
      catchError(() => of(null))
    ).subscribe({
      next: () => {
        const r = this.reviews.find(x => x.id === review.id);
        if (r) r.status = 'VISIBLE';
        this.successMessage = 'Avis restauré.';
        if (this.profileId) {
          this.profileService.getAverageRating(this.profileId)
            .pipe(catchError(() => of(this.averageRating)))
            .subscribe(avg => this.averageRating = avg ?? 0);
        }
        this.autoClear();
      },
      error: () => {
        this.errorMessage = 'Impossible de restaurer cet avis.';
        this.autoClear();
      }
    });
  }

  // ── Reply ──────────────────────────────────────────────────────

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
    if (!reply || !this.freelancerUserId) return;

    this.isReplying = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.profileService.replyToReview(review.id, this.freelancerUserId, { reply }).subscribe({
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
        this.autoClear();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la publication de la réponse.';
        this.isReplying = false;
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────

  private resolveClientNames(): void {
    const uniqueClientIds = [...new Set(this.reviews.map(r => r.clientId))];
    uniqueClientIds.forEach(clientId => {
      if (this.clientNames[clientId]) return;
      this.profileService.getUserIdentity(clientId).subscribe({
        next: (user: any) => {
          const fullName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim();
          const finalName = fullName || `Client #${clientId}`;
          this.clientNames[clientId] = finalName;
          this.clientInitialsMap[clientId] = this.buildInitials(finalName);
        },
        error: () => {
          const fallback = `Client #${clientId}`;
          this.clientNames[clientId] = fallback;
          this.clientInitialsMap[clientId] = this.buildInitials(fallback);
        }
      });
    });
  }

  getStarArray(rating: number): ('full' | 'empty')[] {
    const rounded = Math.round(rating);
    return Array.from({ length: 5 }, (_, i) => i < rounded ? 'full' : 'empty');
  }

  getRatingLabel(rating: number): string {
    if (rating >= 4.5) return 'Excellent';
    if (rating >= 4)   return 'Très bon';
    if (rating >= 3)   return 'Bon';
    if (rating >= 2)   return 'Moyen';
    if (rating > 0)    return 'Faible';
    return 'Aucune note';
  }

  getDistributionPercentage(count: number): number {
    const total = this.reviewSummary?.totalReviews ?? 0;
    return total > 0 ? (count / total) * 100 : 0;
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

  private autoClear(): void {
    setTimeout(() => {
      this.errorMessage = '';
      this.successMessage = '';
    }, 3500);
  }

  trackByReview(index: number, review: ProfileReview): number {
    return review.id;
  }
}
