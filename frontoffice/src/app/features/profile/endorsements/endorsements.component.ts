import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { Skill, Endorsement } from '../../../core/models/freelancer.model';

/**
 * Composant Endorsements — validation des compétences par les pairs
 */
@Component({
  selector: 'app-endorsements',
  templateUrl: './endorsements.component.html',
  styleUrls: ['./endorsements.component.css']
})
export class EndorsementsComponent implements OnInit {

  skills: Skill[] = [];
  endorsementsBySkill: { [skillId: number]: Endorsement[] } = {};
  selectedSkillId: number | null = null;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  // Formulaire d'endorsement
  newEndorsement = {
    comment: ''
  };

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadSkills();
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  loadSkills(): void {
    this.isLoading = true;
    this.profileService.getMySkills(this.currentUserId).subscribe({
      next: (skills) => {
        this.skills = skills;
        this.isLoading = false;
        // Charger les endorsements pour chaque skill
        skills.forEach(skill => this.loadEndorsements(skill.id));
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement des compétences';
        this.isLoading = false;
      }
    });
  }

  loadEndorsements(skillId: number): void {
    this.profileService.getEndorsementsBySkill(skillId).subscribe({
      next: (endorsements) => {
        this.endorsementsBySkill[skillId] = endorsements;
      }
    });
  }

  selectSkill(skillId: number): void {
    this.selectedSkillId = this.selectedSkillId === skillId ? null : skillId;
    this.newEndorsement.comment = '';
  }

  submitEndorsement(skillId: number): void {
    const payload = {
      endorserId: this.currentUserId,
      comment: this.newEndorsement.comment
    };

    this.profileService.addEndorsement(skillId, payload).subscribe({
      next: (endorsement) => {
        if (!this.endorsementsBySkill[skillId]) {
          this.endorsementsBySkill[skillId] = [];
        }
        this.endorsementsBySkill[skillId].push(endorsement);
        // Incrémenter le compteur du skill
        const skill = this.skills.find(s => s.id === skillId);
        if (skill) skill.endorsementCount++;
        this.newEndorsement.comment = '';
        this.selectedSkillId = null;
        this.successMessage = 'Endorsement ajouté avec succès !';
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors de l\'endorsement';
        setTimeout(() => this.errorMessage = '', 3000);
      }
    });
  }

  getEndorsementCount(skillId: number): number {
    return this.endorsementsBySkill[skillId]?.length || 0;
  }

  getLevelColor(level: string): string {
    switch (level) {
      case 'EXPERT':    return '#7c3aed';
      case 'CONFIRMED': return '#2563eb';
      default:          return '#64748b';
    }
  }

  getLevelLabel(level: string): string {
    switch (level) {
      case 'EXPERT':    return '⭐ Expert';
      case 'CONFIRMED': return '✅ Confirmé';
      default:          return '🌱 Junior';
    }
  }
}