import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService, UserDTO } from '../../../core/services/user.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';

export interface NavItem {
  label: string;
  icon: string;
  route?: string;
  badge?: string | number;
  badgeType?: string;
  sectionLabel?: string;
  comingSoon?: boolean;
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  @Input() collapsed = false;
  @Output() toggleCollapse = new EventEmitter<void>();

  activeItems: NavItem[] = [
    {
      sectionLabel: 'Overview',
      label: 'Dashboard',
      icon: 'fa-gauge-high',
      route: '/admin/dashboard'
    },
    {
      sectionLabel: 'User Management',
      label: 'All Users',
      icon: 'fa-users',
      route: '/admin/users'
    },
    {
      label: 'KYC Requests',
      icon: 'fa-id-card',
      route: '/admin/users/kyc',
      badge: '',
      badgeType: 'warning'
    },
    {
      label: 'Audit Logs',
      icon: 'fa-scroll',
      route: '/admin/audit-logs'
    },
    {
      label: 'Suspensions',
      icon: 'fa-ban',
      route: '/admin/suspensions'
    },
    {
      sectionLabel: 'Freelancer Profiles',
      label: 'All Profiles',
      icon: 'fa-address-card',
      route: '/admin/freelancers'
    },
    {
      label: 'Reviews',
      icon: 'fa-star',
      route: '/admin/freelancers/reviews'
    },
    {
      label: 'Profile Reports',
      icon: 'fa-flag',
      route: '/admin/freelancers/reports',
      badge: '',
      badgeType: 'danger'
    },
    {
      label: 'Platform Stats',
      icon: 'fa-chart-pie',
      route: '/admin/freelancers/stats'
    },
    {
      label: 'Trending',
      icon:  'fa-fire',
      route: '/admin/freelancers/trending',
      badge: '🔥',
      badgeType: 'accent'
    },
    {
      sectionLabel: 'Système',
      label: 'Schedulers',
      icon: 'fa-clock-rotate-left',
      route: '/admin/schedulers'
    },
    {
      sectionLabel: 'Engagement & Gamification',
      label: 'Événements',
      icon: 'fa-calendar-check',
      route: '/admin/engagement/events'
    },
    {
      label: 'Défis (Challenges)',
      icon: 'fa-tasks',
      route: '/admin/engagement/challenges'
    },
    {
      label: 'Leaderboard',
      icon: 'fa-trophy',
      route: '/admin/engagement/leaderboard'
    },
    {
      label: 'Badges',
      icon: 'fa-medal',
      route: '/admin/engagement/badges'
    },
    {
      label: 'Growth Profiles',
      icon: 'fa-chart-line',
      route: '/admin/engagement/growth-profiles'
    },
    {
      sectionLabel: 'Activity',
      label: 'Contracts',
      icon: 'fa-file-contract',
      route: '/admin/activity/contracts'
    },
    {
      label: 'Disputes',
      icon: 'fa-gavel',
      route: '/admin/activity/disputes'
    },

    // ── Module 08 : Project Management ──
    {
      sectionLabel: 'Project Management',
      label: 'Dashboard',
      icon: 'fa-diagram-project',
      route: '/admin/projects'
    },
    {
      label: 'Projects',
      icon: 'fa-briefcase',
      route: '/admin/projects/admin/list'
    },
    {
      label: 'Tasks',
      icon: 'fa-list-check',
      route: '/admin/projects/admin/tasks'
    },
    {
      label: 'Deliverables',
      icon: 'fa-box-open',
      route: '/admin/projects/admin/deliverables'
    },
    {
      label: 'Risks',
      icon: 'fa-triangle-exclamation',
      route: '/admin/projects/admin/risks'
    },
    {
      label: 'Reports',
      icon: 'fa-chart-bar',
      route: '/admin/projects/admin/reports'
    },
    {
      label: 'ML Prediction',
      icon: 'fa-wand-magic-sparkles',
      route: '/admin/projects/admin/ml-prediction'
    },
    {
      label: 'Burndown',
      icon: 'fa-chart-line',
      route: '/admin/projects/admin/burndown'
    }
  ];

  comingSoonItems: NavItem[] = [
    {
      sectionLabel: 'Reputation Engine',
      label: 'Trust Scores',
      icon: 'fa-shield-halved',
      comingSoon: true
    },
    {
      label: 'Badges',
      icon: 'fa-trophy',
      comingSoon: true
    },
    {
      label: 'Growth Profiles',
      icon: 'fa-chart-line',
      comingSoon: true
    },
    {
      sectionLabel: 'Platform',
      label: 'Job Listings',
      icon: 'fa-briefcase',
      comingSoon: true
    },
    {
      label: 'Events',
      icon: 'fa-calendar-days',
      comingSoon: true
    },
    {
      label: 'Recruitment',
      icon: 'fa-user-tie',
      comingSoon: true
    }
  ];

