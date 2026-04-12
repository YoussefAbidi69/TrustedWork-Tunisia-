import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';
import { DashboardUser } from '../../../core/models/user.model';

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent implements OnInit {

  loadingUser = true;
  userError = '';

  connectedUser: DashboardUser = {
    id: null, fullName: 'Utilisateur', firstName: 'Utilisateur',
    lastName: '', email: '', role: ''
  };

  trustLevel = 1;
  kycStatus = 'PENDING';
  twoFactorEnabled = false;
  livenessPassed = false;

  trends = [
    { label: 'Développement Web',  growth: '+34%', icon: 'fa-code',      hot: true  },
    { label: 'Design UI/UX',       growth: '+28%', icon: 'fa-pen-nib',   hot: true  },
    { label: 'Data Science / AI',  growth: '+51%', icon: 'fa-brain',     hot: true  },
    { label: 'DevOps / Cloud',     growth: '+42%', icon: 'fa-cloud',     hot: false },
    { label: 'Rédaction Content',  growth: '+19%', icon: 'fa-feather',   hot: false },
    { label: 'Marketing Digital',  growth: '+23%', icon: 'fa-chart-bar', hot: false }
  ];

  featuredJobs = [
    { title: 'Développeur Angular Senior', company: 'TechStart Tunis',  budget: '2 500 – 4 000 DT', tags: ['Angular','TypeScript','Spring Boot'], urgency: 'Urgent',  posted: 'Il y a 2h', trustRequired: 3 },
    { title: 'Designer UI/UX — App Mobile', company: 'Fintech Labs',    budget: '1 800 – 2 800 DT', tags: ['Figma','Mobile','Prototypage'],       urgency: '',        posted: 'Il y a 5h', trustRequired: 2 },
    { title: 'Data Analyst — Dashboard',    company: 'Retail Group TN', budget: '3 000 – 5 000 DT', tags: ['Python','Power BI','SQL'],            urgency: 'Premium', posted: 'Il y a 1j', trustRequired: 4 },
    { title: 'Développeur Full Stack',      company: 'Startup Hub Sfax',budget: '2 000 – 3 500 DT', tags: ['React','Node.js','MySQL'],            urgency: '',        posted: 'Il y a 3h', trustRequired: 2 }
  ];

  recentActivity = [
    { icon: 'fa-circle-check',  color: '#22c55e', text: 'KYC approuvé avec succès',     time: "Aujourd'hui"  },
    { icon: 'fa-shield-halved', color: '#3b82f6', text: 'Trust Level mis à jour : 4/5', time: "Aujourd'hui"  },
    { icon: 'fa-id-card',       color: '#f59e0b', text: 'Documents KYC soumis',         time: 'Hier'         },
    { icon: 'fa-user-plus',     color: '#8b5cf6', text: 'Compte créé sur TrustedWork',  time: 'Cette semaine'}
  ];

  platformStats = [
    { label: 'Freelancers actifs',  value: '12 400+', icon: 'fa-users'         },
    { label: 'Missions publiées',   value: '3 200+',  icon: 'fa-briefcase'     },
    { label: 'Contrats signés',     value: '8 900+',  icon: 'fa-file-contract' },
    { label: 'Taux satisfaction',   value: '97%',     icon: 'fa-star'          }
  ];

  constructor(private authService: AuthService, private userService: UserService) {}

  ngOnInit(): void {
    this.initializeConnectedUser();
    this.loadProfileData();
  }

  private initializeConnectedUser(): void {
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.connectedUser = {
        id: authUser.userId,
        fullName: authUser.email,
        firstName: this.extractDisplayName(authUser.email),
        lastName: '', email: authUser.email, role: authUser.role
      };
    }
  }

  private loadProfileData(): void {
    this.loadingUser = true;
    this.userService.getMyProfile().pipe(finalize(() => this.loadingUser = false)).subscribe({
      next: (data: UserProfileResponse) => {
        const firstName = data.firstName || this.connectedUser.firstName;
        const lastName  = data.lastName  || '';
        this.connectedUser = {
          id: (data as any).id ?? this.connectedUser.id,
          fullName: `${firstName} ${lastName}`.trim() || this.connectedUser.fullName,
          firstName, lastName,
          email: data.email || this.connectedUser.email,
          role:  data.role  || this.connectedUser.role,
          cin:   data.cin
        };
        this.trustLevel       = (data as any).trustLevel    ?? 1;
        this.kycStatus        = data.kycStatus               ?? 'PENDING';
        this.twoFactorEnabled = data.twoFactorEnabled        ?? false;
        this.livenessPassed   = (data as any).livenessPassed ?? false;
      },
      error: (err: HttpErrorResponse) => {
        this.userError = 'Impossible de charger le profil.';
        console.error(err);
      }
    });
  }

  private extractDisplayName(email: string): string {
    if (!email) return 'Utilisateur';
    const local = email.split('@')[0];
    return local ? local.charAt(0).toUpperCase() + local.slice(1) : 'Utilisateur';
  }

  get displayName(): string  { return this.connectedUser.firstName || 'Utilisateur'; }
  get displayEmail(): string { return this.connectedUser.email     || ''; }
  get displayRole(): string  { return this.connectedUser.role      || 'MEMBER'; }

  get kycStatusLabel(): string {
    const map: Record<string,string> = { APPROVED:'Approuvé', IN_REVIEW:'En révision', REJECTED:'Rejeté', PENDING:'En attente' };
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
    if (this.kycStatus === 'PENDING')    return { label: 'Soumettre mon KYC',       route: '/app/profile/kyc',           icon: 'fa-id-card'        };
    if (this.kycStatus === 'IN_REVIEW')  return { label: 'Voir mon dossier KYC',    route: '/app/profile/kyc',           icon: 'fa-hourglass-half' };
    if (!this.twoFactorEnabled)          return { label: 'Activer le 2FA',           route: '/app/profile/settings',      icon: 'fa-lock'           };
    if (this.trustLevel < 3)             return { label: 'Voir mon Trust Passport',  route: '/app/profile/trust-passport',icon: 'fa-passport'       };
    return                                      { label: 'Explorer les offres',      route: '/app/dashboard',             icon: 'fa-briefcase'      };
  }

  canApply(trustRequired: number): boolean { return this.trustLevel >= trustRequired; }

  getGreeting(): string {
    const h = new Date().getHours();
    return h < 12 ? 'Bonjour' : h < 18 ? 'Bon après-midi' : 'Bonsoir';
  }
}