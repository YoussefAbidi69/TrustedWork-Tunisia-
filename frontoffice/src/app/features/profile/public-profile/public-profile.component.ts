import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  FreelancerProfile,
  Skill,
  PortfolioItem,
  Certification,
  WorkExperience,
  ProfileReview
} from '../../../core/models/freelancer.model';
import { forkJoin } from 'rxjs';

/**
 * Profil public d'un freelancer
 * Accessible par n'importe quel utilisateur connecté
 * Fonctionnalités : endorsement skills, avis client, signalement profil
 * Protection : isOwner bloque auto-endorsement et auto-review
 */
@Component({
  selector: 'app-public-profile',
  templateUrl: './public-profile.component.html',
  styleUrls: ['./public-profile.component.css']
})
export class PublicProfileComponent implements OnInit {

  targetUserId!: number;
  targetProfileId!: number;

  profile: FreelancerProfile | null = null;
  skills: Skill[] = [];
  portfolio: PortfolioItem[] = [];
  certifications: Certification[] = [];
  experiences: WorkExperience[] = [];
  reviews: ProfileReview[] = [];
  averageRating = 0;

  isLoading = true;
  errorMessage = '';
  successMessage = '';

  // Endorsement
  endorsingSkillId: number | null = null;
  endorseComment = '';

  // Review
  showReviewForm = false;
  newReview = { rating: 5, comment: '' };

  // Signalement
  showReportForm = false;
  reportReason = '';

  constructor(
    private route: ActivatedRoute,
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.targetUserId = Number(this.route.snapshot.paramMap.get('userId'));
    this.loadPublicProfile();
  }

  get isOwner(): boolean {
    return this.authService.getCurrentAuthUser()?.userId === this.targetUserId;
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  loadPublicProfile(): void {
    this.isLoading = true;

    this.profileService.getProfileByUserId(this.targetUserId).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.targetProfileId = profile.id;

        forkJoin({
          skills:         this.profileService.getMySkills(this.targetUserId),
          portfolio:      this.profileService.getMyPortfolio(this.targetUserId),
          certifications: this.profileService.getMyCertifications(this.targetUserId),
          experiences:    this.profileService.getMyWorkExperiences(this.targetUserId),
          reviews:        this.profileService.getReviews(this.targetProfileId),
          average:        this.profileService.getAverageRating(this.targetProfileId)
        }).subscribe({
          next: (data) => {
            this.skills         = data.skills;
            this.portfolio      = data.portfolio;
            this.certifications = data.certifications;
            this.experiences    = data.experiences;
            this.reviews        = data.reviews;
            this.averageRating  = data.average;
            this.isLoading      = false;
          },
          error: () => { this.isLoading = false; }
        });
      },
      error: () => {
        this.errorMessage = 'Profil introuvable.';
        this.isLoading = false;
      }
    });
  }

  // ===== ENDORSEMENT =====

  toggleEndorse(skillId: number): void {
    this.endorsingSkillId = this.endorsingSkillId === skillId ? null : skillId;
    this.endorseComment = '';
  }

  submitEndorsement(skillId: number): void {
    const payload = {
      endorserId: this.currentUserId,
      comment: this.endorseComment
    };

    this.profileService.addEndorsement(skillId, payload).subscribe({
      next: () => {
        const skill = this.skills.find(s => s.id === skillId);
        if (skill) skill.endorsementCount++;
        this.endorsingSkillId = null;
        this.endorseComment = '';
        this.successMessage = 'Endorsement ajouté avec succès !';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors de l\'endorsement.';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  // ===== REVIEW =====

  submitReview(): void {
    if (!this.newReview.comment.trim()) return;

    const payload = {
      clientId: this.currentUserId,
      rating: this.newReview.rating,
      comment: this.newReview.comment
    };

    this.profileService.addReview(this.targetProfileId, payload).subscribe({
      next: (review) => {
        this.reviews.unshift(review);
        this.showReviewForm = false;
        this.newReview = { rating: 5, comment: '' };
        this.successMessage = 'Avis ajouté avec succès !';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors de l\'ajout de l\'avis.';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  // ===== SIGNALEMENT =====

  submitReport(): void {
    if (!this.reportReason.trim()) return;

    this.profileService.reportProfile(this.targetProfileId, {
      reporterId: this.currentUserId,
      reason: this.reportReason.trim()
    }).subscribe({
      next: () => {
        this.showReportForm = false;
        this.reportReason = '';
        this.successMessage = 'Signalement envoyé à l\'administration.';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: () => {
        this.errorMessage = 'Erreur lors du signalement.';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  getStars(rating: number): number[] {
    return Array(Math.round(rating)).fill(0);
  }
}