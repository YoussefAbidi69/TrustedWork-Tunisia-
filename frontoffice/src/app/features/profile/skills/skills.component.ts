import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { Skill, SkillCategory } from '../../../core/models/freelancer.model';

@Component({
  selector: 'app-skills',
  templateUrl: './skills.component.html',
  styleUrls: [
    './skills.component.css',           // ✅ Votre CSS existant
  ]
})
export class SkillsComponent implements OnInit {
  skills: Skill[] = [];
  isLoading = false;
  isSaving = false;

  errorMessage = '';
  successMessage = '';
  showForm = false;

  deletingSkillId: number | null = null;
  refreshingSkillId: number | null = null;

  // ── Exam score inline edit ──────────────────────────────────
  editingExamSkillId: number | null = null;
  editingExamScore: number = 0;
  isSavingExam = false;
  // ────────────────────────────────────────────────────────────

  categoryOptions: { value: SkillCategory; label: string }[] = [
    { value: 'FRONTEND',  label: 'Frontend' },
    { value: 'BACKEND',   label: 'Backend' },
    { value: 'FULLSTACK', label: 'Fullstack' },
    { value: 'MOBILE',    label: 'Mobile' },
    { value: 'DEVOPS',    label: 'DevOps' },
    { value: 'CLOUD',     label: 'Cloud' },
    { value: 'DATA',      label: 'Data' },
    { value: 'AI',        label: 'AI / Machine Learning' },
    { value: 'DESIGN',    label: 'Design' },
    { value: 'SECURITY',  label: 'Security' },
    { value: 'OTHER',     label: 'Other' }
  ];

  newSkill: { name: string; category: SkillCategory; examScore: number } = {
    name: '',
    category: 'BACKEND',
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

  get canSave(): boolean {
    return !!this.newSkill.name.trim()
      && !!this.newSkill.category
      && this.newSkill.examScore >= 0
      && this.newSkill.examScore <= 100
      && !this.isSaving;
  }

  get averageAuthenticity(): number {
    if (!this.skills.length) return 0;
    const total = this.skills.reduce((sum, s) => sum + (s.authenticityScore || 0), 0);
    return total / this.skills.length;
  }

  get totalEndorsements(): number {
    return this.skills.reduce((sum, s) => sum + (s.endorsementCount || 0), 0);
  }

  get topSkill(): Skill | null {
    return this.skills.length ? this.skills[0] : null;
  }

  loadSkills(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.profileService.getMySkills(this.currentUserId).subscribe({
      next: (data) => {
        this.skills = (data || []).sort(
          (a, b) => (b.authenticityScore || 0) - (a.authenticityScore || 0)
        );
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les compétences.';
        this.isLoading = false;
        this.autoClearMessages();
      }
    });
  }

  openForm(): void {
    this.clearMessages();
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.resetForm();
  }

  addSkill(): void {
    if (!this.canSave) return;
    this.clearMessages();
    this.isSaving = true;

    this.profileService.addSkill(this.currentUserId, {
      name:      this.newSkill.name.trim(),
      category:  this.newSkill.category,
      examScore: this.newSkill.examScore
    }).subscribe({
      next: () => {
        this.isSaving = false;
        this.successMessage = 'Compétence ajoutée avec succès.';
        this.resetForm();
        this.showForm = false;
        this.loadSkills();
        this.autoClearMessages();
      },
      error: () => {
        this.isSaving = false;
        this.errorMessage = "Erreur lors de l'ajout du skill.";
        this.autoClearMessages();
      }
    });
  }

  deleteSkill(skillId: number): void {
    if (!confirm('Supprimer ce skill ?')) return;
    this.clearMessages();
    this.deletingSkillId = skillId;

    this.profileService.deleteSkill(skillId, this.currentUserId).subscribe({
      next: () => {
        this.deletingSkillId = null;
        this.successMessage = 'Compétence supprimée avec succès.';
        this.loadSkills();
        this.autoClearMessages();
      },
      error: () => {
        this.deletingSkillId = null;
        this.errorMessage = 'Erreur lors de la suppression.';
        this.autoClearMessages();
      }
    });
  }

  refreshAuthenticity(skillId: number): void {
    this.clearMessages();
    this.refreshingSkillId = skillId;

    this.profileService.getSkillAuthenticity(skillId).subscribe({
      next: () => {
        this.refreshingSkillId = null;
        this.successMessage = "Score d'authenticité recalculé.";
        this.loadSkills();
        this.autoClearMessages();
      },
      error: () => {
        this.refreshingSkillId = null;
        this.errorMessage = "Erreur lors du calcul d'authenticité.";
        this.autoClearMessages();
      }
    });
  }

  // ── Exam score inline edit ──────────────────────────────────

  startEditExam(skill: Skill): void {
    this.editingExamSkillId = skill.id;
    this.editingExamScore   = skill.examScore || 0;
    this.clearMessages();
  }

  cancelEditExam(): void {
    this.editingExamSkillId = null;
    this.editingExamScore   = 0;
  }

  saveExamScore(skillId: number): void {
    if (this.editingExamScore < 0 || this.editingExamScore > 100) {
      this.errorMessage = 'Le score doit être entre 0 et 100.';
      return;
    }

    this.isSavingExam = true;
    this.clearMessages();

    this.profileService.updateExamScore(
      skillId,
      this.currentUserId,
      this.editingExamScore
    ).subscribe({
      next: (updated) => {
        // Mise à jour locale sans reload complet
        const skill = this.skills.find(s => s.id === skillId);
        if (skill) skill.examScore = updated.examScore;

        this.isSavingExam       = false;
        this.editingExamSkillId = null;
        this.successMessage     = "Score d'examen mis à jour.";
        this.autoClearMessages();

        // Recalcul authenticité automatique après mise à jour exam
        this.refreshAuthenticity(skillId);
      },
      error: () => {
        this.isSavingExam = false;
        this.errorMessage = 'Impossible de mettre à jour le score.';
        this.autoClearMessages();
      }
    });
  }

  // ────────────────────────────────────────────────────────────

  resetForm(): void {
    this.newSkill = { name: '', category: 'BACKEND', examScore: 0 };
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

  getLevelLabel(level: string): string {
    const labels: Record<string, string> = {
      JUNIOR: 'Junior', INTERMEDIATE: 'Intermédiaire',
      CONFIRMED: 'Confirmé', EXPERT: 'Expert'
    };
    return labels[level] ?? level;
  }

  getLevelClass(level: string): string {
    const classes: Record<string, string> = {
      JUNIOR: 'level-junior', INTERMEDIATE: 'level-intermediate',
      CONFIRMED: 'level-confirmed', EXPERT: 'level-expert'
    };
    return classes[level] ?? '';
  }

  getAuthenticityClass(score: number): string {
    if (score >= 75) return 'auth-high';
    if (score >= 40) return 'auth-medium';
    return 'auth-low';
  }

  getCategoryLabel(category: SkillCategory): string {
    const labels: Record<SkillCategory, string> = {
      FRONTEND: 'Frontend', BACKEND: 'Backend', FULLSTACK: 'Fullstack',
      MOBILE: 'Mobile', DEVOPS: 'DevOps', CLOUD: 'Cloud', DATA: 'Data',
      AI: 'AI / ML', DESIGN: 'Design', SECURITY: 'Security', OTHER: 'Other'
    };
    return labels[category] ?? category;
  }

  getCategoryClass(category: SkillCategory): string {
    return `category-${category.toLowerCase()}`;
  }

  trackBySkill(index: number, skill: Skill): number {
    return skill.id;
  }
}