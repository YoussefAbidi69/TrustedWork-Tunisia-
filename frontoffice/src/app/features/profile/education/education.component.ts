import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { Education } from '../../../core/models/freelancer.model';

/**
 * Composant Education — parcours académique du freelancer
 * Données déclarées manuellement par l'utilisateur
 * Branché sur le freelancer-profile-service (port 8082)
 * Endpoints : GET/POST/DELETE /api/educations/user/{userId}
 */
@Component({
  selector: 'app-education',
  templateUrl: './education.component.html',
  styleUrls: ['./education.component.css']
})
export class EducationComponent implements OnInit {

  educations: Education[] = [];
  isLoading = false;
  errorMessage = '';
  showForm = false;

  // Formulaire d'ajout
  newEdu: {
    degree: string;
    institution: string;
    fieldOfStudy: string;
    graduationYear: number | null;
  } = {
    degree: '',
    institution: '',
    fieldOfStudy: '',
    graduationYear: null
  };

  // Années disponibles pour le select (1990 → année courante)
  years: number[] = [];

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.buildYears();
    this.loadEducations();
  }

  private get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  // Construire la liste des années pour le select
  private buildYears(): void {
    const current = new Date().getFullYear();
    for (let y = current; y >= 1990; y--) {
      this.years.push(y);
    }
  }

  loadEducations(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.profileService.getMyEducations(this.currentUserId).subscribe({
      next: (data) => {
        this.educations = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le parcours académique.';
        this.isLoading = false;
      }
    });
  }

  addEducation(): void {
    if (!this.newEdu.degree.trim() || !this.newEdu.institution.trim()) return;

    const payload: Partial<Education> = {
      degree: this.newEdu.degree.trim(),
      institution: this.newEdu.institution.trim(),
      fieldOfStudy: this.newEdu.fieldOfStudy.trim() || undefined,
      graduationYear: this.newEdu.graduationYear ?? undefined
    };

    this.profileService.addEducation(this.currentUserId, payload).subscribe({
      next: () => {
        this.newEdu = {
          degree: '',
          institution: '',
          fieldOfStudy: '',
          graduationYear: null
        };
        this.showForm = false;
        this.loadEducations();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de l\'ajout.';
      }
    });
  }

  deleteEducation(eduId: number): void {
    if (!confirm('Supprimer ce parcours ?')) return;
    this.profileService.deleteEducation(eduId, this.currentUserId).subscribe({
      next: () => this.loadEducations(),
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression.';
      }
    });
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    this.errorMessage = '';
  }
}