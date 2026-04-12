import { Component } from '@angular/core';

type ClaimStatus = 'Open' | 'In Review' | 'Resolved' | 'Urgent';
type ClaimPriority = 'Low' | 'Medium' | 'High';
type ClaimCategory = 'Payment' | 'Client dispute' | 'Account issue' | 'Technical issue';

interface SupportStat {
  label: string;
  value: string;
  caption: string;
}

interface ClaimTimelineItem {
  title: string;
  date: string;
  description: string;
}

interface ReclamationItem {
  id: string;
  title: string;
  category: ClaimCategory;
  status: ClaimStatus;
  priority: ClaimPriority;
  createdAt: string;
  lastUpdate: string;
  assignedTo: string;
  description: string;
  clientName: string;
  missionRef: string;
  timeline: ClaimTimelineItem[];
}

@Component({
  selector: 'app-reclamations',
  templateUrl: './reclamations.component.html',
  styleUrls: ['./reclamations.component.css']
})
export class ReclamationsComponent {
  readonly supportStats: SupportStat[] = [
    {
      label: 'Open claims',
      value: '14',
      caption: 'Tickets currently requiring action'
    },
    {
      label: 'Resolved this month',
      value: '38',
      caption: 'Issues successfully closed by support'
    },
    {
      label: 'Avg. response time',
      value: '2h 40m',
      caption: 'Measured across active conversations'
    }
  ];

  readonly statusFilters: Array<ClaimStatus | 'All'> = [
    'All',
    'Open',
    'In Review',
    'Resolved',
    'Urgent'
  ];

  selectedStatus: ClaimStatus | 'All' = 'All';

  readonly reclamations: ReclamationItem[] = [
    {
      id: 'REC-1042',
      title: 'Delayed payment after completed milestone',
      category: 'Payment',
      status: 'Open',
      priority: 'High',
      createdAt: '03 Apr 2026',
      lastUpdate: '2 hours ago',
      assignedTo: 'Nadia B.',
      description:
        'The client approved the delivered milestone but the payment release has not been reflected in the wallet. Freelancer requests verification and escalation.',
      clientName: 'Orbit Digital Studio',
      missionRef: 'TW-MIS-7821',
      timeline: [
        {
          title: 'Claim submitted',
          date: '03 Apr 2026 · 09:12',
          description: 'Freelancer opened a payment-related reclamation.'
        },
        {
          title: 'Support acknowledged',
          date: '03 Apr 2026 · 10:03',
          description: 'Automated acknowledgment sent and case assigned.'
        },
        {
          title: 'Awaiting finance verification',
          date: '03 Apr 2026 · 11:24',
          description: 'Internal team is checking escrow release logs.'
        }
      ]
    },
    {
      id: 'REC-1038',
      title: 'Dispute about delivered Angular dashboard scope',
      category: 'Client dispute',
      status: 'In Review',
      priority: 'High',
      createdAt: '02 Apr 2026',
      lastUpdate: '5 hours ago',
      assignedTo: 'Sami K.',
      description:
        'The freelancer claims the original scope was completed, while the client considers some premium UI sections incomplete. Requires manual review of milestone acceptance.',
      clientName: 'BluePeak Digital',
      missionRef: 'TW-MIS-7714',
      timeline: [
        {
          title: 'Dispute opened',
          date: '02 Apr 2026 · 14:20',
          description: 'Client and freelancer submitted conflicting notes.'
        },
        {
          title: 'Evidence requested',
          date: '02 Apr 2026 · 16:00',
          description: 'Support requested screenshots and agreed scope files.'
        },
        {
          title: 'Case under arbitration',
          date: '03 Apr 2026 · 08:40',
          description: 'Senior moderator is reviewing the attached deliverables.'
        }
      ]
    },
    {
      id: 'REC-1031',
      title: 'Login protection triggered unexpectedly',
      category: 'Account issue',
      status: 'Resolved',
      priority: 'Medium',
      createdAt: '31 Mar 2026',
      lastUpdate: 'Yesterday',
      assignedTo: 'Lina R.',
      description:
        'User was temporarily locked after multiple login attempts from a new device. Access was restored after identity verification.',
      clientName: 'Personal account',
      missionRef: 'ACC-4421',
      timeline: [
        {
          title: 'Incident reported',
          date: '31 Mar 2026 · 18:10',
          description: 'User reported being unable to access the dashboard.'
        },
        {
          title: 'Verification completed',
          date: '31 Mar 2026 · 19:25',
          description: 'Support validated account ownership.'
        },
        {
          title: 'Issue resolved',
          date: '31 Mar 2026 · 19:42',
          description: 'Access reset and security recommendations sent.'
        }
      ]
    },
    {
      id: 'REC-1027',
      title: 'Notification delivery failure on important updates',
      category: 'Technical issue',
      status: 'Urgent',
      priority: 'High',
      createdAt: '30 Mar 2026',
      lastUpdate: '30 minutes ago',
      assignedTo: 'Tech Ops',
      description:
        'Critical system notifications related to job applications are not appearing consistently. Impacts activity tracking and response times.',
      clientName: 'Platform-wide issue',
      missionRef: 'SYS-ALERT-92',
      timeline: [
        {
          title: 'Issue detected',
          date: '30 Mar 2026 · 08:30',
          description: 'Internal monitoring flagged notification failures.'
        },
        {
          title: 'Incident escalated',
          date: '30 Mar 2026 · 09:15',
          description: 'Tech support and product team were notified.'
        },
        {
          title: 'Hotfix in progress',
          date: '03 Apr 2026 · 12:05',
          description: 'Patch validation is ongoing before deployment.'
        }
      ]
    }
  ];

  selectedReclamation: ReclamationItem = this.reclamations[0];

  get filteredReclamations(): ReclamationItem[] {
    if (this.selectedStatus === 'All') {
      return this.reclamations;
    }

    return this.reclamations.filter(
      (item) => item.status === this.selectedStatus
    );
  }

  selectStatus(status: ClaimStatus | 'All'): void {
    this.selectedStatus = status;
    this.syncSelectedReclamation();
  }

  selectReclamation(item: ReclamationItem): void {
    this.selectedReclamation = item;
  }

  getStatusClass(status: ClaimStatus): string {
    switch (status) {
      case 'Open':
        return 'status-open';
      case 'In Review':
        return 'status-review';
      case 'Resolved':
        return 'status-resolved';
      case 'Urgent':
        return 'status-urgent';
      default:
        return '';
    }
  }

  getPriorityClass(priority: ClaimPriority): string {
    switch (priority) {
      case 'Low':
        return 'priority-low';
      case 'Medium':
        return 'priority-medium';
      case 'High':
        return 'priority-high';
      default:
        return '';
    }
  }

  trackByLabel(index: number, item: SupportStat): string {
    return item.label;
  }

  trackByClaim(index: number, item: ReclamationItem): string {
    return item.id;
  }

  trackByText(index: number, item: string): string {
    return `${index}-${item}`;
  }

  private syncSelectedReclamation(): void {
    const visibleItems = this.filteredReclamations;

    if (!visibleItems.length) {
      return;
    }

    const stillExists = visibleItems.some(
      (item) => item.id === this.selectedReclamation.id
    );

    if (!stillExists) {
      this.selectedReclamation = visibleItems[0];
    }
  }
}