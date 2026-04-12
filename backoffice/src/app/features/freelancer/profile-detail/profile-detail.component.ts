import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
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
import { UserResolutionService } from '../../../core/services/user-resolution.service';

interface ReviewViewModel extends ProfileReview {
  clientFullName: string;
  clientInitials: string;
}

interface EndorsementViewModel extends Endorsement {
  endorserFullName: string;
  endorserInitials: string;
}

@Component({
  selector: 'app-profile-detail',
  templateUrl: './profile-detail.component.html',
  styleUrls: ['./profile-detail.component.css']
})
export class ProfileDetailComponent implements OnInit {
  profile: FreelancerProfile | null = null;
  profileOwnerName = '';

  skills: Skill[] = [];
  portfolio: PortfolioItem[] = [];
  certifications: Certification[] = [];
  workExperiences: WorkExperience[] = [];
  educations: Education[] = [];
  reviews: ReviewViewModel[] = [];

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
  endorsements: EndorsementViewModel[] = [];
  endorsementsLoading = false;
  endorsementsError = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private profileService: FreelancerProfileService,
    private userResolution: UserResolutionService
  ) {}

  get pinnedPortfolio(): PortfolioItem[] {
    return this.portfolio.filter(item => item.pinned);
  }

  get regularPortfolio(): PortfolioItem[] {
    return this.portfolio.filter(item => !item.pinned);
  }

  get pinnedPortfolioCount(): number {
    return this.pinnedPortfolio.length;
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id) {
      this.loading = false;
      this.errorMsg = 'Identifiant de profil invalide.';
      return;
    }

    this.loadProfile(id);
  }
  activeTab: string = 'overview';

