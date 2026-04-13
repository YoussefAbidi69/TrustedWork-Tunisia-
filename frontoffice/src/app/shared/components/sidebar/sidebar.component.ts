import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthUser } from '../../../core/models/auth.model';

interface QuickNavItem {
  label: string;
  icon: string;
  route?: string;
  badge?: string;
  disabled?: boolean;
  isLogout?: boolean;
  isHeader?: boolean;
  roles?: string[];
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

  quickItems: QuickNavItem[] = [
    {
      label: 'Main Dashboard',
      icon: 'fa-house',
      route: '/app/dashboard'
    },
    // SECTION: PROFILE
    { label: 'Personnel', icon: '', isHeader: true },
    {
      label: 'Profil',
      icon: 'fa-user',
      route: '/app/profile/profile-overview'
    },
    {
      label: 'Certifications',
      icon: 'fa-certificate',
      route: '/app/profile/certifications'
    },
    {
      label: 'Settings',
      icon: 'fa-gear',
      route: '/app/profile/settings'
    },
    // SECTION: REPUTATION
    { label: 'Réputation', icon: '', isHeader: true },
    {
      label: 'Trust Score',
      icon: 'fa-shield-halved',
      route: '/app/reputation/trust-score'
    },
    {
      label: 'Badges & XP',
      icon: 'fa-medal',
      route: '/app/reputation/badges-xp'
    },
    {
      label: 'Reviews',
      icon: 'fa-star',
      route: '/app/reputation/reviews'
    },
    // SECTION: OPPORTUNITIES
    { label: 'Opportunités', icon: '', isHeader: true },
    {
      label: 'Freelance Jobs',
      icon: 'fa-briefcase',
      route: '/app/opportunities/freelance-jobs'
    },
    {
      label: 'Recruitment',
      icon: 'fa-user-tie',
      route: '/app/opportunities/recruitment-jobs'
    },
    {
      label: 'Events & Challenges',
      icon: 'fa-bolt',
      route: '/app/opportunities/challenges'
    },
    // SECTION: BUSINESS (Standardised as per user request)
    { label: 'Business', icon: '', isHeader: true },
    {
      label: 'Contrats',
      icon: 'fa-file-contract',
      route: '/app/activity/contracts'
    },
    {
      label: 'Litiges',
      icon: 'fa-scale-balanced',
      route: '/app/activity/disputes'
    },
    {
      label: 'Portefeuille',
      icon: 'fa-wallet',
      route: '/app/finance/wallet'
    },
    {
      label: 'Transactions',
      icon: 'fa-arrow-right-arrow-left',
      route: '/app/finance/transactions'
    },
    {
      label: 'Escrow',
      icon: 'fa-lock',
      route: '/app/finance/escrow',
      roles: ['ADMIN']
    },
    {
      label: 'Historique de paiement',
      icon: 'fa-receipt',
      route: '/app/finance/payments-history'
    },
    {
      label: 'Support',
      icon: 'fa-life-ring',
      route: '/app/support/reclamations'
    },
    {
      label: 'Messages',
      icon: 'fa-envelope',
      route: '/app/messages',
      badge: '2'
    }
  ];

  logoutItem: QuickNavItem = {
    label: 'Déconnexion',
    icon: 'fa-right-from-bracket',
    isLogout: true
  };

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentAuthUser();
  }

  get filteredQuickItems(): QuickNavItem[] {
    return this.quickItems.filter(item => {
      if (!item.roles) return true;
      if (!this.currentUser) return false;
      return item.roles.includes(this.currentUser.role);
    });
  }

  onToggleCollapse(): void {
    this.toggleCollapse.emit();
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}