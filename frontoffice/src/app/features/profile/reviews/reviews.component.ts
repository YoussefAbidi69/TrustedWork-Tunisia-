import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileReview } from '../../../core/models/freelancer.model';

/**
 * Composant Reviews — avis clients sur le profil freelancer
 */
@Component({
  selector: 'app-reviews',
  templateUrl: './reviews.component.html',
  styleUrls: ['./reviews.component.css']
})
export class ReviewsComponent implements OnInit {

  reviews: ProfileReview[] = [];
  averageRating = 0;
  profileId: number | null = null;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  showForm = false;

  // Formulaire d'ajout d'avis
  newReview = {
    clientId: 0,
    rating: 5,
    comment: ''
  };

  // Pour l'affichage des étoiles
  stars = [1, 2, 3, 4, 5];

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  loadProfile(): void {
    this.isLoading = true;
    this.profileService.getProfileByUserId(this.currentUserId).subscribe({
      next: (profile) => {
        this.profileId = profile.id;
        this.loadReviews();
        this.loadAverageRating();
      },
      error: () => {
        this.errorMessage = 'Profil introuvable';
        this.isLoading = false;
      }
    });
  }

  loadReviews(): void {
    if (!this.profileId) return;
    this.profileService.getReviews(this.profileId).subscribe({
      next: (reviews) => {
        this.reviews = reviews;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement des avis';
        this.isLoading = false;
      }
    });
  }

  loadAverageRating(): void {
    if (!this.profileId) return;
    this.profileService.getAverageRating(this.profileId).subscribe({
      next: (avg) => { this.averageRating = avg || 0; }
    });
  }

  submitReview(): void {
    if (!this.profileId) return;

    const payload = {
      clientId: this.newReview.clientId || this.currentUserId,
      rating: this.newReview.rating,
      comment: this.newReview.comment
    };

    this.profileService.addReview(this.profileId, payload).subscribe({
      next: (review) => {
        this.reviews.unshift(review);
        this.averageRating = this.calculateAverage();
        this.resetForm();
        this.showForm = false;
        this.successMessage = 'Avis ajouté avec succès !';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors de l\'ajout de l\'avis';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  setRating(star: number): void {
    this.newReview.rating = star;
  }

  calculateAverage(): number {
    if (this.reviews.length === 0) return 0;
    const sum = this.reviews.reduce((acc, r) => acc + r.rating, 0);
    return Math.round((sum / this.reviews.length) * 10) / 10;
  }

  getStarArray(rating: number): string[] {
    return Array.from({ length: 5 }, (_, i) =>
      i < Math.floor(rating) ? 'full' :
      i < rating ? 'half' : 'empty'
    );
  }

  resetForm(): void {
    this.newReview = { clientId: 0, rating: 5, comment: '' };
  }
}