import { Component } from '@angular/core';

type EventStatus = 'Open' | 'Closing soon' | 'Invite only' | 'Live';
type EventType = 'Networking' | 'Workshop' | 'Hackathon' | 'Recruitment';

interface EventMetric {
  label: string;
  value: string;
  caption: string;
}

interface FeaturedEvent {
  id: number;
  title: string;
  type: EventType;
  status: EventStatus;
  date: string;
  time: string;
  location: string;
  attendees: number;
  trustRequired: number;
  organizer: string;
  description: string;
  tags: string[];
}

interface AgendaItem {
  hour: string;
  title: string;
  kind: string;
}

@Component({
  selector: 'app-events-overview',
  templateUrl: './events-overview.component.html',
  styleUrls: ['./events-overview.component.css']
})
export class EventsOverviewComponent {
  readonly metrics: EventMetric[] = [
    {
      label: 'Upcoming events',
      value: '24',
      caption: 'Professional events curated for your profile'
    },
    {
      label: 'My invitations',
      value: '08',
      caption: 'Events where your profile is shortlisted'
    },
    {
      label: 'Participation rate',
      value: '91%',
      caption: 'Strong consistency across community events'
    }
  ];

  readonly featuredEvents: FeaturedEvent[] = [
    {
      id: 1,
      title: 'TrustedWork Networking Night',
      type: 'Networking',
      status: 'Open',
      date: '18 Apr 2026',
      time: '18:30',
      location: 'Tunis · Lac 1',
      attendees: 120,
      trustRequired: 70,
      organizer: 'TrustedWork Community',
      description:
        'Meet premium freelancers, vetted clients and platform partners in a curated offline networking session focused on collaborations and contracts.',
      tags: ['Community', 'Clients', 'Partnerships']
    },
    {
      id: 2,
      title: 'Angular SaaS UX Masterclass',
      type: 'Workshop',
      status: 'Closing soon',
      date: '22 Apr 2026',
      time: '19:00',
      location: 'Online · Google Meet',
      attendees: 86,
      trustRequired: 60,
      organizer: 'Frontend Circle',
      description:
        'A high-value design and implementation workshop around premium Angular dashboards, scalable UI patterns and recruiter-grade portfolio quality.',
      tags: ['Angular', 'UX', 'Design Systems']
    },
    {
      id: 3,
      title: 'MENA Freelance Hiring Sprint',
      type: 'Recruitment',
      status: 'Invite only',
      date: '27 Apr 2026',
      time: '14:00',
      location: 'Remote',
      attendees: 44,
      trustRequired: 80,
      organizer: 'TrustedWork Talent Ops',
      description:
        'A selective event connecting top-rated freelancers with companies hiring for product, design and engineering missions in Tunisia and the MENA region.',
      tags: ['Hiring', 'Premium Clients', 'Remote']
    }
  ];

  readonly agenda: AgendaItem[] = [
    { hour: '09:00', title: 'Client introductions', kind: 'Warm-up session' },
    { hour: '10:30', title: 'Product demo pitches', kind: 'Showcase' },
    { hour: '13:00', title: '1:1 recruiter matches', kind: 'Recruitment' },
    { hour: '16:00', title: 'Expert panel and closing', kind: 'Talk' }
  ];

  selectedEvent: FeaturedEvent = this.featuredEvents[0];

  selectEvent(event: FeaturedEvent): void {
    this.selectedEvent = event;
  }

  getStatusClass(status: EventStatus): string {
    switch (status) {
      case 'Live':
        return 'status-live';
      case 'Open':
        return 'status-open';
      case 'Closing soon':
        return 'status-closing';
      case 'Invite only':
        return 'status-invite';
      default:
        return '';
    }
  }

  trackByMetric(index: number, item: EventMetric): string {
    return item.label;
  }

  trackByEvent(index: number, item: FeaturedEvent): number {
    return item.id;
  }

  trackByAgenda(index: number, item: AgendaItem): string {
    return `${item.hour}-${item.title}`;
  }

  trackByText(index: number, item: string): string {
    return item;
  }
}