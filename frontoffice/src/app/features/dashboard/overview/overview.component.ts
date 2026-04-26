import { Component, OnInit } from '@angular/core';
import { finalize, catchError, of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { DashboardUser } from '../../../core/models/user.model';
import { FreelancerProfile } from '../../../core/models/freelancer.model';

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent implements OnInit {

  /* ─── États de chargement ─── */
  loadingUser        = true;
  loadingFreelancers = true;
  userError          = '';

  /* ─── Données utilisateur connecté ─── */
  connectedUser: DashboardUser = {
    id: null, fullName: 'Utilisateur', firstName: 'Utilisateur',
    lastName: '', email: '', role: ''
  };

  trustLevel       = 1;
  kycStatus        = 'PENDING';
  twoFactorEnabled = false;
  livenessPassed   = false;

  /* ─── Top Freelancers ─── */
  topFreelancers: FreelancerProfile[] = [];

  /* ─── Tendances marché (statiques) ─── */
  trends = [
    { label: 'Développement Web',  growth: 34, icon: 'fa-code',      hot: true  },
    { label: 'Design UI/UX',       growth: 28, icon: 'fa-pen-nib',   hot: true  },
    { label: 'Data Science / AI',  growth: 51, icon: 'fa-brain',     hot: true  },
    { label: 'DevOps / Cloud',     growth: 42, icon: 'fa-cloud',     hot: false },
    { label: 'Rédaction Content',  growth: 19, icon: 'fa-feather',   hot: false },
    { label: 'Marketing Digital',  growth: 23, icon: 'fa-chart-bar', hot: false }
  ];

  /* ─── Offres recommandées (statiques — module Job Board = module 03) ─── */
  featuredJobs = [
    { title: 'Développeur Angular Senior',   company: 'TechStart Tunis',  budget: '2 500 – 4 000 DT', tags: ['Angular','TypeScript','Spring Boot'], urgency: 'Urgent',  posted: 'Il y a 2h', trustRequired: 3 },
    { title: 'Designer UI/UX — App Mobile',  company: 'Fintech Labs',     budget: '1 800 – 2 800 DT', tags: ['Figma','Mobile','Prototypage'],       urgency: '',        posted: 'Il y a 5h', trustRequired: 2 },
    { title: 'Data Analyst — Dashboard BI',  company: 'Retail Group TN',  budget: '3 000 – 5 000 DT', tags: ['Python','Power BI','SQL'],            urgency: 'Premium', posted: 'Il y a 1j', trustRequired: 4 },
    { title: 'Développeur Full Stack React', company: 'Startup Hub Sfax', budget: '2 000 – 3 500 DT', tags: ['React','Node.js','MySQL'],            urgency: '',        posted: 'Il y a 3h', trustRequired: 2 }
  ];

  /* ─── Activité récente (statique) ─── */
  recentActivity = [
    { icon: 'fa-circle-check',  color: '#22c55e', text: 'KYC approuvé avec succès',     time: "Aujourd'hui"   },
    { icon: 'fa-shield-halved', color: '#3b82f6', text: 'Trust Level mis à jour : 4/5', time: "Aujourd'hui"   },
    { icon: 'fa-id-card',       color: '#f59e0b', text: 'Documents KYC soumis',          time: 'Hier'          },
    { icon: 'fa-user-plus',     color: '#8b5cf6', text: 'Compte créé sur TrustedWork',  time: 'Cette semaine' }
  ];

  /* ─── Stats plateforme (statiques) ─── */
  platformStats = [
    { label: 'Freelancers actifs', value: '12 400+', icon: 'fa-users',         color: '#f97316' },
    { label: 'Missions publiées',  value: '3 200+',  icon: 'fa-briefcase',     color: '#3b82f6' },
    { label: 'Contrats signés',    value: '8 900+',  icon: 'fa-file-contract', color: '#22c55e' },
    { label: 'Taux satisfaction',  value: '97%',     icon: 'fa-star',          color: '#f59e0b' }
  ];

  constructor(
    private readonly authService: AuthService,
    private readonly freelancerService: FreelancerProfileService
  ) {}

  ngOnInit(): void {
    this.initializeConnectedUser();
    this.loadProfileData();
    this.loadTopFreelancers();
  }

  /* ─── Initialisation rapide depuis le token JWT — aucun appel API ─── */
  private initializeConnectedUser(): void {
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.connectedUser = {
        id:        authUser.userId,
        fullName:  this.extractDisplayName(authUser.email),
        firstName: this.extractDisplayName(authUser.email),
        lastName:  '',
        email:     authUser.email,
        role:      authUser.role
      };
    }
  }

  /**
   * Chargement du trustLevel via l'endpoint public /users/{id}/trust-level.
   * ✅ Endpoint en permitAll() — pas de risque de 403.
   * Remplace l'ancien appel à /users/me qui retournait 403.
   */
  private loadProfileData(): void {
    const authUser = this.authService.getCurrentAuthUser();

    if (!authUser?.userId) {
      this.loadingUser = false;
      return;
    }

    this.loadingUser = true;

    this.freelancerService.getUserTrustLevel(authUser.userId)
      .pipe(
        catchError(() => of({ userId: authUser.userId, trustLevel: 1 })),
        finalize(() => this.loadingUser = false)
      )
      .subscribe((data: any) => {
        this.trustLevel = Number(data?.trustLevel ?? 1);
      });
  }

  /* ─── Chargement des top freelancers depuis l'API réelle ─── */
  private loadTopFreelancers(): void {
    this.loadingFreelancers = true;
    this.freelancerService.getAllPublicProfiles()
      .pipe(
        catchError(() => of([])),
        finalize(() => this.loadingFreelancers = false)
      )
      .subscribe((profiles: FreelancerProfile[]) => {
        this.topFreelancers = profiles
          .filter(p => p.visibility === 'PUBLIC')
          .sort((a, b) => (b.completenessScore ?? 0) - (a.completenessScore ?? 0))
          .slice(0, 4);
      });
  }

  /* ─── Utilitaires ─── */
  private extractDisplayName(email: string): string {
    if (!email) return 'Utilisateur';
    const local = email.split('@')[0];
    return local ? local.charAt(0).toUpperCase() + local.slice(1) : 'Utilisateur';
  }

  get displayName(): string  { return this.connectedUser.firstName || 'Utilisateur'; }
  get displayEmail(): string { return this.connectedUser.email     || ''; }
  get displayRole(): string  { return this.connectedUser.role      || 'MEMBER'; }

  get kycStatusLabel(): string {
    const map: Record<string, string> = {
      APPROVED: 'Approuvé', IN_REVIEW: 'En révision',
      REJECTED: 'Rejeté',   PENDING: 'En attente'
    };
    return map[this.kycStatus] || this.kycStatus;
  }

  get profileCompletionScore(): number {
    let s = 20;
    if (this.kycStatus !== 'PENDING')  s += 20;
    if (this.kycStatus === 'APPROVED') s += 20;
    if (this.twoFactorEnabled)         s += 20;
    if (this.livenessPassed)           s += 20;
    return s;
  }

  get nextAction(): { label: string; route: string; icon: string } {
    if (this.kycStatus === 'PENDING')   return { label: 'Soumettre mon KYC',      route: '/app/profile/kyc',            icon: 'fa-id-card'        };
    if (this.kycStatus === 'IN_REVIEW') return { label: 'Voir mon dossier KYC',   route: '/app/profile/kyc',            icon: 'fa-hourglass-half' };
    if (!this.twoFactorEnabled)         return { label: 'Activer le 2FA',          route: '/app/profile/settings',       icon: 'fa-lock'           };
    if (this.trustLevel < 3)            return { label: 'Voir mon Trust Passport', route: '/app/profile/trust-passport', icon: 'fa-passport'       };
    return                                     { label: 'Explorer les offres',     route: '/app/dashboard',              icon: 'fa-briefcase'      };
  }

  canApply(trustRequired: number): boolean { return this.trustLevel >= trustRequired; }

  getAvailabilityLabel(status: string): string {
    const map: Record<string, string> = {
      AVAILABLE: 'Disponible', BUSY: 'Occupé', ON_VACATION: 'En congé'
    };
    return map[status] || status;
  }

  getAvailabilityColor(status: string): string {
    const map: Record<string, string> = {
      AVAILABLE: '#22c55e', BUSY: '#ef4444', ON_VACATION: '#f59e0b'
    };
    return map[status] || '#94a3b8';
  }

  getInitials(headline: string): string {
    if (!headline) return '?';
    const parts = headline.split(' ');
    return parts.length >= 2
      ? (parts[0][0] + parts[1][0]).toUpperCase()
      : headline.substring(0, 2).toUpperCase();
  }

  getGreeting(): string {
    const h = new Date().getHours();
    if (h < 12) { return 'Bonjour'; }
    if (h < 18) { return 'Bon après-midi'; }
    return 'Bonsoir';
  }

  getTrendBarWidth(growth: number): string {
    return Math.min(growth * 1.5, 100) + '%';
  }
}