  constructor(
    private router: Router,
    private userService: UserService,
    private freelancerProfileService: FreelancerProfileService
  ) {}

  ngOnInit(): void {
    this.loadDynamicBadges();
  }

  onToggle(): void {
    this.toggleCollapse.emit();
  }

  loadDynamicBadges(): void {
    this.loadPendingKycCount();
    this.loadPendingReportsCount();
  }

  loadPendingKycCount(): void {
    this.userService.getAllUsers().subscribe({
      next: (users: UserDTO[]) => {
        const pendingCount = users.filter(user => user.kycStatus === 'PENDING').length;
        this.setBadge('KYC Requests', pendingCount);
      },
      error: (err) => {
        console.error('Erreur chargement badge KYC :', err);
        this.setBadge('KYC Requests', 0);
      }
    });
  }

  loadPendingReportsCount(): void {
    this.freelancerProfileService.getPendingReports().subscribe({
      next: (reports) => {
        this.setBadge('Profile Reports', reports.length);
      },
      error: (err) => {
        console.error('Erreur chargement badge reports :', err);
        this.setBadge('Profile Reports', 0);
      }
    });
  }

  setBadge(label: string, count: number): void {
    const item = this.activeItems.find(navItem => navItem.label === label);
    if (!item) return;

    item.badge = count > 0 ? count : '';
  }

  hasBadge(item: NavItem): boolean {
    return item.badge !== '' && item.badge !== undefined && item.badge !== null;
  }

  isActive(route: string): boolean {
    const currentUrl = this.router.url.split('?')[0];

    switch (route) {
      case '/admin/dashboard':
        return currentUrl === '/admin/dashboard';

      case '/admin/users':
        return currentUrl === '/admin/users' || /^\/admin\/users\/\d+$/.test(currentUrl);

      case '/admin/users/kyc':
        return currentUrl === '/admin/users/kyc';

      case '/admin/audit-logs':
        return currentUrl === '/admin/audit-logs';

      case '/admin/suspensions':
        return currentUrl === '/admin/suspensions';

      case '/admin/freelancers':
        return currentUrl === '/admin/freelancers' || /^\/admin\/freelancers\/\d+$/.test(currentUrl);

      case '/admin/freelancers/reports':
        return currentUrl === '/admin/freelancers/reports';

      case '/admin/freelancers/stats':
        return currentUrl === '/admin/freelancers/stats';
        
        case '/admin/freelancers/reviews':
        return currentUrl === '/admin/freelancers/reviews';

      case '/admin/schedulers':
        return currentUrl === '/admin/schedulers';

      case '/admin/engagement/events':
        return currentUrl.startsWith('/admin/engagement/events');
      case '/admin/engagement/challenges':
        return currentUrl.startsWith('/admin/engagement/challenges');
      case '/admin/engagement/leaderboard':
        return currentUrl.startsWith('/admin/engagement/leaderboard');
      case '/admin/engagement/badges':
        return currentUrl.startsWith('/admin/engagement/badges');
      case '/admin/engagement/growth-profiles':
        return currentUrl.startsWith('/admin/engagement/growth-profiles');

      case '/admin/projects':
        return currentUrl === '/admin/projects';
      case '/admin/projects/admin/list':
        return currentUrl === '/admin/projects/admin/list';
      case '/admin/projects/admin/tasks':
        return currentUrl === '/admin/projects/admin/tasks';
      case '/admin/projects/admin/deliverables':
        return currentUrl === '/admin/projects/admin/deliverables';
      case '/admin/projects/admin/risks':
        return currentUrl === '/admin/projects/admin/risks';
      case '/admin/projects/admin/reports':
        return currentUrl === '/admin/projects/admin/reports';
      case '/admin/projects/admin/ml-prediction':
        return currentUrl === '/admin/projects/admin/ml-prediction';
      case '/admin/projects/admin/burndown':
        return currentUrl === '/admin/projects/admin/burndown';

      default:
        return currentUrl === route;
    }
  }

  getBadgeDotClass(item: NavItem): string {
    switch (item.badgeType) {
      case 'danger':
        return 'danger';
      case 'warning':
        return 'warning';
      default:
        return 'accent';
    }
  }
}