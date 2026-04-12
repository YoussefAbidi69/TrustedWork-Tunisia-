import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  FreelancerProfile,
  Skill,
  PortfolioItem,
  Certification,
  WorkExperience,
  Education,
  ProfileReview,
  Endorsement,
  CompletenessResponse,
  CareerPathResponse,
  SkillGapResponse,
  SkillGapRecommendation
} from '../../../core/models/freelancer.model';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';

@Component({
  selector: 'app-profile-detail',
  templateUrl: './profile-detail.component.html',
  styleUrls: ['./profile-detail.component.css']
})
export class ProfileDetailComponent implements OnInit {

  profile: FreelancerProfile | null = null;
  skills: Skill[] = [];
  portfolio: PortfolioItem[] = [];
  certifications: Certification[] = [];
  workExperiences: WorkExperience[] = [];
  educations: Education[] = [];
  reviews: ProfileReview[] = [];
  averageRating = 0;

  completeness: CompletenessResponse | null = null;
  careerPath: CareerPathResponse | null = null;

  skillGapDiagnostic: SkillGapResponse | null = null;
  skillGapRecommendations: SkillGapRecommendation | null = null;

  loading = true;
  errorMsg = '';
  successMsg = '';

  showDeleteConfirm = false;

  selectedSkill: Skill | null = null;
  endorsements: Endorsement[] = [];
  endorsementsLoading = false;
  endorsementsError = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private profileService: FreelancerProfileService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadProfile(id);
    } else {
      this.loading = false;
      this.errorMsg = 'Identifiant de profil invalide.';
    }
  }

  loadProfile(profileId: number): void {
    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.profileService.getProfileById(profileId).subscribe({
      next: (data) => {
        this.profile = data;
        this.loading = false;

        const userId = data.userId;

        this.profileService.getSkillsByUserId(userId).subscribe({
          next: (s) => this.skills = s,
          error: () => this.skills = []
        });

        this.profileService.getPortfolio(userId).subscribe({
          next: (p) => this.portfolio = p,
          error: () => this.portfolio = []
        });

        this.profileService.getCertifications(userId).subscribe({
          next: (c) => this.certifications = c,
          error: () => this.certifications = []
        });

        this.profileService.getWorkExperiences(userId).subscribe({
          next: (w) => this.workExperiences = w,
          error: () => this.workExperiences = []
        });

        this.profileService.getEducations(userId).subscribe({
          next: (e) => this.educations = e,
          error: () => this.educations = []
        });

        this.profileService.getReviewsByProfile(profileId).subscribe({
          next: (r) => this.reviews = r,
          error: () => this.reviews = []
        });

        this.profileService.getAverageRating(profileId).subscribe({
          next: (avg) => this.averageRating = avg,
          error: () => this.averageRating = 0
        });

        this.profileService.getCompleteness(userId).subscribe({
          next: (c) => this.completeness = c,
          error: () => this.completeness = null
        });

        this.profileService.getCareerPath(userId).subscribe({
          next: (cp) => this.careerPath = cp,
          error: () => this.careerPath = null
        });

        this.profileService.getSkillGaps(userId).subscribe({
          next: (diag) => this.skillGapDiagnostic = diag,
          error: () => this.skillGapDiagnostic = null
        });

        this.profileService.getSkillGapRecommendations(userId).subscribe({
          next: (rec) => this.skillGapRecommendations = rec,
          error: () => this.skillGapRecommendations = null
        });
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors du chargement du profil';
        this.loading = false;
        console.error(err);
      }
    });
  }

  private showSuccess(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => this.successMsg = '', 3000);
  }

  confirmDeleteProfile(): void {
    if (!this.profile) return;

    this.profileService.deleteProfile(this.profile.userId).subscribe({
      next: () => {
        this.router.navigate(['/admin/freelancers']);
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression du profil';
        console.error(err);
      }
    });
  }

  changeAvailability(status: 'AVAILABLE' | 'BUSY' | 'ON_VACATION'): void {
    if (!this.profile) return;

    this.profileService.updateAvailability(this.profile.userId, status).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.showSuccess('Disponibilité changée → ' + this.getAvailabilityLabel(status));
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors du changement de disponibilité';
        console.error(err);
      }
    });
  }

  deleteSkill(skillId: number): void {
    if (!this.profile) return;

    this.profileService.deleteSkill(skillId, this.profile.userId).subscribe({
      next: () => {
        this.skills = this.skills.filter(s => s.id !== skillId);
        this.showSuccess('Compétence supprimée');
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression de la compétence';
        console.error(err);
      }
    });
  }

  deleteCertification(certId: number): void {
    if (!this.profile) return;

    this.profileService.deleteCertification(certId, this.profile.userId).subscribe({
      next: () => {
        this.certifications = this.certifications.filter(c => c.id !== certId);
        this.showSuccess('Certification supprimée');
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression de la certification';
        console.error(err);
      }
    });
  }

  deletePortfolioItem(itemId: number): void {
    if (!this.profile) return;

    this.profileService.deletePortfolioItem(itemId, this.profile.userId).subscribe({
      next: () => {
        this.portfolio = this.portfolio.filter(p => p.id !== itemId);
        this.showSuccess('Projet portfolio supprimé');
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression du projet portfolio';
        console.error(err);
      }
    });
  }

  deleteWorkExperience(expId: number): void {
    if (!this.profile) return;

    this.profileService.deleteWorkExperience(expId, this.profile.userId).subscribe({
      next: () => {
        this.workExperiences = this.workExperiences.filter(w => w.id !== expId);
        this.showSuccess('Expérience supprimée');
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression de l’expérience';
        console.error(err);
      }
    });
  }

  deleteEducation(eduId: number): void {
    if (!this.profile) return;

    this.profileService.deleteEducation(eduId, this.profile.userId).subscribe({
      next: () => {
        this.educations = this.educations.filter(e => e.id !== eduId);
        this.showSuccess('Formation supprimée');
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression de la formation';
        console.error(err);
      }
    });
  }

  getStars(rating: number): string[] {
    const stars: string[] = [];
    for (let i = 1; i <= 5; i++) {
      if (i <= Math.floor(rating)) {
        stars.push('fas fa-star');
      } else if (i - rating < 1) {
        stars.push('fas fa-star-half-alt');
      } else {
        stars.push('far fa-star');
      }
    }
    return stars;
  }

  getStatusBadge(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'badge-success';
      case 'BUSY':
        return 'badge-warning';
      case 'ON_VACATION':
        return 'badge-danger';
      default:
        return 'badge-muted';
    }
  }

  getAvailabilityLabel(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'Disponible';
      case 'BUSY':
        return 'Occupé';
      case 'ON_VACATION':
        return 'En vacances';
      default:
        return status || '—';
    }
  }

  getVisibilityLabel(value: string): string {
    switch (value) {
      case 'PUBLIC':
        return 'Public';
      case 'PRIVATE':
        return 'Privé';
      case 'CONNECTIONS_ONLY':
        return 'Connexions uniquement';
      default:
        return value || '—';
    }
  }

  getProjectTypeLabel(value: string): string {
    switch (value) {
      case 'SHORT_TERM':
        return 'Court terme';
      case 'LONG_TERM':
        return 'Long terme';
      case 'BOTH':
        return 'Les deux';
      default:
        return value || '—';
    }
  }

  getScoreClass(score: number): string {
    if (score >= 80) return 'text-success';
    if (score >= 50) return 'text-warning';
    return 'text-danger';
  }

  openEndorsements(skill: Skill): void {
    if (this.selectedSkill?.id === skill.id) {
      this.closeEndorsements();
      return;
    }

    this.selectedSkill = skill;
    this.endorsements = [];
    this.endorsementsLoading = true;
    this.endorsementsError = '';

    this.profileService.getEndorsementsBySkill(skill.id).subscribe({
      next: (list) => {
        this.endorsements = list;
        this.endorsementsLoading = false;
      },
      error: () => {
        this.endorsementsError = 'Impossible de charger les endorsements.';
        this.endorsementsLoading = false;
      }
    });
  }

  closeEndorsements(): void {
    this.selectedSkill = null;
    this.endorsements = [];
    this.endorsementsError = '';
  }

  getCompletenessValue(value: number | null | undefined): number {
    return value ?? 0;
  }
}