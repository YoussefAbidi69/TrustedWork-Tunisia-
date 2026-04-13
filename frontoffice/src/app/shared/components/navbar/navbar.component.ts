import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthUser } from '../../../core/models/auth.model';

interface NavChildItem {
  label: string;
  route?: string;
  description?: string;
  badge?: string | number;
  disabled?: boolean;
}

interface NavItem {
  label: string;
  route?: string;
  children?: NavChildItem[];
}

interface QuickSearchItem {
  label: string;
  route: string;
}

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  @Input() showBrand = false;
  @Output() toggleSidebar = new EventEmitter<void>();

  searchOpen = false;
  searchTerm = '';
  currentUser: AuthUser | null = null;

  quickSearchItems: QuickSearchItem[] = [
    { label: 'Mon Profil',     route: '/app/profile/profile-overview' },
    { label: 'KYC',            route: '/app/profile/kyc' },
    { label: 'Trust Passport', route: '/app/profile/trust-passport' },
    { label: 'Paramètres',     route: '/app/profile/settings' }
  ];

  navItems: NavItem[] = [
    {
      label: 'Dashboard',
      route: '/app/dashboard'
    },
    {
      label: 'Profil',
      children: [
        { label: 'Vue générale',   route: '/app/profile/profile-overview', description: 'Résumé de votre profil' },
        { label: 'KYC',            route: '/app/profile/kyc',              description: 'Vérification identité CIN' },
        { label: 'Trust Passport', route: '/app/profile/trust-passport',   description: 'Score de confiance' },
        { label: 'Paramètres',     route: '/app/profile/settings',         description: 'Sécurité et 2FA' }
      ]
    },
    {
      label: 'Marketplace',
      children: [
        { label: 'Offres Freelance', description: 'Missions disponibles',    disabled: true },
         {
          label: 'Contracts',
          route: '/app/activity/contracts',
          description: 'Contrats actifs et en attente'
        },
         {
          label: 'Litiges',
          route: '/app/activity/disputes',
          description: 'Litiges en cours'
        }
      ]
    },
    {
      label: 'Communauté',
      children: [
        { label: 'Événements',  description: 'Hackathons et meetups',  disabled: true },
        { label: 'Challenges',  description: 'Défis et gamification',  disabled: true },
        { label: 'Agences',     description: 'Équipes freelance',      disabled: true }
      ]
    },
     {
      label: 'Business',
      children: [
        {
          label: 'Wallet',
          route: '/app/finance/wallet',
          description: 'Paiements et transactions'
        },
        {
          label: 'Transactions',
          route: '/app/finance/transactions',
          description: 'Suivi détaillé des mouvements financiers'
        },
        {
          label: 'Escrow',
          route: '/app/finance/escrow',
          description: 'Paiements sécurisés en attente'
        },
        {
          label: 'Historique paiements',
          route: '/app/finance/payments-history',
          description: 'Historique complet des règlements'
        },
        {
          label: 'Mes Paiements',
          route: '/app/finance/payment-list',
          description: 'Règlements et sécurisation des contrats'
        }
      ]
    }
  ];

  constructor(public authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentAuthUser();
  }

  hasChildren(item: NavItem): boolean {
    return !!item.children && item.children.length > 0;
  }

  onToggleSidebar(): void { this.toggleSidebar.emit(); }
  toggleSearch(): void { this.searchOpen = !this.searchOpen; }
  closeSearch(): void { this.searchOpen = false; }
  onQuickNavigate(): void { this.closeSearch(); }
  trackByLabel(index: number, item: QuickSearchItem): string { return item.label; }

  onLogout(): void {
    this.closeSearch();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}