import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { WorkExperience } from '../../../core/models/freelancer.model';

/**
 * Composant Expériences Professionnelles
 */
@Component({
  selector: 'app-work-experience',
  templateUrl: './work-experience.component.html',
  styleUrls: ['./work-experience.component.css']
})
export class WorkExperienceComponent implements OnInit {

  experiences: WorkExperience[] = [];
  isLoading = false;
  errorMessage = '';
  showForm = false;

 newExp: {
  jobTitle: string;
  company: string;
  description: string;
  startDate: string;
  endDate: string | undefined;
  isCurrent: boolean;
} = {
  jobTitle: '',
  company: '',
  description: '',
  startDate: '',
  endDate: undefined,
  isCurrent: false
};

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadExperiences();
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  loadExperiences(): void {
    this.isLoading = true;
    this.profileService.getMyWorkExperiences(this.currentUserId).subscribe({
      next: (data) => {
        this.experiences = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement des expériences';
        this.isLoading = false;
      }
    });
  }

  addExperience(): void {
    if (!this.newExp.jobTitle.trim() || !this.newExp.company.trim()) return;

    const payload = {
      ...this.newExp,
        endDate: this.newExp.isCurrent ? undefined : this.newExp.endDate
    };

    this.profileService.addWorkExperience(this.currentUserId, payload).subscribe({
      next: (exp) => {
        this.experiences.unshift(exp);
        this.resetForm();
        this.showForm = false;
      },
      error: () => {
        this.errorMessage = 'Erreur lors de l\'ajout de l\'expérience';
      }
    });
  }

  deleteExperience(expId: number): void {
    if (!confirm('Supprimer cette expérience ?')) return;

    this.profileService.deleteWorkExperience(expId, this.currentUserId).subscribe({
      next: () => {
        this.experiences = this.experiences.filter(e => e.id !== expId);
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression';
      }
    });
  }

  resetForm(): void {
  this.newExp = {
    jobTitle: '',
    company: '',
    description: '',
    startDate: '',
    endDate: undefined, 
    isCurrent: false
  };
}

  // Calculer la durée d'une expérience
  getDuration(exp: WorkExperience): string {
    if (!exp.startDate) return '';
    const start = new Date(exp.startDate);
    const end = exp.isCurrent ? new Date() : new Date(exp.endDate);
    const months = (end.getFullYear() - start.getFullYear()) * 12
                 + (end.getMonth() - start.getMonth());
    if (months < 12) return `${months} mois`;
    const years = Math.floor(months / 12);
    const rem = months % 12;
    return rem > 0 ? `${years} an(s) ${rem} mois` : `${years} an(s)`;
  }
}