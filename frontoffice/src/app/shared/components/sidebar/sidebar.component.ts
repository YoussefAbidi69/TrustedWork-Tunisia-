import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';
import { AuthUser } from '../../../core/models/auth.model';

interface NavItem {
  label: string;
  icon: string;
  route?: string;
  badge?: string;
  disabled?: boolean;
  isLogout?: boolean;
  comingSoon?: boolean;
  roles?: string[]; // ← NOUVEAU : rôles autorisés (vide = tous)
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

  // ── Tous les items avec filtrage par rôle ──
  allActiveItems: NavItem[] = [
    { label: 'Dashboard',      icon: 'fa-house',           route: '/app/dashboard' },
    { label: 'Mon Profil',     icon: 'fa-user',            route: '/app/profile/overview' },
    { label: 'Mes Projets',    icon: 'fa-diagram-project', route: '/app/projects',              roles: ['CLIENT', 'FREELANCER'] },
    { label: 'Tous les Projets', icon: 'fa-folder-tree',   route: '/app/projects',              roles: ['ADMIN'] },
    { label: 'Notifications',  icon: 'fa-bell',            route: '/app/projects/notifications', roles: ['CLIENT', 'FREELANCER'] },
    { label: 'KYC',            icon: 'fa-id-card',         route: '/app/profile/kyc' },
    { label: 'Trust Passport', icon: 'fa-passport',        route: '/app/profile/trust-passport' },
    { label: 'Paramètres',     icon: 'fa-gear',            route: '/app/profile/settings' }
  ];

  comingSoonItems: NavItem[] = [
    { label: 'Offres Freelance', icon: 'fa-briefcase',     comingSoon: true },
    { label: 'Contrats',         icon: 'fa-file-contract', comingSoon: true },
    { label: 'Événements',       icon: 'fa-calendar-days', comingSoon: true },
    { label: 'Wallet',           icon: 'fa-wallet',        comingSoon: true },
    { label: 'Messages',         icon: 'fa-envelope',      comingSoon: true },
    { label: 'Réputation',       icon: 'fa-star',          comingSoon: true },
    { label: 'Agence',           icon: 'fa-building',      comingSoon: true }
  ];

  constructor(
    public authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentAuthUser();

    this.userService.getMyProfile().subscribe({
      next: (data: UserProfileResponse) => {
        this.trustLevel = (data as any).trustLevel ?? 1;
      },
      error: () => {
        this.trustLevel = 1;
      }
    });
  }

  /** Filtre les items selon le rôle de l'utilisateur connecté */
  get activeItems(): NavItem[] {
    const role = this.currentUser?.role?.toUpperCase() || '';
    return this.allActiveItems.filter(item => {
      if (!item.roles || item.roles.length === 0) return true; // visible pour tous
      return item.roles.includes(role);
    });
  }

  onToggleCollapse(): void {
    this.toggleCollapse.emit();
  }

  onLogout(): void {
    this.authService.logout();
    window.location.href = '/';
  }
}