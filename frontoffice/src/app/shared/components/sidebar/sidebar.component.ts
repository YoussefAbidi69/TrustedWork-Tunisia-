import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthUser } from '../../../core/models/auth.model';

interface NavChild {
  label: string;
  icon: string;
  route: string;
  isDynamic?: boolean;
}

interface NavGroup {
  label: string;
  icon: string;
  children: NavChild[];
  expanded?: boolean;
}

interface ComingSoonItem {
  label: string;
  icon: string;
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  @Input() collapsed = true;
  @Output() toggleCollapse = new EventEmitter<void>();

  currentUser: AuthUser | null = null;
  trustLevel = 1;
  currentUserId = 0;

  displayName = 'Utilisateur';
  roleLabel = 'Freelancer';
  publicProfileReady = false;
  avatarUrl: string | null = null;

  dashboardLink = {
    label: 'Dashboard',
    icon: 'fa-house',
    route: '/app/dashboard'
  };

  navGroups: NavGroup[] = [
    {
      label: 'Identité & Accès',
      icon: 'fa-shield-halved',
      expanded: true,
      children: [
        { label: 'Vue générale',   icon: 'fa-user',     route: '/app/profile/overview'       },
        { label: 'KYC',            icon: 'fa-id-card',  route: '/app/profile/kyc'            },
        { label: 'Trust Passport', icon: 'fa-passport', route: '/app/profile/trust-passport' },
        { label: 'Paramètres',     icon: 'fa-gear',     route: '/app/profile/settings'       }
      ]
    },
    {
      label: 'Profil Freelancer',
      icon: 'fa-briefcase',
      expanded: false,
      children: [
        { label: 'Portfolio',      icon: 'fa-images',          route: '/app/profile/portfolio'       },
        { label: 'Expériences',    icon: 'fa-building',        route: '/app/profile/work-experience' },
        { label: 'Skills',         icon: 'fa-code',            route: '/app/profile/skills'          },
        { label: 'Formations',     icon: 'fa-graduation-cap',  route: '/app/profile/education'       },
        { label: 'Certifications', icon: 'fa-certificate',     route: '/app/profile/certifications'  },
        { label: 'Endorsements',   icon: 'fa-handshake',       route: '/app/profile/endorsements'    },
        { label: 'Avis Clients',   icon: 'fa-star',            route: '/app/profile/reviews'         },
        { label: 'Analytics', icon: 'fa-chart-line', route: '/app/profile/analytics' },
        { label: 'Carrière AI',    icon: 'fa-robot',           route: '/app/profile/career-path'     }
      ]
    },
    {
      label: 'Découvrir',
      icon: 'fa-globe',
      expanded: false,
      children: [
        { label: 'Freelancers',        icon: 'fa-users', route: '/app/freelancers' },
        { label: 'Mon profil public',  icon: 'fa-eye',   route: '', isDynamic: true }
      ]
    },
    {
      label: 'Plateforme',
      icon: 'fa-laptop-code',
      expanded: false,
      children: [
        { label: 'Contrats',       icon: 'fa-file-contract',     route: '/app/activity/contracts'       },
        { label: 'Litiges',        icon: 'fa-scale-balanced',    route: '/app/activity/disputes'        },
        { label: 'Wallet',         icon: 'fa-wallet',            route: '/app/finance/wallet'           },
        { label: 'Historique',     icon: 'fa-clock-rotate-left', route: '/app/finance/payments-history' },
        { label: 'Recommendations',icon: 'fa-wand-magic-sparkles', route: '/app/recommendation'         }
      ]
    },
    {
      label: 'Gestion de Projets',
      icon: 'fa-diagram-project',
      expanded: false,
      children: [
        { label: 'Mes Projets',      icon: 'fa-folder-open',    route: '/app/projects'               },
        { label: 'Notifications',    icon: 'fa-bell',           route: '/app/projects/notifications' }
      ]
    },
    {
      label: 'Engagement & Gamification',
      icon: 'fa-gamepad',
      expanded: false,
      children: [
        { label: 'Tableau de bord', icon: 'fa-chart-pie', route: '/app/engagement/overview' },
        { label: 'Événements',      icon: 'fa-calendar-check', route: '/app/engagement/events' },
        { label: 'Challenges',      icon: 'fa-tasks', route: '/app/engagement/missions' },
        { label: 'Leaderboard',     icon: 'fa-trophy', route: '/app/engagement/leaderboard' },
        { label: 'Gamification',    icon: 'fa-medal', route: '/app/engagement/gamification' }
      ]
    }
  ];

