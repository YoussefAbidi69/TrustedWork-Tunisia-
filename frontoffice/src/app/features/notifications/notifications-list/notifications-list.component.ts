import { Component } from '@angular/core';

interface NotificationStat {
  label: string;
  value: string;
  tone?: 'default' | 'accent' | 'success' | 'warning';
}

interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: 'Application' | 'Message' | 'Trust' | 'Finance' | 'System';
  time: string;
  unread: boolean;
  priority: 'High' | 'Medium' | 'Low';
}

@Component({
  selector: 'app-notifications-list',
  templateUrl: './notifications-list.component.html',
  styleUrls: ['./notifications-list.component.css']
})
export class NotificationsListComponent {
  selectedNotificationId = 'notif-1';

  readonly stats: NotificationStat[] = [
    { label: 'Unread alerts', value: '08', tone: 'accent' },
    { label: 'Today', value: '14', tone: 'success' },
    { label: 'Priority items', value: '03', tone: 'warning' },
    { label: 'Resolved rate', value: '96%', tone: 'success' }
  ];

  readonly notifications: NotificationItem[] = [
    {
      id: 'notif-1',
      title: 'Application shortlisted',
      message: 'Your application for Senior Product Designer has been shortlisted by a premium client.',
      type: 'Application',
      time: '5 min ago',
      unread: true,
      priority: 'High'
    },
    {
      id: 'notif-2',
      title: 'New unread message',
      message: 'A client sent you a new message regarding the project timeline and deliverables.',
      type: 'Message',
      time: '18 min ago',
      unread: true,
      priority: 'High'
    },
    {
      id: 'notif-3',
      title: 'Trust score updated',
      message: 'Your trust score increased after a verified review was published on your profile.',
      type: 'Trust',
      time: '1 hour ago',
      unread: true,
      priority: 'Medium'
    },
    {
      id: 'notif-4',
      title: 'Escrow payment released',
      message: 'A secured escrow payment has been released to your wallet successfully.',
      type: 'Finance',
      time: '3 hours ago',
      unread: false,
      priority: 'Medium'
    },
    {
      id: 'notif-5',
      title: 'Platform security reminder',
      message: 'Enable all recommended profile verification options to strengthen client confidence.',
      type: 'System',
      time: 'Today',
      unread: false,
      priority: 'Low'
    }
  ];

  readonly quickFilters: string[] = [
    'All',
    'Unread',
    'Applications',
    'Messages',
    'Trust',
    'Finance'
  ];

  get selectedNotification(): NotificationItem {
    return (
      this.notifications.find(item => item.id === this.selectedNotificationId) ??
      this.notifications[0]
    );
  }

  selectNotification(id: string): void {
    this.selectedNotificationId = id;
  }

  getStatToneClass(tone?: NotificationStat['tone']): string {
    switch (tone) {
      case 'accent':
        return 'stat-value stat-value--accent';
      case 'success':
        return 'stat-value stat-value--success';
      case 'warning':
        return 'stat-value stat-value--warning';
      default:
        return 'stat-value';
    }
  }

  getTypeClass(type: NotificationItem['type']): string {
    switch (type) {
      case 'Application':
        return 'tag tag--application';
      case 'Message':
        return 'tag tag--message';
      case 'Trust':
        return 'tag tag--trust';
      case 'Finance':
        return 'tag tag--finance';
      default:
        return 'tag tag--system';
    }
  }

  getPriorityClass(priority: NotificationItem['priority']): string {
    switch (priority) {
      case 'High':
        return 'priority priority--high';
      case 'Medium':
        return 'priority priority--medium';
      default:
        return 'priority priority--low';
    }
  }
}