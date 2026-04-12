import { Component } from '@angular/core';

interface ProgressPoint {
  label: string;
  xp: number;
}

interface Insight {
  title: string;
  description: string;
  type: 'positive' | 'warning';
}

@Component({
  selector: 'app-progression',
  templateUrl: './progression.component.html',
  styleUrls: ['./progression.component.css']
})
export class ProgressionComponent {

  currentLevel = 12;
  totalXp = 2480;
  weeklyGain = 320;
  streakDays = 9;

  progressData: ProgressPoint[] = [
    { label: 'Jan', xp: 900 },
    { label: 'Feb', xp: 1200 },
    { label: 'Mar', xp: 1500 },
    { label: 'Apr', xp: 1900 },
    { label: 'May', xp: 2200 },
    { label: 'Jun', xp: 2480 }
  ];

  insights: Insight[] = [
    {
      title: 'Strong growth momentum',
      description: 'Your XP increased consistently over the last 3 months.',
      type: 'positive'
    },
    {
      title: 'High activity streak',
      description: 'You maintained activity for 9 consecutive days.',
      type: 'positive'
    },
    {
      title: 'Opportunity to accelerate',
      description: 'Increasing completed contracts will boost your progression.',
      type: 'warning'
    }
  ];

  get maxXp(): number {
    return Math.max(...this.progressData.map(p => p.xp));
  }

  getBarHeight(value: number): number {
    return (value / this.maxXp) * 100;
  }

  trackByLabel(index: number, item: ProgressPoint): string {
    return item.label;
  }
}