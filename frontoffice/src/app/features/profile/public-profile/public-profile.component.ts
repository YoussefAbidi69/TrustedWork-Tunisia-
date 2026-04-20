import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { ApiService } from '../../../core/services/api.service';

interface FreelancerProfile {
  id: number;
  userId: number;
  headline: string;
  bio: string;
  avatarUrl: string;
  hourlyRate: number;
  availabilityStatus: 'AVAILABLE' | 'BUSY' | 'ON_VACATION';
  visibility: 'PUBLIC' | 'PRIVATE' | 'CONNECTIONS_ONLY';
  projectType: 'SHORT_TERM' | 'LONG_TERM' | 'BOTH';
  completenessScore: number;
  region: string;
  regionalRank: number;
  totalViews: number;
  createdAt: string;
  updatedAt: string;
}

interface Skill {
  id: number;
  name: string;
  category: string;
  level: string;
  authenticityScore: number;
  examScore: number;
  endorsementCount: number;
}

interface PortfolioItem {
  id: number;
  title: string;
  description: string;
  projectUrl: string;
  imageUrl: string;
  technologies: string;
  completionDate: string;
  pinned: boolean;
  projectScore: number;
}

interface Certification {
  id: number;
  title: string;
  issuer: string;
  type: 'EXTERNAL' | 'INTERNAL' | 'ACADEMIC';
  issueDate: string;
  expiryDate: string;
  certificateUrl: string;
  isExpired: boolean;
}

interface ProfileReview {
  id: number;
  clientId: number;
  rating: number;
  comment: string;
  freelancerReply?: string | null;
  flagged: boolean;
  flagReason?: string | null;
  status: 'VISIBLE' | 'HIDDEN' | 'FLAGGED';
  reviewedAt: string;
  updatedAt?: string | null;
}

interface WorkExperience {
  id: number;
  jobTitle: string;
  company: string;
  location?: string;
  description?: string;
  startDate: string;
  endDate?: string | null;
  isCurrent: boolean;
}

@Component({
  selector: 'app-public-profile',
  templateUrl: './public-profile.component.html',
  styleUrls: ['./public-profile.component.css']
})
export class PublicProfileComponent implements OnInit {

  profile: FreelancerProfile | null = null;

  skills: Skill[] = [];
  portfolio: PortfolioItem[] = [];
  certifications: Certification[] = [];
  reviews: ProfileReview[] = [];
  experiences: WorkExperience[] = [];

  isLoading = true;
  errorMessage = '';
  successMessage = '';

  isOwner = false;

  showReviewForm = false;
  showReportForm = false;
  submittingReview = false;
  submittingReport = false;

  reportCategory: 'FAKE_SKILLS' | 'SPAM' | 'IDENTITY_THEFT' | 'INAPPROPRIATE_CONTENT' | 'OTHER' = 'FAKE_SKILLS';
  reportReason = '';

  newReview = {
    rating: 5,
    comment: ''
  };

  endorsingSkillId: number | null = null;
  endorseComment = '';

  averageRating = 0;
  totalEndorsements = 0;
  averageAuthenticity = 0;
  totalViews = 0;

  targetUserFullName = 'Freelancer';
  targetUserPhoto = '';
  targetUserInitials = 'FR';

  // ── ML Trust Score ──────────────────────────────────────────────────────────
  mlTrustLevel: 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN' = 'UNKNOWN';
  mlTrustConfidence = 0;

  get trustLevelClass(): string {
    switch (this.mlTrustLevel) {
      case 'HIGH':   return 'trust-badge--high';
      case 'MEDIUM': return 'trust-badge--medium';
      case 'LOW':    return 'trust-badge--low';
      default:       return '';
    }
  }

  get trustLevelLabel(): string {
    switch (this.mlTrustLevel) {
      case 'HIGH':   return 'Profil hautement fiable';
      case 'MEDIUM': return 'Profil modérément fiable';
      case 'LOW':    return 'Fiabilité faible';
      default:       return '';
    }
  }
  // ────────────────────────────────────────────────────────────────────────────

