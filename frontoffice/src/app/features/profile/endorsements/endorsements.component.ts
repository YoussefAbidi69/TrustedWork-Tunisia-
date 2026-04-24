import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { ApiService } from '../../../core/services/api.service';
import { Skill, Endorsement } from '../../../core/models/freelancer.model';

/**
 * Interface ViewModel pour les endorsements avec infos endorser enrichies
 */
interface EndorsementViewModel extends Endorsement {
  endorserName: string;
  endorserInitials: string;
  endorserRole: string;
}

@Component({
  selector: 'app-endorsements',
  templateUrl: './endorsements.component.html',
  styleUrls: ['./endorsements.component.css']
})
export class EndorsementsComponent implements OnInit {

  // ==========================================
  // DONNÉES DU COMPOSANT
  // ==========================================
  
  /** Liste des skills du freelancer */
  skills: Skill[] = [];
  
  /** Map skillId -> liste des endorsements avec infos endorser */
  endorsementsBySkill: { [skillId: number]: EndorsementViewModel[] } = {};

  // ==========================================
  // ÉTATS UI
  // ==========================================
  
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  // ==========================================
  // CONSTRUCTEUR - Injection des dépendances
  // ==========================================

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService,
    private api: ApiService,
    private cdr: ChangeDetectorRef
  ) {}

  // ==========================================
  // CYCLE DE VIE
  // ==========================================

  ngOnInit(): void {
    this.loadSkills();
  }

  // ==========================================
  // GETTERS - Données calculées
  // ==========================================

  /**
   * Retourne l'ID de l'utilisateur connecté
   */
  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  /**
   * Calcule le total des endorsements tous skills confondus
   */
  get totalEndorsements(): number {
    return this.skills.reduce((sum, skill) => sum + this.getEndorsementCount(skill.id), 0);
  }

  /**
   * Calcule la moyenne d'authenticité sur tous les skills
   */
  get averageAuthenticity(): number {
    if (!this.skills.length) {
      return 0;
    }

    const total = this.skills.reduce((sum, skill) => sum + (skill.authenticityScore || 0), 0);
    return total / this.skills.length;
  }

  /**
   * Compte combien de skills ont au moins un endorsement
   */
  get endorsedSkillsCount(): number {
    return this.skills.filter(skill => this.getEndorsementCount(skill.id) > 0).length;
  }

  // ==========================================
  // MÉTHODES DE CHARGEMENT DES DONNÉES
  // ==========================================

  /**
   * Charge les skills puis déclenchement du chargement des endorsements
   */
  loadSkills(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.profileService.getMySkills(this.currentUserId).subscribe({
      next: (skills) => {
        // Tri par authenticité décroissante
        this.skills = (skills || []).sort(
          (a, b) => (b.authenticityScore || 0) - (a.authenticityScore || 0)
        );

        // Si pas de skills, on arrête là
        if (!this.skills.length) {
          this.isLoading = false;
          return;
        }

        // Sinon on charge tous les endorsements
        this.loadAllEndorsements();
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement des compétences.';
        this.isLoading = false;
        this.autoClearMessages();
      }
    });
  }

  /**
   * Charge les endorsements pour TOUS les skills en parallèle
   * + Récupère les infos des endorsers en parallèle aussi
   */
  loadAllEndorsements(): void {
    // Création des requêtes HTTP pour chaque skill
    const skillRequests = this.skills.map(skill =>
      this.profileService.getEndorsementsBySkill(skill.id).pipe(
        catchError(() => of([] as Endorsement[]))
      )
    );

    // Exécution parallèle de toutes les requêtes
    forkJoin(skillRequests).subscribe({
      next: (allEndorsements) => {
        const allFlat = allEndorsements.flat();
        
        // Extraction des IDs uniques d'endorsers
        const uniqueIds = [...new Set(allFlat.map(e => e.endorserId))];

        // Cas : pas d'endorsers → on applique un fallback simple
        if (!uniqueIds.length) {
          this.skills.forEach((skill, index) => {
            const endorsements = allEndorsements[index] || [];
            this.endorsementsBySkill[skill.id] = endorsements.map(e => this.toFallbackViewModel(e));
          });
          this.isLoading = false;
          this.cdr.detectChanges();
          return;
        }

        // Récupération des infos utilisateurs des endorsers
        const userRequests = uniqueIds.map(userId =>
          this.api.get<any>(`/identity/users/${userId}`).pipe(
            catchError(() => of(null))
          )
        );

        forkJoin(userRequests).subscribe({
          next: (users) => {
            // Construction de la map userId -> user
            const userMap: { [id: number]: any } = {};
            uniqueIds.forEach((id, index) => {
              userMap[id] = users[index];
            });

            // Association endorsements + infos endorsers
            this.skills.forEach((skill, skillIndex) => {
              const endorsements = allEndorsements[skillIndex] || [];

              this.endorsementsBySkill[skill.id] = endorsements.map(e => {
                const user = userMap[e.endorserId];

                // Si pas d'utilisateur trouvé → fallback
                if (!user) {
                  return this.toFallbackViewModel(e);
                }

                // Construction du ViewModel enrichi
                const firstName = (user.firstName || '').trim();
                const lastName = (user.lastName || '').trim();
                const fullName = `${firstName} ${lastName}`.trim();

                return {
                  ...e,
                  endorserName: fullName || `Freelancer #${e.endorserId}`,
                  endorserInitials: this.buildInitials(firstName, lastName, e.endorserId),
                  endorserRole: this.getRoleLabel(user.role)
                };
              });
            });

            this.isLoading = false;
            this.cdr.detectChanges();
          },
          error: () => {
            // En cas d'erreur lors de la récupération des users → fallback
            this.applyFallbackEndorsements(allEndorsements);
          }
        });
      },
      error: () => {
        // Erreur critique : initialisation vide
        this.skills.forEach(skill => {
          this.endorsementsBySkill[skill.id] = [];
        });
        this.isLoading = false;
        this.errorMessage = 'Erreur lors du chargement des endorsements.';
        this.autoClearMessages();
      }
    });
  }

  // ==========================================
  // MÉTHODES PRIVÉES - HELPERS
  // ==========================================

  /**
   * Applique un fallback basique quand on ne peut pas récupérer les infos endorsers
   */
  private applyFallbackEndorsements(allEndorsements: Endorsement[][]): void {
    this.skills.forEach((skill, skillIndex) => {
      const endorsements = allEndorsements[skillIndex] || [];
      this.endorsementsBySkill[skill.id] = endorsements.map(e => this.toFallbackViewModel(e));
    });

    this.isLoading = false;
    this.cdr.detectChanges();
  }

  /**
   * Crée un ViewModel basique sans infos endorser
   */
  private toFallbackViewModel(e: Endorsement): EndorsementViewModel {
    return {
      ...e,
      endorserName: `Freelancer #${e.endorserId}`,
      endorserInitials: `F${e.endorserId}`,
      endorserRole: 'Freelancer'
    };
  }

  // ==========================================
  // MÉTHODES PUBLIQUES - UTILITAIRES TEMPLATE
  // ==========================================

  /**
   * Retourne le nombre d'endorsesments pour un skill donné
   */
  getEndorsementCount(skillId: number): number {
    return this.endorsementsBySkill[skillId]?.length || 0;
  }

  /**
   * Retourne la classe CSS pour le niveau de skill
   */
  getLevelClass(level: string): string {
    const classes: Record<string, string> = {
      JUNIOR: 'level-junior',
      INTERMEDIATE: 'level-intermediate',
      CONFIRMED: 'level-confirmed',
      EXPERT: 'level-expert'
    };
    return classes[level] ?? 'level-junior';
  }

  /**
   * Retourne le label lisible pour le niveau de skill
   */
  getLevelLabel(level: string): string {
    const labels: Record<string, string> = {
      JUNIOR: 'Junior',
      INTERMEDIATE: 'Intermédiaire',
      CONFIRMED: 'Confirmé',
      EXPERT: 'Expert'
    };
    return labels[level] ?? level;
  }

  /**
   * Retourne la classe CSS pour le score d'authenticité
   */
  getAuthenticityClass(score: number): string {
    if (score >= 75) {
      return 'auth-high';
    }
    if (score >= 40) {
      return 'auth-medium';
    }
    return 'auth-low';
  }

  /**
   * Retourne la classe CSS pour le rôle de l'endorser
   */
  getRoleClass(role?: string): string {
    const normalized = (role || '').toUpperCase().trim();

    if (normalized === 'CLIENT') {
      return 'role-client';
    }
    if (normalized === 'FREELANCER') {
      return 'role-freelancer';
    }
    if (normalized === 'ADMIN') {
      return 'role-admin';
    }

    return 'role-utilisateur'; // Default fallback
  }

  /**
   * Retourne le label lisible pour le rôle
   */
  getRoleLabel(role?: string): string {
    const normalized = (role || '').toUpperCase();

    if (normalized === 'CLIENT') {
      return 'Client';
    }
    if (normalized === 'FREELANCER') {
      return 'Freelancer';
    }
    if (normalized === 'ADMIN') {
      return 'Admin';
    }

    return 'Utilisateur';
  }

  /**
   * Construit les initiales à partir du prénom/nom
   */
  buildInitials(firstName: string, lastName: string, fallbackId: number): string {
    const first = (firstName || '').trim();
    const last = (lastName || '').trim();

    // Cas nominal : prénom + nom
    if (first && last) {
      return `${first.charAt(0)}${last.charAt(0)}`.toUpperCase();
    }

    // Cas : seulement prénom
    if (first) {
      return first.charAt(0).toUpperCase();
    }

    // Fallback : ID
    return `F${fallbackId}`;
  }

  // ==========================================
  // TRACKING POUR *ngFor (Performance Angular)
  // ==========================================

  /**
   * Tracking function pour la liste des skills
   */
  trackBySkill(index: number, skill: Skill): number {
    return skill.id;
  }

  /**
   * Tracking function pour la liste des endorsements
   */
  trackByEndorsement(index: number, endorsement: EndorsementViewModel): number {
    return endorsement.id;
  }

  // ==========================================
  // GESTION DES MESSAGES (Toasts)
  // ==========================================

  /**
   * Efface automatiquement les messages après un délai
   */
  autoClearMessages(): void {
    setTimeout(() => {
      this.errorMessage = '';
      this.successMessage = '';
    }, 3500);
  }
}