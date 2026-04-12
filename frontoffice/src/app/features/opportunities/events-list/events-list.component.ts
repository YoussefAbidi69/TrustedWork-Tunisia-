import { Component } from '@angular/core';

type EventCategory = 'All' | 'Networking' | 'Workshop' | 'Hackathon' | 'Recruitment';
type EventStatus = 'Open' | 'Closing soon' | 'Invite only';

interface EventListItem {
  id: number;
  title: string;
  category: Exclude<EventCategory, 'All'>;
  status: EventStatus;
  location: string;
  date: string;
  time: string;
  organizer: string;
  attendees: number;
  summary: string;
}

@Component({
  selector: 'app-events-list',
  templateUrl: './events-list.component.html',
  styleUrls: ['./events-list.component.css']
})
export class EventsListComponent {
  readonly filters: EventCategory[] = ['All', 'Networking', 'Workshop', 'Hackathon', 'Recruitment'];
  selectedFilter: EventCategory = 'All';

  readonly events: EventListItem[] = [
    {
      id: 1,
      title: 'TrustedWork Networking Night',
      category: 'Networking',
      status: 'Open',
      location: 'Tunis · Lac 1',
      date: '18 Apr 2026',
      time: '18:30',
      organizer: 'TrustedWork Community',
      attendees: 120,
      summary: 'Curated networking night with freelancers, clients and talent partners.'
    },
    {
      id: 2,
      title: 'Angular SaaS UX Masterclass',
      category: 'Workshop',
      status: 'Closing soon',
      location: 'Online',
      date: '22 Apr 2026',
      time: '19:00',
      organizer: 'Frontend Circle',
      attendees: 86,
      summary: 'Deep dive into premium Angular dashboard UX, architecture and UI patterns.'
    },
    {
      id: 3,
      title: 'Spring Boot Hiring Hackday',
      category: 'Hackathon',
      status: 'Open',
      location: 'Sousse',
      date: '25 Apr 2026',
      time: '09:00',
      organizer: 'Tech Tunisia',
      attendees: 54,
      summary: 'Technical challenge event designed for backend engineers and recruiters.'
    },
    {
      id: 4,
      title: 'MENA Freelance Hiring Sprint',
      category: 'Recruitment',
      status: 'Invite only',
      location: 'Remote',
      date: '27 Apr 2026',
      time: '14:00',
      organizer: 'TrustedWork Talent Ops',
      attendees: 44,
      summary: 'Selective hiring event for trusted profiles with strong match scores.'
    }
  ];

  get filteredEvents(): EventListItem[] {
    if (this.selectedFilter === 'All') {
      return this.events;
    }

    return this.events.filter(event => event.category === this.selectedFilter);
  }

  setFilter(filter: EventCategory): void {
    this.selectedFilter = filter;
  }

  getStatusClass(status: EventStatus): string {
    switch (status) {
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

  trackByEvent(index: number, item: EventListItem): number {
    return item.id;
  }

  trackByFilter(index: number, item: EventCategory): EventCategory {
    return item;
  }
}