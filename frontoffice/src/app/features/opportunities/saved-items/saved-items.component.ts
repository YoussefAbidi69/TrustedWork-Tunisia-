import { Component } from '@angular/core';

type SavedKind = 'Freelance Job' | 'Recruitment' | 'Event' | 'Challenge';

interface SavedItem {
  id: number;
  title: string;
  kind: SavedKind;
  subtitle: string;
  meta: string;
}

@Component({
  selector: 'app-saved-items',
  templateUrl: './saved-items.component.html',
  styleUrls: ['./saved-items.component.css']
})
export class SavedItemsComponent {
  readonly items: SavedItem[] = [
    {
      id: 1,
      title: 'Senior Angular Frontend Engineer for SaaS Marketplace',
      kind: 'Freelance Job',
      subtitle: 'Nexora Studio',
      meta: 'Best match · 94%'
    },
    {
      id: 2,
      title: 'TrustedWork Networking Night',
      kind: 'Event',
      subtitle: 'TrustedWork Community',
      meta: '18 Apr 2026 · Tunis'
    },
    {
      id: 3,
      title: 'Complete 3 verified applications',
      kind: 'Challenge',
      subtitle: 'Growth challenge',
      meta: '+120 XP'
    },
    {
      id: 4,
      title: 'Senior Angular Engineer',
      kind: 'Recruitment',
      subtitle: 'Nexora Studio',
      meta: 'Remote · Priority'
    }
  ];

  getKindClass(kind: SavedKind): string {
    switch (kind) {
      case 'Freelance Job':
        return 'kind-job';
      case 'Recruitment':
        return 'kind-recruitment';
      case 'Event':
        return 'kind-event';
      case 'Challenge':
        return 'kind-challenge';
      default:
        return '';
    }
  }

  trackByItem(index: number, item: SavedItem): number {
    return item.id;
  }
}