  constructor(
    private route: ActivatedRoute,
    private freelancerProfileService: FreelancerProfileService,
    private authService: AuthService,
    private api: ApiService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const profileId = Number(params.get('userId'));

      if (!profileId) {
        this.isLoading = false;
        this.errorMessage = 'Profil invalide.';
        return;
      }

      this.loadPublicProfile(profileId);
    });
  }

  private loadPublicProfile(profileId: number): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.freelancerProfileService.getProfileById(profileId).pipe(
      catchError(() => {
        this.isLoading = false;
        this.errorMessage = 'Profil introuvable.';
        return of(null);
      })
    ).subscribe({
      next: (profile: FreelancerProfile | null) => {
        if (!profile) return;

        this.profile = profile;

        const authUser = this.authService.getCurrentAuthUser();
        const currentUserId = authUser?.userId || null;
        this.isOwner = !!currentUserId && currentUserId === profile.userId;

        forkJoin({
          skills:         this.freelancerProfileService.getSkillsByUserId(profile.userId).pipe(catchError(() => of([]))),
          portfolio:      this.freelancerProfileService.getPortfolioByUserId(profile.userId).pipe(catchError(() => of([]))),
          certifications: this.freelancerProfileService.getCertificationsByUserId(profile.userId).pipe(catchError(() => of([]))),
          reviews:        this.freelancerProfileService.getVisibleReviews(profile.id).pipe(catchError(() => of([]))),
          experiences:    this.freelancerProfileService.getWorkExperiencesByUserId(profile.userId).pipe(catchError(() => of([]))),
          viewsCount:     this.freelancerProfileService.getProfileViewsCount(profile.id).pipe(catchError(() => of(profile.totalViews ?? 0))),
          mlTrust:        this.freelancerProfileService.getMlTrustScore(profile.userId).pipe(catchError(() => of(null))),
          userIdentity:   this.api.get<any>(`/identity/users/${profile.userId}`).pipe(catchError(() => of(null)))
        }).subscribe({
          next: (res: any) => {
            this.skills         = res.skills         || [];
            this.portfolio      = res.portfolio       || [];
            this.certifications = res.certifications  || [];
            this.reviews        = (res.reviews || []).filter((r: ProfileReview) =>
              !r.status || r.status === 'VISIBLE'
            );
            this.experiences    = res.experiences     || [];
            this.totalViews     = res.viewsCount      ?? profile.totalViews ?? 0;

            // ML trust score
            if (res.mlTrust) {
              this.mlTrustLevel      = res.mlTrust.level       || 'UNKNOWN';
              this.mlTrustConfidence = res.mlTrust.confidence  ?? 0;
            }

            this.computeDerivedStats();
            this.resolveTargetUserDisplay(res.userIdentity);

            if (!this.isOwner) {
              this.recordView(profile.id, currentUserId ?? undefined);
            } else {
              this.isLoading = false;
            }
          },
          error: () => {
            this.isLoading = false;
            this.errorMessage = 'Impossible de charger ce profil pour le moment.';
          }
        });
      }
    });
  }

  private recordView(profileId: number, viewerId?: number): void {
    this.freelancerProfileService.recordProfileView(profileId, viewerId).subscribe({
      next: () => this.refreshViewsCount(profileId),
      error: () => { this.isLoading = false; }
    });
  }

  private refreshViewsCount(profileId: number): void {
    this.freelancerProfileService.getProfileViewsCount(profileId).pipe(
      catchError(() => of(this.totalViews))
    ).subscribe({
      next: (count: number) => {
        this.totalViews = count ?? 0;
        if (this.profile) this.profile.totalViews = this.totalViews;
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  private computeDerivedStats(): void {
    if (this.reviews.length > 0) {
      const sum = this.reviews.reduce((acc, r) => acc + (r.rating || 0), 0);
      this.averageRating = sum / this.reviews.length;
    } else {
      this.averageRating = 0;
    }

    this.totalEndorsements = this.skills.reduce((acc, s) => acc + (s.endorsementCount || 0), 0);

    if (this.skills.length > 0) {
      const totalAuth = this.skills.reduce((acc, s) => {
        const value = s.authenticityScore ?? 0;
const normalized = Math.min(value <= 1 ? value * 100 : value, 100);

return acc + normalized;
      }, 0);
      this.averageAuthenticity = totalAuth / this.skills.length;
    } else {
      this.averageAuthenticity = 0;
    }
  }

  private resolveTargetUserDisplay(userIdentity?: any): void {
    if (!this.profile) return;

    // Prefer real name from user-service identity, fall back to headline
    if (userIdentity?.firstName || userIdentity?.lastName) {
      const first = (userIdentity.firstName || '').trim();
      const last  = (userIdentity.lastName  || '').trim();
      this.targetUserFullName = `${first} ${last}`.trim() || this.profile.headline || `Freelancer #${this.profile.userId}`;
    } else {
      this.targetUserFullName = this.profile.headline || `Freelancer #${this.profile.userId}`;
    }

    // Prefer Cloudinary photo from user-service, fall back to avatarUrl from freelancer-profile-service
    this.targetUserPhoto    = (userIdentity?.photo as string) || this.profile.avatarUrl || '';
    this.targetUserInitials = this.extractInitials(this.targetUserFullName);
  }

  private extractInitials(value: string): string {
    if (!value?.trim()) return 'FR';
    const parts = value.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  hasRealPhoto(): boolean {
    return !!this.targetUserPhoto && this.targetUserPhoto.trim().length > 0;
  }

  get availabilityLabel(): string {
    switch (this.profile?.availabilityStatus) {
      case 'AVAILABLE':   return 'Disponible';
      case 'BUSY':        return 'Occupé';
      case 'ON_VACATION': return 'En vacances';
      default:            return 'Indisponible';
    }
  }

  get availabilityClass(): string {
    switch (this.profile?.availabilityStatus) {
      case 'AVAILABLE':   return 'status-pill--available';
      case 'BUSY':        return 'status-pill--busy';
      case 'ON_VACATION': return 'status-pill--vacation';
      default:            return '';
    }
  }

  getStars(rating: number): number[] {
    const rounded = Math.round(rating || 0);
    return Array.from({ length: rounded }, (_, i) => i + 1);
  }

  toggleEndorse(skillId: number): void {
    this.endorsingSkillId = this.endorsingSkillId === skillId ? null : skillId;
    if (this.endorsingSkillId === null) this.endorseComment = '';
    this.errorMessage   = '';
    this.successMessage = '';
  }

  submitEndorsement(skillId: number): void {
    const authUser   = this.authService.getCurrentAuthUser();
    const endorserId = authUser?.userId;

    if (!endorserId) {
      this.errorMessage = 'Vous devez être connecté pour endorser une compétence.';
      return;
    }

    this.freelancerProfileService.endorseSkill(skillId, {
      endorserId,
      comment: this.endorseComment?.trim() ? this.endorseComment.trim() : null
    }).subscribe({
      next: () => {
        this.successMessage   = 'Endorsement ajouté avec succès.';
        this.errorMessage     = '';
        this.endorsingSkillId = null;
        this.endorseComment   = '';

        const skill = this.skills.find(s => s.id === skillId);
        if (skill) skill.endorsementCount = (skill.endorsementCount || 0) + 1;

        this.totalEndorsements = this.skills.reduce((acc, s) => acc + (s.endorsementCount || 0), 0);
      },
      error: () => {
        this.successMessage = '';
        this.errorMessage   = "Impossible d'ajouter l'endorsement.";
      }
    });
  }

  submitReview(): void {
    if (!this.profile) return;

    const authUser = this.authService.getCurrentAuthUser();
    const clientId = authUser?.userId;

    if (!clientId) {
      this.errorMessage = 'Vous devez être connecté pour laisser un avis.';
      return;
    }

    if (this.isOwner) {
      this.errorMessage = 'Vous ne pouvez pas laisser un avis sur votre propre profil.';
      return;
    }

    if ((this.newReview.comment || '').trim().length < 20) {
      this.errorMessage = 'Le commentaire doit contenir au moins 20 caractères.';
      return;
    }

    this.submittingReview = true;
    this.errorMessage     = '';
    this.successMessage   = '';

    this.freelancerProfileService.addProfileReview(this.profile.id, {
      clientId,
      rating:  this.newReview.rating,
      comment: this.newReview.comment.trim()
    }).subscribe({
      next: (created: any) => {
        if (!created.status || created.status === 'VISIBLE') {
          this.reviews = [created, ...this.reviews];
        }
        this.showReviewForm   = false;
        this.newReview        = { rating: 5, comment: '' };
        this.submittingReview = false;
        this.successMessage   = 'Votre avis a été publié avec succès.';
        this.computeDerivedStats();
      },
      error: () => {
        this.submittingReview = false;
        this.errorMessage     = 'Impossible de publier votre avis.';
      }
    });
  }

  submitReport(): void {
    if (!this.profile) return;

    const authUser   = this.authService.getCurrentAuthUser();
    const reporterId = authUser?.userId;

    if (!reporterId) {
      this.errorMessage = 'Vous devez être connecté pour signaler un profil.';
      return;
    }

    if (this.isOwner) {
      this.errorMessage = 'Vous ne pouvez pas signaler votre propre profil.';
      return;
    }

    if (!this.reportReason.trim()) {
      this.errorMessage = 'Veuillez préciser la raison du signalement.';
      return;
    }

    this.submittingReport = true;
    this.errorMessage     = '';
    this.successMessage   = '';

    this.freelancerProfileService.reportProfile(this.profile.id, {
      reporterId,
      category:    this.reportCategory,
      description: this.reportReason.trim()
    }).subscribe({
      next: () => {
        this.submittingReport = false;
        this.showReportForm   = false;
        this.reportReason     = '';
        this.successMessage   = "Le signalement a été envoyé à l'administration.";
      },
      error: () => {
        this.submittingReport = false;
        this.errorMessage     = "Impossible d'envoyer le signalement.";
      }
    });
  }

  trackBySkill(index: number, item: Skill): number      { return item.id; }
  trackByPortfolio(index: number, item: PortfolioItem): number { return item.id; }
  trackByReview(index: number, item: ProfileReview): number    { return item.id; }
}