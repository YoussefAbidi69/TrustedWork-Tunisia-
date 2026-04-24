import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { WorkExperience } from '../../../core/models/freelancer.model';

interface WorkExperienceForm {
  jobTitle: string;
  company: string;
  location: string;
  description: string;
  startDate: string;
  endDate: string | null;
  isCurrent: boolean;
}

@Component({
  selector: 'app-work-experience',
  templateUrl: './work-experience.component.html',
  styleUrls: ['./work-experience.component.css']
})
export class WorkExperienceComponent implements OnInit {
  experiences: WorkExperience[] = [];
  isLoading = false;
  isSaving = false;
  deletingId: number | null = null;

  errorMessage = '';
  successMessage = '';

  showForm = false;
  isEditMode = false;
  editingId: number | null = null;

  totalExperienceMonths = 0;

  newExp: WorkExperienceForm = this.createEmptyForm();

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadExperiences();
    this.loadTotalDuration();
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  get hasExperiences(): boolean {
    return this.experiences.length > 0;
  }

  get canSave(): boolean {
    if (this.isSaving) return false;
    if (!this.newExp.jobTitle.trim()) return false;
    if (!this.newExp.company.trim()) return false;
    if (!this.newExp.startDate) return false;
    if (!this.newExp.isCurrent && !this.newExp.endDate) return false;
    return !this.hasDateError;
  }

  get hasDateError(): boolean {
    if (!this.newExp.startDate) return false;
    if (this.newExp.isCurrent) return false;
    if (!this.newExp.endDate) return false;

    return new Date(this.newExp.startDate) > new Date(this.newExp.endDate);
  }

  get totalExperienceLabel(): string {
    return this.formatMonths(this.totalExperienceMonths);
  }

  loadExperiences(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.profileService.getMyWorkExperiences(this.currentUserId).subscribe({
      next: (data) => {
        this.experiences = data || [];
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement des expériences.';
        this.isLoading = false;
      }
    });
  }

  loadTotalDuration(): void {
    this.profileService.getTotalWorkExperienceDuration(this.currentUserId).subscribe({
      next: (months) => {
        this.totalExperienceMonths = months || 0;
      },
      error: () => {
        this.totalExperienceMonths = 0;
      }
    });
  }

  openForm(): void {
    this.clearMessages();
    this.showForm = true;
    this.isEditMode = false;
    this.editingId = null;
    this.newExp = this.createEmptyForm();
  }

  openEditForm(exp: WorkExperience): void {
    this.clearMessages();
    this.showForm = true;
    this.isEditMode = true;
    this.editingId = exp.id;

    this.newExp = {
      jobTitle: exp.jobTitle || '',
      company: exp.company || '',
      location: exp.location || '',
      description: exp.description || '',
      startDate: exp.startDate || '',
      endDate: exp.endDate ?? null,
      isCurrent: !!exp.isCurrent
    };
  }

  closeForm(): void {
    this.showForm = false;
    this.isEditMode = false;
    this.editingId = null;
    this.newExp = this.createEmptyForm();
  }

  onCurrentChange(): void {
    if (this.newExp.isCurrent) {
      this.newExp.endDate = null;
    }
  }

  saveExperience(): void {
    if (!this.canSave) return;

    this.clearMessages();
    this.isSaving = true;

    const payload = {
      jobTitle: this.newExp.jobTitle.trim(),
      company: this.newExp.company.trim(),
      location: this.newExp.location.trim() || undefined,
      description: this.newExp.description.trim() || undefined,
      startDate: this.newExp.startDate,
      endDate: this.newExp.isCurrent ? null : this.newExp.endDate,
      isCurrent: this.newExp.isCurrent
    };

    if (this.isEditMode && this.editingId !== null) {
      this.profileService.updateWorkExperience(this.editingId, this.currentUserId, payload).subscribe({
        next: () => {
          this.isSaving = false;
          this.successMessage = 'Expérience mise à jour avec succès.';
          this.closeForm();
          this.reloadData();
          this.autoClearMessages();
        },
        error: (error) => {
          this.isSaving = false;
          this.errorMessage = error?.error?.message || 'Erreur lors de la mise à jour.';
          this.autoClearMessages();
        }
      });
      return;
    }

    this.profileService.addWorkExperience(this.currentUserId, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.successMessage = 'Expérience ajoutée avec succès.';
        this.closeForm();
        this.reloadData();
        this.autoClearMessages();
      },
      error: (error) => {
        this.isSaving = false;
        this.errorMessage = error?.error?.message || 'Erreur lors de l’ajout de l’expérience.';
        this.autoClearMessages();
      }
    });
  }

  deleteExperience(expId: number): void {
    if (!confirm('Supprimer cette expérience ?')) return;

    this.clearMessages();
    this.deletingId = expId;

    this.profileService.deleteWorkExperience(expId, this.currentUserId).subscribe({
      next: () => {
        this.deletingId = null;
        this.successMessage = 'Expérience supprimée avec succès.';
        this.reloadData();
        this.autoClearMessages();
      },
      error: (error) => {
        this.deletingId = null;
        this.errorMessage = error?.error?.message || 'Erreur lors de la suppression.';
        this.autoClearMessages();
      }
    });
  }

  reloadData(): void {
    this.loadExperiences();
    this.loadTotalDuration();
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  autoClearMessages(): void {
    setTimeout(() => {
      this.errorMessage = '';
      this.successMessage = '';
    }, 3500);
  }

  createEmptyForm(): WorkExperienceForm {
    return {
      jobTitle: '',
      company: '',
      location: '',
      description: '',
      startDate: '',
      endDate: null,
      isCurrent: false
    };
  }

  getDuration(exp: WorkExperience): string {
    if (exp.durationLabel?.trim()) {
      return exp.durationLabel;
    }

    if (!exp.startDate) return '';

    const start = new Date(exp.startDate);
    const end = exp.isCurrent ? new Date() : new Date(exp.endDate as string);

    const totalMonths =
      (end.getFullYear() - start.getFullYear()) * 12 +
      (end.getMonth() - start.getMonth());

    return this.formatMonths(totalMonths);
  }

  getPeriod(exp: WorkExperience): string {
    if (exp.periodLabel?.trim()) {
      return exp.periodLabel;
    }

    const start = this.formatMonthYear(exp.startDate);
    const end = exp.isCurrent ? 'Présent' : this.formatMonthYear(exp.endDate || '');

    return `${start} - ${end}`;
  }

  formatMonths(totalMonths: number): string {
    if (!totalMonths || totalMonths <= 0) return 'Moins d’un mois';
    if (totalMonths < 12) return `${totalMonths} mois`;

    const years = Math.floor(totalMonths / 12);
    const months = totalMonths % 12;

    if (years > 0 && months > 0) {
      return `${years} ${years === 1 ? 'an' : 'ans'} ${months} mois`;
    }

    return `${years} ${years === 1 ? 'an' : 'ans'}`;
  }

  formatMonthYear(value: string): string {
    if (!value) return '';
    const date = new Date(value);
    return date.toLocaleDateString('fr-FR', {
      month: 'short',
      year: 'numeric'
    });
  }

  trackByExperience(index: number, exp: WorkExperience): number {
    return exp.id;
  }
}