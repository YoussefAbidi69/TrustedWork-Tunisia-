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
    { label: 'Trust Score', route: '/app/reputation/trust-score' },
    { label: 'Messages', route: '/app/messages' },
    { label: 'Wallet', route: '/app/finance/wallet' },
    { label: 'Opportunités', route: '/app/opportunities/freelance-jobs' }
  ];

  navItems: NavItem[] = [
    {
      label: 'Dashboard',
      route: '/app/dashboard'
    },
    {
      label: 'Profil',
      children: [
        {
          label: 'Vue générale',
          route: '/app/profile/profile-overview',
          description: 'Résumé complet du profil'
        },
        {
          label: 'Certifications',
          route: '/app/profile/certifications',
          description: 'Certificats et validations'
        },
        {
          label: 'Skills',
          route: '/app/profile/skills',
          description: 'Compétences et expertise'
        },
        {
          label: 'Settings',
          route: '/app/profile/settings',
          description: 'Préférences du compte'
        },
        {
          label: 'KYC',
          route: '/app/profile/kyc',
          description: 'Vérification identité'
        },
        {
          label: 'Trust Passport',
          route: '/app/profile/trust-passport',
          description: 'Passeport de confiance'
        }
      ]
    },
    {
      label: 'Réputation',
      children: [
        {
          label: 'Trust Score',
          route: '/app/reputation/trust-score',
          description: 'Score global de confiance'
        },
        {
          label: 'Badges',
          route: '/app/reputation/badges',
          description: 'Collection de badges et distinctions'
        },
        {
          label: 'Badges & XP',
          route: '/app/reputation/badges-xp',
          description: 'Progression et récompenses'
        },
        {
          label: 'Avis reçus',
          route: '/app/reputation/reviews',
          description: 'Retours clients et missions',
          badge: 12
        },
        {
          label: 'Historique',
          route: '/app/reputation/history',
          description: 'Chronologie réputationnelle'
        },
        {
          label: 'Progression',
          route: '/app/reputation/progression',
          description: 'Évolution de votre réputation'
        }
      ]
    },
    {
      label: 'Activité',
      children: [
        {
          label: 'Freelance jobs',
          route: '/app/opportunities/freelance-jobs',
          description: 'Missions freelance recommandées'
        },
        {
          label: 'Recruitment overview',
          route: '/app/recruitment/overview',
          description: 'Vue globale recrutement long terme'
        },
        {
          label: 'Recruitment jobs',
          route: '/app/opportunities/recruitment-jobs',
          description: 'Opportunités long terme et recrutement'
        },
        {
          label: 'Events overview',
          route: '/app/opportunities/events-overview',
          description: 'Vue premium des événements à venir'
        },
        {
          label: 'Events list',
          route: '/app/opportunities/events-list',
          description: 'Liste complète des événements'
        },
        {
          label: 'Challenges',
          route: '/app/opportunities/challenges',
          description: 'Défis et progression gamifiée'
        },
        {
          label: 'Saved items',
          route: '/app/opportunities/saved-items',
          description: 'Éléments sauvegardés'
        },
        {
          label: 'Applications',
          route: '/app/activity/applications',
          description: 'Mes candidatures en cours'
        },
        {
          label: 'Contracts',
          route: '/app/activity/contracts',
          description: 'Contrats actifs et en attente'
        },
        {
          label: 'Deliveries',
          route: '/app/activity/deliveries',
          description: 'Livrables et handoff client'
        },
        {
          label: 'Participations',
          route: '/app/activity/participations',
          description: 'Événements et présence pro'
        },
        {
          label: 'My Reviews',
          route: '/app/activity/my-reviews',
          description: 'Avis et feedback reçus'
        },
        {
          label: 'Messages',
          route: '/app/messages',
          description: 'Conversations et échanges'
        },
        {
          label: 'Notifications',
          route: '/app/notifications',
          description: 'Alertes et activité récente'
        }
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
        },
        {
          label: 'Support',
          route: '/app/support/reclamations',
          description: 'Réclamations et assistance'
        }
      ]
    }
  ];

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentAuthUser();
  }

  hasChildren(item: NavItem): boolean {
    return !!item.children && item.children.length > 0;
  }

  onToggleSidebar(): void {
    this.toggleSidebar.emit();
  }

  toggleSearch(): void {
    this.searchOpen = !this.searchOpen;
  }

  closeSearch(): void {
    this.searchOpen = false;
  }

  onQuickNavigate(): void {
    this.closeSearch();
  }

  trackByLabel(index: number, item: QuickSearchItem): string {
    return item.label;
  }

  onLogout(): void {
    this.closeSearch();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}