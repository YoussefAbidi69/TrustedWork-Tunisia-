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
  trustLevel = 1; // ← chargé depuis le backend

  // ── Module 01 — actifs ──────────────────────────────
  activeItems: NavItem[] = [
    { label: 'Dashboard',      icon: 'fa-house',    route: '/app/dashboard' },
    { label: 'Mon Profil',     icon: 'fa-user',     route: '/app/profile/overview' },
    { label: 'KYC',            icon: 'fa-id-card',  route: '/app/profile/kyc' },
    { label: 'Trust Passport', icon: 'fa-passport', route: '/app/profile/trust-passport' },
    { label: 'Paramètres',     icon: 'fa-gear',     route: '/app/profile/settings' }
  ];

  // ── Autres modules — désactivés (coming soon) ──────
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

    // Chargement du trust level réel depuis le backend
    this.userService.getMyProfile().subscribe({
      next: (data: UserProfileResponse) => {
        this.trustLevel = (data as any).trustLevel ?? 1;
      },
      error: () => {
        this.trustLevel = 1;
      }
    });
  }

  onToggleCollapse(): void {
    this.toggleCollapse.emit();
  }

  onLogout(): void {
    this.authService.logout();
    // Redirige vers la landing page (frontoffice)
    window.location.href = '/';
  }
}