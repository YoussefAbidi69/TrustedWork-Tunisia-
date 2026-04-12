import { Component } from '@angular/core';

type BadgeRarity = 'Common' | 'Rare' | 'Epic' | 'Legendary';
type BadgeStatus = 'Unlocked' | 'In Progress' | 'Locked';

interface BadgeItem {
  id: number;
  title: string;
  category: string;
  rarity: BadgeRarity;
  status: BadgeStatus;
  icon: string;
  description: string;
  progress: number;
  xpReward: number;
}

@Component({
  selector: 'app-badges',
  templateUrl: './badges.component.html',
  styleUrls: ['./badges.component.css']
})
export class BadgesComponent {
  selectedFilter: 'All' | BadgeStatus = 'All';

  stats = {
    totalBadges: 24,
    unlockedBadges: 14,
    legendaryBadges: 3,
    totalXp: 2480
  };

  badges: BadgeItem[] = [
    {
      id: 1,
      title: 'Top Rated',
      category: 'Performance',
      rarity: 'Legendary',
      status: 'Unlocked',
      icon: '★',
      description: 'Awarded for consistently receiving outstanding client reviews across multiple projects.',
      progress: 100,
      xpReward: 500
    },
    {
      id: 2,
      title: 'Fast Responder',
      category: 'Communication',
      rarity: 'Epic',
      status: 'Unlocked',
      icon: '⚡',
      description: 'Recognizes excellent responsiveness and strong communication reliability.',
      progress: 100,
      xpReward: 300
    },
    {
      id: 3,
      title: 'Trusted Builder',
      category: 'Trust',
      rarity: 'Rare',
      status: 'Unlocked',
      icon: '🛡',
      description: 'Granted for maintaining strong trust signals and verified project history.',
      progress: 100,
      xpReward: 220
    },
    {
      id: 4,
      title: 'Cloud Specialist',
      category: 'Expertise',
      rarity: 'Epic',
      status: 'Unlocked',
      icon: '☁',
      description: 'Highlights strong delivery in cloud engineering and scalable architecture projects.',
      progress: 100,
      xpReward: 340
    },
    {
      id: 5,
      title: 'Reliable Finisher',
      category: 'Delivery',
      rarity: 'Rare',
      status: 'In Progress',
      icon: '✓',
      description: 'Complete more projects with high satisfaction and on-time delivery.',
      progress: 72,
      xpReward: 180
    },
    {
      id: 6,
      title: 'Client Favorite',
      category: 'Reputation',
      rarity: 'Legendary',
      status: 'In Progress',
      icon: '❤',
      description: 'Reserved for freelancers with exceptional repeat-client satisfaction.',
      progress: 64,
      xpReward: 550
    },
    {
      id: 7,
      title: 'Mentor Mindset',
      category: 'Collaboration',
      rarity: 'Rare',
      status: 'Locked',
      icon: '✦',
      description: 'Earned by maintaining a collaborative and supportive delivery style.',
      progress: 0,
      xpReward: 200
    },
    {
      id: 8,
      title: 'Delivery Master',
      category: 'Performance',
      rarity: 'Legendary',
      status: 'Locked',
      icon: '🏆',
      description: 'One of the highest tier badges for elite consistency and execution quality.',
      progress: 0,
      xpReward: 700
    }
  ];

  get filteredBadges(): BadgeItem[] {
    if (this.selectedFilter === 'All') {
      return this.badges;
    }

    return this.badges.filter(badge => badge.status === this.selectedFilter);
  }

  setFilter(filter: 'All' | BadgeStatus): void {
    this.selectedFilter = filter;
  }

  getRarityClass(rarity: BadgeRarity): string {
    switch (rarity) {
      case 'Legendary':
        return 'rarity-legendary';
      case 'Epic':
        return 'rarity-epic';
      case 'Rare':
        return 'rarity-rare';
      default:
        return 'rarity-common';
    }
  }

  getStatusClass(status: BadgeStatus): string {
    switch (status) {
      case 'Unlocked':
        return 'status-unlocked';
      case 'In Progress':
        return 'status-progress';
      case 'Locked':
        return 'status-locked';
      default:
        return '';
    }
  }

  trackByBadgeId(index: number, badge: BadgeItem): number {
    return badge.id;
  }
}