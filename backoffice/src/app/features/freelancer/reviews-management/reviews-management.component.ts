import { Component, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subject, forkJoin, of } from 'rxjs';
import { catchError, switchMap, takeUntil } from 'rxjs/operators';
import { FreelancerProfile, ProfileReview } from '../../../core/models/freelancer.model';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { UserResolutionService } from '../../../core/services/user-resolution.service';

interface ReviewViewModel extends ProfileReview {
  profileId: number;
  profileUserId: number;
  headline: string;
  region: string;
  completenessScore: number;
  totalViews: number;
  clientFullName: string;
  clientInitials: string;
  freelancerFullName: string;
  freelancerInitials: string;
}

@Component({
  selector: 'app-reviews-management',
  templateUrl: './reviews-management.component.html',
  styleUrls: ['./reviews-management.component.css']
})
export class ReviewsManagementComponent implements OnInit, OnDestroy {

  reviews: ReviewViewModel[] = [];
  filteredReviews: ReviewViewModel[] = [];

  loading = true;
  errorMsg = '';
  successMsg = '';

  searchTerm = '';
  selectedRegion = 'ALL';
  selectedRating = 'ALL';
  selectedStatus = 'ALL';

  availableRegions: string[] = [];

  totalReviews = 0;
  averageRating = 0;
  flaggedCount = 0;
  visibleCount = 0;

  expandedId: number | null = null;