setTab(tab: string) {
  this.activeTab = tab;
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

        this.userResolution.getFullName(userId).subscribe({
          next: (name) => {
            this.profileOwnerName = name;
          },
          error: () => {
            this.profileOwnerName = `User #${userId}`;
          }
        });

        this.profileService.getSkillsByUserId(userId).subscribe({
          next: (skills) => {
            this.skills = skills || [];
          },
          error: () => {
            this.skills = [];
          }
        });

        this.profileService.getPortfolio(userId).subscribe({
          next: (portfolio) => {
            this.portfolio = this.sortPortfolioItems(portfolio || []);
          },
          error: () => {
            this.portfolio = [];
          }
        });

        this.profileService.getCertifications(userId).subscribe({
          next: (certifications) => {
            this.certifications = (certifications || []).sort(
              (a, b) => this.getSortableDateValue(b.issueDate) - this.getSortableDateValue(a.issueDate)
            );
          },
          error: () => {
            this.certifications = [];
          }
        });

        this.profileService.getWorkExperiences(userId).subscribe({
          next: (workExperiences) => {
            this.workExperiences = (workExperiences || []).sort((a, b) => {
              if (!!b.isCurrent !== !!a.isCurrent) {
                return Number(!!b.isCurrent) - Number(!!a.isCurrent);
              }
              return this.getSortableDateValue(b.startDate) - this.getSortableDateValue(a.startDate);
            });
          },
          error: () => {
            this.workExperiences = [];
          }
        });

        this.profileService.getEducations(userId).subscribe({
          next: (educations) => {
            this.educations = educations || [];
          },
          error: () => {
            this.educations = [];
          }
        });

        this.profileService.getReviewsByProfile(profileId).subscribe({
          next: (rawReviews) => {
            if (!rawReviews || rawReviews.length === 0) {
              this.reviews = [];
              return;
            }

            forkJoin(rawReviews.map(r => this.userResolution.getFullName(r.clientId))).subscribe({
              next: (names) => {
                this.reviews = rawReviews.map((r, i) => ({
                  ...r,
                  clientFullName: names[i],
                  clientInitials: this.userResolution.getInitials(names[i])
                }));
              },
              error: () => {
                this.reviews = rawReviews.map(r => ({
                  ...r,
                  clientFullName: `User #${r.clientId}`,
                  clientInitials: 'U'
                }));
              }
            });
          },
          error: () => {
            this.reviews = [];
          }
        });

        this.profileService.getAverageRating(profileId).subscribe({
          next: (avg) => {
            this.averageRating = avg;
          },
          error: () => {
            this.averageRating = 0;
          }
        });

        this.profileService.getCompleteness(userId).subscribe({
          next: (completeness) => {
            this.completeness = completeness;
          },
          error: () => {
            this.completeness = null;
          }
        });

        this.profileService.getCareerPath(userId).subscribe({
          next: (careerPath) => {
            this.careerPath = careerPath;
          },
          error: () => {
            this.careerPath = null;
          }
        });

        this.profileService.getSkillGaps(userId).subscribe({
          next: (diagnostic) => {
            this.skillGapDiagnostic = diagnostic;
          },
          error: () => {
            this.skillGapDiagnostic = null;
          }
        });

        this.profileService.getSkillGapRecommendations(userId).subscribe({
          next: (recommendations) => {
            this.skillGapRecommendations = recommendations;
          },
          error: () => {
            this.skillGapRecommendations = null;
          }
        });
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors du chargement du profil';
        this.loading = false;
        console.error(err);
      }
    });
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
        this.skills = this.skills.filter(skill => skill.id !== skillId);

        if (this.selectedSkill?.id === skillId) {
          this.closeEndorsements();
        }

        this.showSuccess('Compétence supprimée');
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression de la compétence';
        console.error(err);
      }
    });
  }

  pinPortfolioItem(itemId: number): void {
    if (!this.profile) return;

    this.profileService.pinPortfolioItem(itemId, this.profile.userId).subscribe({
      next: (updated) => {
        this.portfolio = this.sortPortfolioItems(
          this.portfolio.map(item => item.id === itemId ? updated : item)
        );
        this.showSuccess('Projet épinglé');
      },
      error: (err) => {
        this.errorMsg = err?.error?.message || 'Erreur lors de l’épinglage du projet';
        console.error(err);
      }
    });
  }

  unpinPortfolioItem(itemId: number): void {
    if (!this.profile) return;

    this.profileService.unpinPortfolioItem(itemId, this.profile.userId).subscribe({
      next: (updated) => {
        this.portfolio = this.sortPortfolioItems(
          this.portfolio.map(item => item.id === itemId ? updated : item)
        );
        this.showSuccess('Projet désépinglé');
      },
      error: (err) => {
        this.errorMsg = err?.error?.message || 'Erreur lors du désépinglage du projet';
        console.error(err);
      }
    });
  }

  deletePortfolioItem(itemId: number): void {
    if (!this.profile) return;

    this.profileService.deletePortfolioItem(itemId, this.profile.userId).subscribe({
      next: () => {
        this.portfolio = this.portfolio.filter(item => item.id !== itemId);
        this.showSuccess('Projet portfolio supprimé');
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression du projet portfolio';
        console.error(err);
      }
    });
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
      next: (rawList) => {
        if (!rawList || rawList.length === 0) {
          this.endorsements = [];
          this.endorsementsLoading = false;
          return;
        }

        forkJoin(rawList.map(e => this.userResolution.getFullName(e.endorserId))).subscribe({
          next: (names) => {
            this.endorsements = rawList.map((e, i) => ({
              ...e,
              endorserFullName: names[i],
              endorserInitials: this.userResolution.getInitials(names[i])
            }));
            this.endorsementsLoading = false;
          },
          error: () => {
            this.endorsements = rawList.map(e => ({
              ...e,
              endorserFullName: `User #${e.endorserId}`,
              endorserInitials: 'U'
            }));
            this.endorsementsLoading = false;
          }
        });
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
    this.endorsementsLoading = false;
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

  private showSuccess(msg: string): void {
    this.successMsg = msg;
    setTimeout(() => {
      this.successMsg = '';
    }, 3000);
  }

  private getSortableDateValue(date: string | Date | undefined | null): number {
    if (!date) return 0;

    const parsed = new Date(date);
    return Number.isNaN(parsed.getTime()) ? 0 : parsed.getTime();
  }

  private sortPortfolioItems(items: PortfolioItem[]): PortfolioItem[] {
    return [...items].sort((a, b) => {
      if (!!a.pinned !== !!b.pinned) {
        return Number(!!b.pinned) - Number(!!a.pinned);
      }

      return this.getSortableDateValue(b.completionDate) - this.getSortableDateValue(a.completionDate);
    });
  }

  
}