  comingSoonItems: ComingSoonItem[] = [
    { label: 'Job Board',   icon: 'fa-magnifying-glass' },
    { label: 'Messages',    icon: 'fa-envelope'         },
    { label: 'Évènements',  icon: 'fa-calendar-days'    }
  ];

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private freelancerService: FreelancerProfileService
  ) {}

  ngOnInit(): void {
    this.currentUser   = this.authService.getCurrentAuthUser();
    this.currentUserId = this.currentUser?.userId ?? 0;

    // Valeur initiale depuis le token (peut être l'email si le nom est absent)
    this.displayName = this.resolveNameFromAuthUser(this.currentUser);
    this.roleLabel   = this.resolveRoleLabel(this.currentUser?.role);

    // ✅ Activer le bouton IMMÉDIATEMENT — userId disponible depuis le token JWT
    if (this.currentUserId > 0) {
      this.activerBoutonProfilPublic(this.currentUserId);
    }

    // Récupérer le vrai nom ET la photo depuis /users/me
    this.userService.getMyProfile().subscribe({
      next: (user: any) => {
        const resolved = this.resolveNameFromProfile(user);
        if (resolved && resolved.trim().length > 0) {
          this.displayName = resolved;
        }
        if (user?.photo) {
          this.avatarUrl = this.resolvePhotoUrl(String(user.photo));
        }
      },
      error: () => { /* conserve la valeur initiale */ }
    });

    // Fallback photo depuis le profil freelancer
    if (this.currentUserId > 0) {
      this.freelancerService.getProfileByUserId(this.currentUserId).subscribe({
        next: (profile) => {
          if (!this.avatarUrl && profile?.avatarUrl) {
            this.avatarUrl = this.resolvePhotoUrl(profile.avatarUrl);
          }
        },
        error: () => {}
      });
    }

    // Charger le trustLevel via l'endpoint PUBLIC /users/{id}/trust-level
    if (this.currentUserId > 0) {
      this.freelancerService.getUserTrustLevel(this.currentUserId).subscribe({
        next: (res: any) => {
          this.trustLevel = Number(res?.trustLevel ?? 1);
        },
        error: () => {
          this.trustLevel = 1;
        }
      });
    }
  }

  /**
   * Active le lien "Mon profil public" dans la sidebar.
   * Appelé dès que le userId est connu depuis le token JWT.
   */
  private activerBoutonProfilPublic(userId: number): void {
    const discoverGroup     = this.navGroups.find(g => g.label === 'Découvrir');
    const publicProfileItem = discoverGroup?.children.find(c => c.label === 'Mon profil public');
    if (publicProfileItem && userId > 0) {
      publicProfileItem.route = `/app/profile/public/${userId}`;
      this.publicProfileReady = true;
    }
  }

  get trustPercent(): number {
    return (Math.max(0, Math.min(5, this.trustLevel)) / 5) * 100;
  }

  get visibleTrustLabel(): string {
    if (this.trustLevel >= 4.5) return 'Excellent';
    if (this.trustLevel >= 3.5) return 'Solide';
    if (this.trustLevel >= 2.5) return 'En progression';
    return 'Débutant';
  }

  get initials(): string {
    const authInitials = this.authService.getUserInitials?.();
    if (authInitials && authInitials.trim()) {
      return authInitials.trim().slice(0, 2).toUpperCase();
    }
    const parts = this.displayName.split(' ').filter(Boolean);
    if (!parts.length) return 'U';
    return parts.slice(0, 2).map(p => p.charAt(0).toUpperCase()).join('');
  }

  get totalNavItems(): number {
    return this.navGroups.reduce((total, group) => total + group.children.length, 0);
  }

  toggleGroup(group: NavGroup): void {
    if (this.collapsed) {
      this.toggleCollapse.emit();
      setTimeout(() => {
        this.navGroups.forEach(g => { g.expanded = g === group; });
      }, 50);
      return;
    }
    this.navGroups.forEach(g => { if (g !== group) g.expanded = false; });
    group.expanded = !group.expanded;
  }

  onToggleCollapse(): void {
    if (!this.collapsed) {
      this.navGroups.forEach(group => (group.expanded = false));
    }
    this.toggleCollapse.emit();
  }

  onLogout(): void {
    this.authService.logout();
    window.location.href = '/';
  }

  trackByGroup(_: number, group: NavGroup): string  { return group.label; }
  trackByChild(_: number, child: NavChild): string  { return `${child.label}-${child.route}`; }

  private activerBoutonProfilPublicInternal(userId: number): void {
    this.activerBoutonProfilPublic(userId);
  }

  private resolveNameFromAuthUser(user: AuthUser | null): string {
    if (!user) return 'Utilisateur';
    const possibleValues = [
      (user as any).fullName,
      `${(user as any).firstName  ?? ''} ${(user as any).lastName  ?? ''}`.trim(),
      `${(user as any).firstname  ?? ''} ${(user as any).lastname  ?? ''}`.trim(),
      `${(user as any).prenom     ?? ''} ${(user as any).nom       ?? ''}`.trim(),
      (user as any).name
    ];
    const found = possibleValues.find(v => !!v && String(v).trim().length > 0);
    return found ? String(found).trim() : (user.email ?? 'Utilisateur');
  }

  private resolveNameFromProfile(profile: UserProfileResponse | null | undefined): string {
    if (!profile) return '';
    const candidates = [
      (profile as any).fullName,
      `${(profile as any).firstName ?? ''} ${(profile as any).lastName ?? ''}`.trim(),
      `${(profile as any).firstname ?? ''} ${(profile as any).lastname ?? ''}`.trim(),
      `${(profile as any).prenom    ?? ''} ${(profile as any).nom      ?? ''}`.trim(),
      (profile as any).name
    ];
    const match = candidates.find(v => !!v && String(v).trim().length > 0);
    return match ? String(match).trim() : '';
  }

  private resolvePhotoUrl(photo: string): string {
    if (!photo) return '';
    if (photo.startsWith('http://') || photo.startsWith('https://') || photo.startsWith('data:')) {
      return photo;
    }
    return `/api${photo}`;
  }

  private resolveRoleLabel(role: string | undefined | null): string {
    switch ((role ?? '').toString().toUpperCase()) {
      case 'ADMIN':      return 'Administrateur';
      case 'CLIENT':     return 'Client';
      case 'FREELANCER': return 'Freelancer';
      default:           return 'Membre TrustedWork';
    }
  }
}