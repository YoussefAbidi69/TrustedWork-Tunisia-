import { Component } from '@angular/core';

interface ParticipationStat {
  label: string;
  value: string;
  tone?: 'default' | 'accent' | 'success' | 'warning';
}

interface ParticipationItem {
  id: string;
  eventTitle: string;
  organizer: string;
  role: string;
  date: string;
  status: 'Confirmed' | 'Pending' | 'Completed';
  summary: string;
}

@Component({
  selector: 'app-participations',
  templateUrl: './participations.component.html',
  styleUrls: ['./participations.component.css']
})
export class ParticipationsComponent {
  selectedParticipationId = 'part-1';

  readonly stats: ParticipationStat[] = [
    { label: 'Confirmed participations', value: '09', tone: 'success' },
    { label: 'Pending', value: '03', tone: 'warning' },
    { label: 'Completed', value: '18', tone: 'accent' },
    { label: 'Visibility boost', value: '+19%', tone: 'default' }
  ];

  readonly participations: ParticipationItem[] = [
    {
      id: 'part-1',
      eventTitle: 'TrustedWork Design Roundtable',
      organizer: 'TrustedWork Tunisia',
      role: 'Speaker',
      date: '14 Apr 2026',
      status: 'Confirmed',
      summary: 'Participation as a featured speaker on premium freelance positioning and trust-building in digital marketplaces.'
    },
    {
      id: 'part-2',
      eventTitle: 'Creative Portfolio Showcase',
      organizer: 'Nova Studio',
      role: 'Participant',
      date: '22 Apr 2026',
      status: 'Pending',
      summary: 'Portfolio showcase opportunity focused on premium client-facing case studies and strong visual presentation.'
    },
    {
      id: 'part-3',
      eventTitle: 'UX Leadership Meetup',
      organizer: 'Pixel Forge',
      role: 'Guest',
      date: '28 Mar 2026',
      status: 'Completed',
      summary: 'Completed participation in a UX strategy meetup with senior professionals and product leads.'
    }
  ];

  get selectedParticipation(): ParticipationItem {
    return this.participations.find(item => item.id === this.selectedParticipationId) ?? this.participations[0];
  }

  selectParticipation(id: string): void {
    this.selectedParticipationId = id;
  }

  getStatToneClass(tone?: ParticipationStat['tone']): string {
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

  getStatusClass(status: ParticipationItem['status']): string {
    switch (status) {
      case 'Confirmed':
        return 'status-badge status-badge--confirmed';
      case 'Pending':
        return 'status-badge status-badge--pending';
      case 'Completed':
        return 'status-badge status-badge--completed';
      default:
        return 'status-badge';
    }
  }
}