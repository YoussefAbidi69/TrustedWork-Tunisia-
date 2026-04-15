import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { Router } from '@angular/router';

interface NavItem {
  label: string;
  icon: string;
  route?: string;
  badge?: string | number;
  badgeType?: string;
  children?: NavItem[];
  expanded?: boolean;
  separator?: boolean;
  sectionLabel?: string;
}

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  @Input() collapsed = false;
  @Output() toggleCollapse = new EventEmitter<void>();

  navItems: NavItem[] = [
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
      route: '/admin/users',
      badge: '',
      badgeType: 'accent'
    },
    {
      label: 'KYC Requests',
      icon: 'fa-id-card',
      route: '/admin/users/kyc',
      badge: '12',
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
      sectionLabel: 'Reputation Engine',
      label: 'Reviews',
      icon: 'fa-star',
      route: '/admin/reviews',
    },
    {
      label: 'Reclamations',
      icon: 'fa-flag',
      route: '/admin/reviews/reclamations',
      badge: '3',
      badgeType: 'danger'
    },
    {
      label: 'Trust Scores',
      icon: 'fa-shield-halved',
      route: '/admin/reviews/trust-scores',
    },
    {
      label: 'Badges',
      icon: 'fa-trophy',
      route: '/admin/reviews/badges',
    },
    {
      label: 'Growth Profiles',
      icon: 'fa-chart-line',
      route: '/admin/reviews/growth-profiles',
    },
    {
      sectionLabel: 'Platform',
      label: 'Contracts',
      icon: 'fa-file-contract',
      route: '/admin/contracts',
    },
    {
      label: 'Job Listings',
      icon: 'fa-briefcase',
      route: '/admin/jobs',
    },
    {
      label: 'Events',
      icon: 'fa-calendar-days',
      route: '/admin/events',
    },
    {
      label: 'Recruitment',
      icon: 'fa-user-tie',
      route: '/admin/recruitment',
    },
  ];

  constructor(private router: Router) {}

  ngOnInit() {}

  onToggle() {
    this.toggleCollapse.emit();
  }

  isActive(route: string): boolean {
    return this.router.url === route;
  }

  isParentActive(route: string): boolean {
    return this.router.url.startsWith(route);
  }
}