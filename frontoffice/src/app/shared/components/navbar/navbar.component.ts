import { Component, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthUser } from '../../../core/models/auth.model';
import { NotificationService } from '../../../core/services/notification.service';

// ... (interfaces NavChildItem, NavItem, QuickSearchItem inchangées)

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

  // Injection via inject() — évite le problème de token Angular 18
  public authService        = inject(AuthService);
  private router            = inject(Router);
  private notificationService = inject(NotificationService);

  searchOpen = false;
  searchTerm = '';
  currentUser: AuthUser | null = null;
  activeDropdown: string | null = null;

  quickSearchItems: QuickSearchItem[] = [
    { label: 'Mon Profil',      route: '/app/profile/overview'      },
    { label: 'KYC',             route: '/app/profile/kyc'           },
    { label: 'Trust Passport',  route: '/app/profile/trust-passport'},
    { label: 'Parametres',      route: '/app/profile/settings'      },
    { label: 'Portfolio',       route: '/app/profile/portfolio'     },
    { label: 'Carriere AI',     route: '/app/profile/career-path'   }
  ];

  navItems: NavItem[] = [
    { label: 'Dashboard', route: '/app/dashboard' },
    {
      label: 'Identite et Acces',
      children: [
        { label: 'Vue generale',   route: '/app/profile/overview',       description: 'Resume de votre profil'  },
        { label: 'KYC',            route: '/app/profile/kyc',            description: 'Verification identite CIN'},
        { label: 'Trust Passport', route: '/app/profile/trust-passport', description: 'Score de confiance'      },
        { label: 'Parametres',     route: '/app/profile/settings',       description: 'Securite et 2FA'         }
      ]
    },
    {
      label: 'Profil Freelancer',
      children: [
        { label: 'Portfolio',    route: '/app/profile/portfolio',      description: 'Mes projets realises'         },
        { label: 'Experiences',  route: '/app/profile/work-experience',description: 'Parcours professionnel'       },
        { label: 'Endorsements', route: '/app/profile/endorsements',   description: 'Validation des competences'   },
        { label: 'Avis Clients', route: '/app/profile/reviews',        description: 'Reputation publique'          },
        { label: 'Carriere AI',  route: '/app/profile/career-path',    description: 'Recommandations intelligentes'}
      ]
    },
    {
      label: 'Marketplace',
      children: [
        { label: 'Offres Freelance', description: 'Missions disponibles',   disabled: true },
        { label: 'Contrats',         description: 'Gestion des contrats',   disabled: true },
        { label: 'Escrow',           description: 'Paiements securises',    disabled: true }
      ]
    },
    {
      label: 'Communaute',
      children: [
        { label: 'Evenements', description: 'Hackathons et meetups', disabled: true },
        { label: 'Challenges', description: 'Defis et gamification', disabled: true },
        { label: 'Agences',    description: 'Equipes freelance',     disabled: true }
      ]
    }
  ];

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentAuthUser();
    // Connexion WebSocket dès que la navbar est initialisée
    this.notificationService.connect();
  }

  get displayName(): string {
    if (!this.currentUser) return 'User';
    const user: any = this.currentUser;
    const firstName = user.firstName || user.firstname || user.prenom || '';
    const lastName  = user.lastName  || user.lastname  || user.nom    || '';
    const fullName  = user.fullName  || `${firstName} ${lastName}`.trim();
    if (fullName) return fullName;
    if (this.currentUser.email) {
      return this.currentUser.email.split('@')[0]
        .replace(/[._-]+/g, ' ')
        .replace(/\b\w/g, (c: string) => c.toUpperCase())
        .trim() || 'User';
    }
    return 'User';
  }

  hasChildren(item: NavItem): boolean       { return !!item.children?.length; }
  openDropdown(label: string): void         { this.activeDropdown = label; }
  closeDropdown(): void                     { this.activeDropdown = null; }
  isDropdownOpen(label: string): boolean    { return this.activeDropdown === label; }
  onToggleSidebar(): void                   { this.toggleSidebar.emit(); }
  toggleSearch(): void                      { this.searchOpen = !this.searchOpen; }
  closeSearch(): void                       { this.searchOpen = false; this.activeDropdown = null; }
  onQuickNavigate(): void                   { this.closeSearch(); }
  stopProp(e: Event): void                  { e.stopPropagation(); }
  trackByLabel(_: number, item: QuickSearchItem): string { return item.label; }

  onLogout(): void {
    this.closeSearch();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}