import { Component } from '@angular/core';

type ChallengeDifficulty = 'Easy' | 'Medium' | 'Hard';
type ChallengeStatus = 'In progress' | 'Completed' | 'Locked';

interface ChallengeItem {
  id: number;
  title: string;
  category: string;
  difficulty: ChallengeDifficulty;
  status: ChallengeStatus;
  progress: number;
  reward: string;
  deadline: string;
  description: string;
  goals: string[];
}

@Component({
  selector: 'app-challenges',
  templateUrl: './challenges.component.html',
  styleUrls: ['./challenges.component.css']
})
export class ChallengesComponent {
  readonly challenges: ChallengeItem[] = [
    {
      id: 1,
      title: 'Complete 3 verified applications',
      category: 'Growth',
      difficulty: 'Easy',
      status: 'In progress',
      progress: 66,
      reward: '+120 XP',
      deadline: '3 days left',
      description: 'Increase your visibility by sending high-quality applications to verified opportunities.',
      goals: ['Submit 3 applications', 'Keep profile updated', 'Use tailored proposal']
    },
    {
      id: 2,
      title: 'Attend one premium networking event',
      category: 'Community',
      difficulty: 'Medium',
      status: 'In progress',
      progress: 40,
      reward: 'Community badge',
      deadline: '5 days left',
      description: 'Boost your credibility by joining a trusted networking or workshop session.',
      goals: ['Register for event', 'Attend event', 'Leave event feedback']
    },
    {
      id: 3,
      title: 'Win a recruiter shortlist',
      category: 'Talent',
      difficulty: 'Hard',
      status: 'Locked',
      progress: 0,
      reward: '+300 XP',
      deadline: 'Unlock after level 4',
      description: 'Position yourself for recruiter-led shortlists on premium opportunities.',
      goals: ['Improve trust score', 'Finish KYC', 'Raise profile match']
    }
  ];

  getDifficultyClass(level: ChallengeDifficulty): string {
    switch (level) {
      case 'Easy':
        return 'difficulty-easy';
      case 'Medium':
        return 'difficulty-medium';
      case 'Hard':
        return 'difficulty-hard';
      default:
        return '';
    }
  }

  getStatusClass(status: ChallengeStatus): string {
    switch (status) {
      case 'Completed':
        return 'status-completed';
      case 'In progress':
        return 'status-progress';
      case 'Locked':
        return 'status-locked';
      default:
        return '';
    }
  }

  trackByChallenge(index: number, item: ChallengeItem): number {
    return item.id;
  }

  trackByText(index: number, item: string): string {
    return item;
  }
}