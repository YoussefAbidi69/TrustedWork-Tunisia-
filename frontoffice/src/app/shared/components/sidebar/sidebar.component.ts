import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';
import { AgencyService } from '../../../features/agencies/services/agency.service';
import { AuthUser } from '../../../core/models/auth.model';
import { AgencyContextDto } from '../../../core/models/agency.model';

interface NavItem {
  label: string;
  icon: string;
  route?: string;
  queryParams?: any;
  badge?: string;
  disabled?: boolean;
  isLogout?: boolean;
  comingSoon?: boolean;
  children?: NavItem[];
  isExpanded?: boolean;
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
    { label: 'Réputation',       icon: 'fa-star',          comingSoon: true }
  ];

  agencyExpanded = true;
  agencyContext: AgencyContextDto | null = null;
  agencyItems: NavItem[] = [];

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private agencyService: AgencyService
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

    if (this.currentUser) {
      this.agencyService.getMyAgencyContext(this.currentUser.userId).subscribe({
        next: (context) => {
          this.agencyContext = context;
          this.buildAgencyMenu();
        },
        error: () => {
          this.buildAgencyMenu(); // Fallback empty
        }
      });
    }
  }

  buildAgencyMenu(): void {
    this.agencyItems = [];
    
    this.agencyItems.push({
      label: 'Accéder aux agences',
      icon: 'fa-building-columns',
      route: '/app/agencies'
    });

    this.agencyItems.push({
      label: 'Invitations',
      icon: 'fa-envelope-open-text',
      route: '/app/agencies/invitations',
      badge: (this.agencyContext?.pendingInvitationCount ?? 0) > 0 ? this.agencyContext?.pendingInvitationCount?.toString() : undefined
    });

    this.agencyItems.push({
      label: 'Messagerie',
      icon: 'fa-comments',
      route: '/app/agencies/chat'
    });

    this.agencyItems.push({
      label: 'Créer une agence',
      icon: 'fa-square-plus',
      route: '/app/agencies/new'
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