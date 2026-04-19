import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, UserProfileResponse } from '../../../core/services/user.service';
import { AgencyService } from '../../../features/agencies/services/agency.service';
import { AgencyInvitationService } from '../../../features/agencies/services/agency-invitation.service';
import { AuthUser } from '../../../core/models/auth.model';

interface NavItem {
  label: string;
  icon: string;
  route?: string;
  badge?: string;
  disabled?: boolean;
  isLogout?: boolean;
  comingSoon?: boolean;
  children?: NavItem[];
  expanded?: boolean;
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
  hasAgencies = false;
  invitationCount = 0;

  activeItems: NavItem[] = [
    { label: 'Dashboard',      icon: 'fa-house',    route: '/app/dashboard' },
    { label: 'Mon Profil',     icon: 'fa-user',     route: '/app/profile/overview' },
    { label: 'KYC',            icon: 'fa-id-card',  route: '/app/profile/kyc' },
    { label: 'Trust Passport', icon: 'fa-passport', route: '/app/profile/trust-passport' },
    { label: 'Paramètres',     icon: 'fa-gear',     route: '/app/profile/settings' },
    { 
      label: 'Agences',        
      icon: 'fa-building', 
      expanded: false,
      children: [
        { label: 'Accéder aux agences', route: '/app/agencies', icon: 'fa-users-rectangle' },
        { label: 'Créer une agence', route: '/app/agencies/create', icon: 'fa-square-plus' },
        { label: 'Invitations', route: '/app/agencies/invitations', icon: 'fa-envelope-open-text' }
      ]
    }
  ];

  comingSoonItems: NavItem[] = [
    { label: 'Offres Freelance', icon: 'fa-briefcase',     comingSoon: true },
    { label: 'Contrats',         icon: 'fa-file-contract', comingSoon: true },
    { label: 'Événements',       icon: 'fa-calendar-days', comingSoon: true },
    { label: 'Wallet',           icon: 'fa-wallet',        comingSoon: true },
    { label: 'Messages',         icon: 'fa-envelope',      comingSoon: true },
    { label: 'Réputation',       icon: 'fa-star',          comingSoon: true }
  ];

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private agencyService: AgencyService,
    private agencyInvitationService: AgencyInvitationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentAuthUser();

    // Re-evaluate expanded state on navigation
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => this.checkActiveRoutes());

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
          this.hasAgencies = context.hasMemberships;
          this.checkActiveRoutes();
        }
      });
      
      this.loadInvitationsCount();
    }
  }

  loadInvitationsCount(): void {
    if (!this.currentUser) return;
    this.agencyInvitationService.getMyInvitations(this.currentUser.userId).subscribe({
      next: (invitations) => {
        this.invitationCount = invitations.filter(inv => inv.status.toString() === 'PENDING').length;
        this.updateBadge();
      }
    });
  }

  updateBadge(): void {
    const agencyItem = this.activeItems.find(i => i.label === 'Agences');
    if (agencyItem && agencyItem.children) {
      const invItem = agencyItem.children.find(c => c.label === 'Invitations');
      if (invItem) {
        invItem.badge = this.invitationCount > 0 ? this.invitationCount.toString() : undefined;
      }
    }
  }

  checkActiveRoutes(): void {
    const currentUrl = this.router.url;
    const agencyItem = this.activeItems.find(i => i.label === 'Agences');
    if (agencyItem && currentUrl.includes('/app/agencies')) {
      agencyItem.expanded = true;
    }
  }

  toggleItem(item: NavItem): void {
    if (item.children && !this.collapsed) {
      item.expanded = !item.expanded;
    }
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