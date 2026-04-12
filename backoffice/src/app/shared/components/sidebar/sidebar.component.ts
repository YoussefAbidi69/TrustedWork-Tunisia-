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
    }
  ];

  comingSoonItems: NavItem[] = [
    {
      sectionLabel: 'Reputation Engine',
      label: 'Reviews',
      icon: 'fa-star',
      comingSoon: true
    },
    {
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
      label: 'Contracts',
      icon: 'fa-file-contract',
      comingSoon: true
    },
    {
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