  // ID de la review en cours de suppression ou masquage
  processingId: number | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private profileService: FreelancerProfileService,
    private userResolution: UserResolutionService
  ) {}

  ngOnInit(): void {
    this.loadReviews();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadReviews(): void {
    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';
    this.expandedId = null;

    this.profileService.getAllProfiles()
      .pipe(
        takeUntil(this.destroy$),
        switchMap((profiles: FreelancerProfile[]) => {
          if (!profiles || profiles.length === 0) {
            return of([] as ReviewViewModel[]);
          }

          const validProfiles = profiles.filter(
            profile => profile?.id !== undefined && profile?.id !== null
          );

          if (validProfiles.length === 0) {
            return of([] as ReviewViewModel[]);
          }

          const profileRequests = validProfiles.map(profile =>
            this.profileService.getReviewsByProfile(profile.id).pipe(
              switchMap((reviews: ProfileReview[]) => {
                if (!reviews || reviews.length === 0) {
                  return of([] as ReviewViewModel[]);
                }

                const reviewVmRequests = reviews.map(review =>
                  forkJoin({
                    clientName: this.safeName(() => this.userResolution.getFullName(review.clientId)),
                    freelancerName: this.safeName(() => this.userResolution.getFullName(profile.userId))
                  }).pipe(
                    switchMap(({ clientName, freelancerName }) => {
                      const vm: ReviewViewModel = {
                        ...review,
                        profileId: profile.id!,
                        profileUserId: profile.userId!,
                        headline: profile.headline || '—',
                        region: profile.region || '—',
                        completenessScore: profile.completenessScore ?? 0,
                        totalViews: profile.totalViews ?? 0,
                        clientFullName: clientName,
                        clientInitials: this.userResolution.getInitials(clientName),
                        freelancerFullName: freelancerName,
                        freelancerInitials: this.userResolution.getInitials(freelancerName)
                      };

                      return of(vm);
                    }),
                    catchError(() => of({
                      ...review,
                      profileId: profile.id!,
                      profileUserId: profile.userId!,
                      headline: profile.headline || '—',
                      region: profile.region || '—',
                      completenessScore: profile.completenessScore ?? 0,
                      totalViews: profile.totalViews ?? 0,
                      clientFullName: 'Client inconnu',
                      clientInitials: 'CI',
                      freelancerFullName: 'Freelancer inconnu',
                      freelancerInitials: 'FI'
                    } as ReviewViewModel))
                  )
                );

                return forkJoin(reviewVmRequests);
              }),
              catchError(() => of([] as ReviewViewModel[]))
            )
          );

          return forkJoin(profileRequests).pipe(
            switchMap((groups: ReviewViewModel[][]) => of(groups.flat()))
          );
        }),
        catchError((err) => {
          console.error(err);
          this.errorMsg = 'Erreur lors du chargement des reviews.';
          this.loading = false;
          return of([] as ReviewViewModel[]);
        })
      )
      .subscribe((reviews: ReviewViewModel[]) => {
        this.reviews = reviews.sort((a, b) =>
          new Date(b.reviewedAt || 0).getTime() - new Date(a.reviewedAt || 0).getTime()
        );

        this.availableRegions = [
          ...new Set(
            this.reviews
              .map(r => r.region)
              .filter((region): region is string => !!region && region.trim().length > 0 && region !== '—')
          )
        ].sort((a, b) => a.localeCompare(b));

        this.computeStats();
        this.applyFilters();
        this.loading = false;
      });
  }

  // Masquer une review (admin)
  hideReview(review: ReviewViewModel): void {
    if (!confirm(`Masquer cet avis de ${review.clientFullName} ?`)) return;

    this.errorMsg = '';
    this.successMsg = '';
    this.processingId = review.id;

    this.profileService.hideReview(review.id).subscribe({
      next: () => {
        const target = this.reviews.find(r => r.id === review.id);
        if (target) {
          target.status = 'HIDDEN';
        }

        this.computeStats();
        this.applyFilters();
        this.processingId = null;
        this.expandedId = null;
        this.successMsg = 'Avis masqué avec succès.';
        this.autoClearSuccess();
      },
      error: () => {
        this.errorMsg = 'Erreur lors du masquage de l’avis.';
        this.processingId = null;
      }
    });
  }

  // Supprimer une review (admin)
  deleteReview(review: ReviewViewModel): void {
    if (!confirm(`Supprimer définitivement cet avis de ${review.clientFullName} ? Cette action est irréversible.`)) {
      return;
    }

    this.errorMsg = '';
    this.successMsg = '';
    this.processingId = review.id;

    this.profileService.deleteReview(review.id).subscribe({
      next: () => {
        this.reviews = this.reviews.filter(r => r.id !== review.id);
        this.computeStats();
        this.applyFilters();
        this.processingId = null;
        this.expandedId = null;
        this.successMsg = 'Avis supprimé avec succès.';
        this.autoClearSuccess();
      },
      error: () => {
        this.errorMsg = 'Erreur lors de la suppression de l’avis.';
        this.processingId = null;
      }
    });
  }

  private safeName(fn: () => Observable<string>): Observable<string> {
    return fn().pipe(
      catchError(() => of('Utilisateur inconnu'))
    );
  }

  computeStats(): void {
    this.totalReviews = this.reviews.length;
    this.visibleCount = this.reviews.filter(r => r.status === 'VISIBLE').length;
    this.flaggedCount = this.reviews.filter(r => r.status === 'FLAGGED' || !!r.flagged).length;

    if (this.reviews.length === 0) {
      this.averageRating = 0;
      return;
    }

    const total = this.reviews.reduce((sum, review) => sum + (review.rating || 0), 0);
    this.averageRating = Math.round((total / this.reviews.length) * 10) / 10;
  }

  applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();

    this.filteredReviews = this.reviews.filter(review => {
      const matchesSearch =
        !term ||
        (review.clientFullName || '').toLowerCase().includes(term) ||
        (review.freelancerFullName || '').toLowerCase().includes(term) ||
        (review.headline || '').toLowerCase().includes(term) ||
        (review.comment || '').toLowerCase().includes(term);

      const matchesRegion =
        this.selectedRegion === 'ALL' || review.region === this.selectedRegion;

      const matchesRating =
        this.selectedRating === 'ALL' || review.rating === Number(this.selectedRating);

      const matchesStatus =
        this.selectedStatus === 'ALL' || review.status === this.selectedStatus;

      return matchesSearch && matchesRegion && matchesRating && matchesStatus;
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedRegion = 'ALL';
    this.selectedRating = 'ALL';
    this.selectedStatus = 'ALL';
    this.applyFilters();
  }

  toggleExpand(id: number): void {
    this.expandedId = this.expandedId === id ? null : id;
  }

  getRatingStars(rating: number): ('full' | 'empty')[] {
    const stars: ('full' | 'empty')[] = [];

    for (let i = 1; i <= 5; i++) {
      stars.push(i <= rating ? 'full' : 'empty');
    }

    return stars;
  }

  getRatingClass(rating: number): string {
    if (rating >= 4.5) return 'excellent';
    if (rating >= 4) return 'very-good';
    if (rating >= 3) return 'good';
    if (rating >= 2) return 'average';
    return 'poor';
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'VISIBLE':
        return 'Visible';
      case 'HIDDEN':
        return 'Masqué';
      case 'FLAGGED':
        return 'Signalé';
      default:
        return status || '—';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'VISIBLE':
        return 'success';
      case 'HIDDEN':
        return 'warning';
      case 'FLAGGED':
        return 'danger';
      default:
        return 'muted';
    }
  }

  getShortComment(comment: string | null | undefined): string {
    if (!comment) return '—';
    return comment.length > 110 ? `${comment.slice(0, 110)}...` : comment;
  }

  private autoClearSuccess(): void {
    setTimeout(() => {
      this.successMsg = '';
    }, 3000);
  }

  trackByReview(index: number, review: ReviewViewModel): number {
    return review.id;
  }

  restoreReview(review: ReviewViewModel): void {
    if (!confirm(`Rendre cet avis visible à nouveau ?`)) return;
  
    this.processingId = review.id;
    this.errorMsg = '';
    this.successMsg = '';
  
    this.profileService.restoreReview(review.id).subscribe({
      next: () => {
        const target = this.reviews.find(r => r.id === review.id);
        if (target) {
          target.status = 'VISIBLE';
        }
  
        this.computeStats();
        this.applyFilters();
        this.processingId = null;
        this.successMsg = 'Avis remis en ligne.';
      },
      error: () => {
        this.errorMsg = 'Erreur lors de la restauration.';
        this.processingId = null;
      }
    });
  }
}