import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthUser } from '../../../core/models/auth.model';

interface NavChild {
  label: string;
  icon: string;
  route: string;
}

interface NavGroup {
  label: string;
  icon: string;
  children: NavChild[];
  expanded?: boolean;
}

/**
 * Sidebar de navigation — frontoffice TrustedWork Tunisia
 * Groupe 1 : Identité & Accès (Module 01)
 * Groupe 2 : Profil Freelancer (Module 02)
 * Groupe 3 : Découvrir (profils publics)
 */
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

  dashboardLink = { label: 'Dashboard', icon: 'fa-house', route: '/app/dashboard' };

  navGroups: NavGroup[] = [
    {
      label: 'Identite et Acces',
      icon: 'fa-shield-halved',
      expanded: false,
      children: [
        { label: 'Vue generale',   icon: 'fa-user',     route: '/app/profile/overview' },
        { label: 'KYC',            icon: 'fa-id-card',  route: '/app/profile/kyc' },
        { label: 'Trust Passport', icon: 'fa-passport', route: '/app/profile/trust-passport' },
        { label: 'Parametres',     icon: 'fa-gear',     route: '/app/profile/settings' }
      ]
    },
    {
      label: 'Profil Freelancer',
      icon: 'fa-briefcase',
      expanded: false,
      children: [
        { label: 'Portfolio',      icon: 'fa-images',         route: '/app/profile/portfolio' },
        { label: 'Experiences',    icon: 'fa-building',       route: '/app/profile/work-experience' },
        { label: 'Formation',      icon: 'fa-graduation-cap', route: '/app/profile/education' },
        { label: 'Skills',         icon: 'fa-code',           route: '/app/profile/skills' },
        { label: 'Certifications', icon: 'fa-certificate',    route: '/app/profile/certifications' },
        { label: 'Endorsements',   icon: 'fa-handshake',      route: '/app/profile/endorsements' },
        { label: 'Avis Clients',   icon: 'fa-star',           route: '/app/profile/reviews' },
        { label: 'Carriere AI',    icon: 'fa-robot',          route: '/app/profile/career-path' }
      ]
    },
    {
      label: 'Decouvrir',
      icon: 'fa-globe',
      expanded: false,
      children: [
        { label: 'Freelancers',    icon: 'fa-users',          route: '/app/freelancers' },
        { label: 'Mon profil pub', icon: 'fa-eye',            route: '' } // route dynamique
      ]
    }
  ];

  comingSoonItems = [
    { label: 'Job Board',  icon: 'fa-magnifying-glass' },
    { label: 'Contrats',   icon: 'fa-file-contract' },
    { label: 'Wallet',     icon: 'fa-wallet' },
    { label: 'Messages',   icon: 'fa-envelope' },
    { label: 'Evenements', icon: 'fa-calendar-days' }
  ];

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private freelancerService: FreelancerProfileService
  ) {}

  ngOnInit(): void {
    this.currentUser   = this.authService.getCurrentAuthUser();
    this.currentUserId = this.currentUser?.userId ?? 0;

    this.userService.getMyProfile().subscribe({
      next: (data: UserProfileResponse) => {
        this.trustLevel    = (data as any).trustLevel ?? 1;
        this.currentUserId = (data as any).id ?? (data as any).userId ?? 0;

        // Mettre à jour le lien "Mon profil pub" dynamiquement
        const decouvrir = this.navGroups.find(g => g.label === 'Decouvrir');
        if (decouvrir) {
          const monProfil = decouvrir.children.find(c => c.label === 'Mon profil pub');
          if (monProfil) {
            monProfil.route = `/app/profile/public/${this.currentUserId}`;
          }
        }
      },
      error: () => { this.trustLevel = 1; }
    });
  }

  toggleGroup(group: NavGroup): void {
    this.navGroups.forEach(g => { if (g !== group) g.expanded = false; });
    group.expanded = !group.expanded;
  }

  onToggleCollapse(): void {
    if (!this.collapsed) { this.navGroups.forEach(g => g.expanded = false); }
    this.toggleCollapse.emit();
  }

  onLogout(): void {
    this.authService.logout();
    window.location.href = '/';
  }
}