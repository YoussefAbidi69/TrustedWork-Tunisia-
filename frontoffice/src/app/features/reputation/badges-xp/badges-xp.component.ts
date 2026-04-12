import { Component } from '@angular/core';

interface XpMilestone {
  level: number;
  title: string;
  xpRequired: number;
  reward: string;
  status: 'Completed' | 'Current' | 'Locked';
}

interface RewardCard {
  title: string;
  description: string;
  icon: string;
  unlocked: boolean;
}

@Component({
  selector: 'app-badges-xp',
  templateUrl: './badges-xp.component.html',
  styleUrls: ['./badges-xp.component.css']
})
export class BadgesXpComponent {
  currentLevel = 12;
  currentXp = 2480;
  currentLevelMinXp = 2200;
  nextLevelXp = 2700;
  totalEarnedBadges = 14;
  completedMilestones = 11;

  milestones: XpMilestone[] = [
    {
      level: 10,
      title: 'Reliable Freelancer',
      xpRequired: 1800,
      reward: 'Priority profile boost',
      status: 'Completed'
    },
    {
      level: 11,
      title: 'Trusted Performer',
      xpRequired: 2100,
      reward: 'Advanced trust visibility',
      status: 'Completed'
    },
    {
      level: 12,
      title: 'Premium Builder',
      xpRequired: 2400,
      reward: 'Featured badge highlight',
      status: 'Current'
    },
    {
      level: 13,
      title: 'Elite Collaborator',
      xpRequired: 2700,
      reward: 'Higher recommendation weight',
      status: 'Locked'
    },
    {
      level: 14,
      title: 'Top Reputation Tier',
      xpRequired: 3100,
      reward: 'Premium marketplace exposure',
      status: 'Locked'
    }
  ];

  rewards: RewardCard[] = [
    {
      title: 'Featured Profile Accent',
      description: 'Your profile gets a stronger premium visual signal across the platform.',
      icon: '✦',
      unlocked: true
    },
    {
      title: 'Higher Search Ranking',
      description: 'Profiles with strong XP progression gain better positioning in relevant searches.',
      icon: '↗',
      unlocked: true
    },
    {
      title: 'Premium Badge Glow',
      description: 'Legendary and epic badges receive a stronger visual highlight.',
      icon: '★',
      unlocked: true
    },
    {
      title: 'Marketplace Priority',
      description: 'Future milestone reward that boosts exposure on opportunity feeds.',
      icon: '⚡',
      unlocked: false
    }
  ];

  get levelProgress(): number {
    const levelRange = this.nextLevelXp - this.currentLevelMinXp;
    const progress = this.currentXp - this.currentLevelMinXp;
    return Math.round((progress / levelRange) * 100);
  }

  get xpRemaining(): number {
    return this.nextLevelXp - this.currentXp;
  }

  get milestoneCompletionRate(): number {
    return Math.round((this.completedMilestones / 14) * 100);
  }

  getStatusClass(status: XpMilestone['status']): string {
    switch (status) {
      case 'Completed':
        return 'status-completed';
      case 'Current':
        return 'status-current';
      case 'Locked':
        return 'status-locked';
      default:
        return '';
    }
  }

  trackByLevel(index: number, item: XpMilestone): number {
    return item.level;
  }

  trackByReward(index: number, item: RewardCard): string {
    return item.title;
  }
}