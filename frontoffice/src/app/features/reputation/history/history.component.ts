import { Component } from '@angular/core';

interface HistoryEvent {
  id: number;
  title: string;
  description: string;
  date: string;
  type: 'xp' | 'badge' | 'review' | 'trust';
  value?: string;
}

@Component({
  selector: 'app-history',
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.css']
})
export class HistoryComponent {
  historyEvents: HistoryEvent[] = [
    {
      id: 1,
      title: 'Trust Score increased',
      description: 'Your trust score improved after a verified positive client review.',
      date: '18 Mar 2026',
      type: 'trust',
      value: '+4'
    },
    {
      id: 2,
      title: 'New badge unlocked',
      description: 'You unlocked the Fast Responder badge thanks to consistent responsiveness.',
      date: '14 Mar 2026',
      type: 'badge',
      value: 'Epic'
    },
    {
      id: 3,
      title: 'XP milestone reached',
      description: 'You reached a new XP milestone after completing recent platform actions.',
      date: '10 Mar 2026',
      type: 'xp',
      value: '+180 XP'
    },
    {
      id: 4,
      title: 'New client review received',
      description: 'A verified client left a 5-star review on your recent SaaS platform mission.',
      date: '08 Mar 2026',
      type: 'review',
      value: '5.0 ★'
    },
    {
      id: 5,
      title: 'Trust signal validated',
      description: 'Your profile gained an additional reliability signal through verified completion.',
      date: '03 Mar 2026',
      type: 'trust',
      value: 'Verified'
    },
    {
      id: 6,
      title: 'Badge progress updated',
      description: 'Your Reliable Finisher badge progressed after another on-time delivery.',
      date: '27 Feb 2026',
      type: 'badge',
      value: '72%'
    }
  ];

  getTypeClass(type: HistoryEvent['type']): string {
    switch (type) {
      case 'xp':
        return 'event-xp';
      case 'badge':
        return 'event-badge';
      case 'review':
        return 'event-review';
      case 'trust':
        return 'event-trust';
      default:
        return '';
    }
  }

  getTypeIcon(type: HistoryEvent['type']): string {
    switch (type) {
      case 'xp':
        return '⚡';
      case 'badge':
        return '🏅';
      case 'review':
        return '★';
      case 'trust':
        return '🛡';
      default:
        return '•';
    }
  }

  trackByEventId(index: number, item: HistoryEvent): number {
    return item.id;
  }
}