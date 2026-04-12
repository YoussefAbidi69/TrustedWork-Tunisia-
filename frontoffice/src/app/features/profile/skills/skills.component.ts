import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { Skill } from '../../../core/models/freelancer.model';


@Component({
  selector: 'app-skills',
  templateUrl: './skills.component.html',
  styleUrls: ['./skills.component.css']
})
export class SkillsComponent implements OnInit {

  skills: Skill[] = [];
  isLoading = false;
  errorMessage = '';
  showForm = false;

  // Formulaire d'ajout
  newSkill: { name: string; examScore: number } = {
    name: '',
    examScore: 0
  };

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadSkills();
  }

  private get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  // Charger les skills depuis le backend
  loadSkills(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.profileService.getMySkills(this.currentUserId).subscribe({
      next: (data) => {
        this.skills = data;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les compétences.';
        this.isLoading = false;
      }
    });
  }

  // Ajouter un skill
  addSkill(): void {
    if (!this.newSkill.name.trim()) return;

    // examScore envoyé entre 0.0 et 1.0
    const payload = {
      name: this.newSkill.name.trim(),
      examScore: this.newSkill.examScore / 100
    };

    this.profileService.addSkill(this.currentUserId, payload).subscribe({
      next: () => {
        this.newSkill = { name: '', examScore: 0 };
        this.showForm = false;
        this.loadSkills();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de l\'ajout du skill.';
      }
    });
  }

  // Supprimer un skill
  deleteSkill(skillId: number): void {
    if (!confirm('Supprimer ce skill ?')) return;
    this.profileService.deleteSkill(skillId, this.currentUserId).subscribe({
      next: () => this.loadSkills(),
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression.';
      }
    });
  }

  // Calculer et afficher le score d'authenticité
  refreshAuthenticity(skillId: number): void {
    this.profileService.getSkillAuthenticity(skillId).subscribe({
      next: () => this.loadSkills(),
      error: () => {
        this.errorMessage = 'Erreur lors du calcul d\'authenticité.';
      }
    });
  }

  // Retourner le label du niveau
  getLevelLabel(level: string): string {
    const labels: Record<string, string> = {
      'JUNIOR': 'Junior',
      'CONFIRMED': 'Confirmé',
      'EXPERT': 'Expert'
    };
    return labels[level] ?? level;
  }

  // Retourner la classe CSS selon le niveau
  getLevelClass(level: string): string {
    const classes: Record<string, string> = {
      'JUNIOR': 'level-junior',
      'CONFIRMED': 'level-confirmed',
      'EXPERT': 'level-expert'
    };
    return classes[level] ?? '';
  }

  // Retourner la classe CSS selon le score d'authenticité
  getAuthenticityClass(score: number): string {
    if (score >= 0.75) return 'auth-high';
    if (score >= 0.40) return 'auth-medium';
    return 'auth-low';
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    this.errorMessage = '';
  }
}