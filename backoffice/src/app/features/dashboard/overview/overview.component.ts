import { Component, OnInit } from '@angular/core';
import { UserService, DashboardStats } from '../../../core/services/user.service';

interface StatCard {
  label: string;
  value: string;
  change: string;
  changeType: 'up' | 'down' | 'flat';
  icon: string;
  iconClass: string;
}

interface RecentActivity {
  user: string;
  initials: string;
  action: string;
  time: string;
  type: 'kyc' | 'review' | 'contract' | 'badge';
}

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent implements OnInit {
  stats: StatCard[] = [
    {
      label: 'Total Users',
      value: '0',
      change: '',
      changeType: 'flat',
      icon: 'fa-users',
      iconClass: 'accent'
    },
    {
      label: 'Active Users',
      value: '0',
      change: '',
      changeType: 'flat',
      icon: 'fa-user-check',
      iconClass: 'success'
    },
    {
      label: 'KYC Pending',
      value: '0',
      change: '',
      changeType: 'flat',
      icon: 'fa-id-card',
      iconClass: 'warning'
    },
    {
      label: 'Suspended Users',
      value: '0',
      change: '',
      changeType: 'flat',
      icon: 'fa-ban',
      iconClass: 'danger'
    },
    {
      label: 'Freelancers',
      value: '0',
      change: '',
      changeType: 'flat',
      icon: 'fa-briefcase',
      iconClass: 'info'
    },
    {
      label: 'Clients',
      value: '0',
      change: '',
      changeType: 'flat',
      icon: 'fa-building',
      iconClass: 'gold'
    }
  ];

  recentActivities: RecentActivity[] = [
    { user: 'Ahmed Ben Ali', initials: 'AB', action: 'KYC submitted — awaiting review', time: '2 min ago', type: 'kyc' },
    { user: 'Sarra Trabelsi', initials: 'ST', action: 'Received 5★ review on contract #482', time: '8 min ago', type: 'review' },
    { user: 'Mohamed Gharbi', initials: 'MG', action: 'Contract #489 signed — 2,400 DT', time: '15 min ago', type: 'contract' },
    { user: 'Ines Mansouri', initials: 'IM', action: 'Badge unlocked', time: '32 min ago', type: 'badge' }
  ];

  modules = [
    { name: 'User Service', port: '8081', status: 'online', endpoints: 24, icon: 'fa-user-shield' },
    { name: 'Review Service', port: '8085', status: 'online', endpoints: 18, icon: 'fa-star' },
    { name: 'Contract Service', port: '8083', status: 'online', endpoints: 21, icon: 'fa-file-contract' },
    { name: 'Job Service', port: '8082', status: 'offline', endpoints: 15, icon: 'fa-briefcase' },
    { name: 'Event Service', port: '8087', status: 'online', endpoints: 12, icon: 'fa-calendar-days' },
    { name: 'Recruit Service', port: '8089', status: 'offline', endpoints: 16, icon: 'fa-user-tie' }
  ];

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.userService.getDashboardStats().subscribe({
      next: (data: DashboardStats) => {
        this.stats = [
          {
            label: 'Total Users',
            value: String(data.totalUsers),
            change: '',
            changeType: 'flat',
            icon: 'fa-users',
            iconClass: 'accent'
          },
          {
            label: 'Active Users',
            value: String(data.activeUsers),
            change: '',
            changeType: 'flat',
            icon: 'fa-user-check',
            iconClass: 'success'
          },
          {
            label: 'KYC Pending',
            value: String(data.kycPending),
            change: '',
            changeType: 'flat',
            icon: 'fa-id-card',
            iconClass: 'warning'
          },
          {
            label: 'Suspended Users',
            value: String(data.suspendedUsers),
            change: '',
            changeType: 'flat',
            icon: 'fa-ban',
            iconClass: 'danger'
          },
          {
            label: 'Freelancers',
            value: String(data.totalFreelancers),
            change: '',
            changeType: 'flat',
            icon: 'fa-briefcase',
            iconClass: 'info'
          },
          {
            label: 'Clients',
            value: String(data.totalClients),
            change: '',
            changeType: 'flat',
            icon: 'fa-building',
            iconClass: 'gold'
          }
        ];
      },
      error: (error) => {
        console.error('Erreur chargement dashboard stats:', error);
      }
    });
  }

  getActivityIcon(type: string): string {
    const icons: Record<string, string> = {
      kyc: 'fa-id-card',
      review: 'fa-star',
      contract: 'fa-file-contract',
      badge: 'fa-trophy'
    };
    return icons[type] || 'fa-circle';
  }

  getActivityClass(type: string): string {
    const classes: Record<string, string> = {
      kyc: 'warning',
      review: 'accent',
      contract: 'success',
      badge: 'gold'
    };
    return classes[type] || 'accent';
  }
}