import { Component } from '@angular/core';

interface DeliveryStat {
  label: string;
  value: string;
  tone?: 'default' | 'accent' | 'success' | 'warning';
}

interface DeliveryItem {
  id: string;
  project: string;
  client: string;
  dueDate: string;
  status: 'Delivered' | 'In Review' | 'Pending' | 'Revision';
  files: number;
  summary: string;
}

@Component({
  selector: 'app-deliveries',
  templateUrl: './deliveries.component.html',
  styleUrls: ['./deliveries.component.css']
})
export class DeliveriesComponent {
  selectedDeliveryId = 'delivery-1';

  readonly stats: DeliveryStat[] = [
    { label: 'Delivered today', value: '03', tone: 'success' },
    { label: 'In review', value: '04', tone: 'warning' },
    { label: 'Pending handoff', value: '05', tone: 'accent' },
    { label: 'Revision requests', value: '02', tone: 'default' }
  ];

  readonly deliveries: DeliveryItem[] = [
    {
      id: 'delivery-1',
      project: 'SaaS Dashboard Redesign',
      client: 'Nova Studio',
      dueDate: '06 Apr 2026',
      status: 'In Review',
      files: 8,
      summary: 'Main dashboard UI kit, interaction states and final handoff deck delivered for client review.'
    },
    {
      id: 'delivery-2',
      project: 'Brand Identity Package',
      client: 'Pixel Forge',
      dueDate: '08 Apr 2026',
      status: 'Delivered',
      files: 12,
      summary: 'Complete logo system, color rules, typography selection and social media brand kit.'
    },
    {
      id: 'delivery-3',
      project: 'Marketing Motion Pack',
      client: 'North Star Agency',
      dueDate: '10 Apr 2026',
      status: 'Revision',
      files: 5,
      summary: 'Motion assets delivered with one client revision cycle requested for transitions and pacing.'
    }
  ];

  get selectedDelivery(): DeliveryItem {
    return this.deliveries.find(item => item.id === this.selectedDeliveryId) ?? this.deliveries[0];
  }

  selectDelivery(id: string): void {
    this.selectedDeliveryId = id;
  }

  getStatToneClass(tone?: DeliveryStat['tone']): string {
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

  getStatusClass(status: DeliveryItem['status']): string {
    switch (status) {
      case 'Delivered':
        return 'status-badge status-badge--delivered';
      case 'In Review':
        return 'status-badge status-badge--review';
      case 'Pending':
        return 'status-badge status-badge--pending';
      case 'Revision':
        return 'status-badge status-badge--revision';
      default:
        return 'status-badge';
    }
  }
}