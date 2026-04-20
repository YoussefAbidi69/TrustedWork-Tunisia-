import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { Education } from '../../../core/models/freelancer.model';

/**
 * Composant Education — parcours académique du freelancer
 * Endpoints utilisés :
 *   GET    /api/educations/user/{userId}
 *   POST   /api/educations/user/{userId}
 *   PUT    /api/educations/{eduId}/user/{userId}
 *   DELETE /api/educations/{eduId}/user/{userId}
 *
 * Fonctionnalités :
 * - Affichage trié par année décroissante (géré côté backend)
 * - Ajout avec détection doublon (message d'erreur backend)
 * - Modification inline (formulaire d'édition par card)
 * - Suppression avec confirmation
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
  showAddForm = false;

  // Formulaire d'ajout
  newEdu = {
    degree: '',
    institution: '',
    fieldOfStudy: '',
    graduationYear: null as number | null
  };

  // Formulaire d'édition — on stocke l'id de la card en cours d'édition
  editingId: number | null = null;
  editForm = {
    degree: '',
    institution: '',
    fieldOfStudy: '',
    graduationYear: null as number | null
  };

  // Années disponibles pour les selects (1950 → année courante)
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

  // Construire la liste d'années pour les selects
  private buildYears(): void {
    const current = new Date().getFullYear();
    for (let y = current; y >= 1950; y--) {
      this.years.push(y);
    }
  }

  // ─── Chargement ──────────────────────────────────────────────────────────
  loadEducations(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.profileService.getMyEducations(this.currentUserId).subscribe({
      next: (data) => {
        this.educations = data; // déjà triées par année DESC côté backend
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le parcours académique.';
        this.isLoading = false;
      }
    });
  }

  // ─── Ajout ────────────────────────────────────────────────────────────────
  addEducation(): void {
    if (!this.newEdu.degree.trim() || !this.newEdu.institution.trim()) {
      this.errorMessage = 'Le diplôme et l\'établissement sont obligatoires.';
      return;
    }

    const payload: Partial<Education> = {
      degree: this.newEdu.degree.trim(),
      institution: this.newEdu.institution.trim(),
      fieldOfStudy: this.newEdu.fieldOfStudy.trim() || undefined,
      graduationYear: this.newEdu.graduationYear ?? undefined
    };

    this.profileService.addEducation(this.currentUserId, payload).subscribe({
      next: () => {
        this.resetAddForm();
        this.showAddForm = false;
        this.loadEducations();
      },
      error: (err) => {
        // Le backend retourne le message métier (ex: doublon détecté)
        this.errorMessage = err.error || 'Erreur lors de l\'ajout.';
      }
    });
  }

  // ─── Édition ─────────────────────────────────────────────────────────────
  startEdit(edu: Education): void {
    this.editingId = edu.id;
    this.editForm = {
      degree: edu.degree,
      institution: edu.institution,
      fieldOfStudy: edu.fieldOfStudy || '',
      graduationYear: edu.graduationYear ?? null
    };
    this.errorMessage = '';
  }

  cancelEdit(): void {
    this.editingId = null;
    this.errorMessage = '';
  }

  saveEdit(eduId: number): void {
    const payload: Partial<Education> = {
      degree: this.editForm.degree.trim(),
      institution: this.editForm.institution.trim(),
      // undefined est compatible avec string | undefined (contrairement à null)
      fieldOfStudy: this.editForm.fieldOfStudy.trim() || undefined,
      graduationYear: this.editForm.graduationYear ?? undefined
    };

    this.profileService.updateEducation(eduId, this.currentUserId, payload).subscribe({
      next: () => {
        this.editingId = null;
        this.loadEducations();
      },
      error: (err) => {
        this.errorMessage = err.error || 'Erreur lors de la modification.';
      }
    });
  }

  // ─── Suppression ─────────────────────────────────────────────────────────
  deleteEducation(eduId: number): void {
    if (!confirm('Supprimer cette formation ?')) return;
    this.profileService.deleteEducation(eduId, this.currentUserId).subscribe({
      next: () => this.loadEducations(),
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression.';
      }
    });
  }

  // ─── Utilitaires ─────────────────────────────────────────────────────────
  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    this.errorMessage = '';
    if (!this.showAddForm) this.resetAddForm();
  }

  private resetAddForm(): void {
    this.newEdu = { degree: '', institution: '', fieldOfStudy: '', graduationYear: null };
  }
}