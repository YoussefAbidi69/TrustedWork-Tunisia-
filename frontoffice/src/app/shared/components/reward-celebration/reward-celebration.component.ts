import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { GamificationService, RewardEvent } from '../../../features/engagement/services/gamification.service';

@Component({
  selector: 'app-reward-celebration',
  templateUrl: './reward-celebration.component.html',
  styleUrls: ['./reward-celebration.component.css']
})
export class RewardCelebrationComponent implements OnInit, OnDestroy {
  private subscription: Subscription = new Subscription();
  
  activeXpGains: { id: number, amount: number }[] = [];
  activeBadge: RewardEvent | null = null;
  activeLevelUp: number | null = null;
  
  private nextId = 0;

  constructor(private gamService: GamificationService) {}

  ngOnInit(): void {
    this.subscription.add(
      this.gamService.rewards$.subscribe(event => {
        if (event.type === 'XP_GAIN' && event.amount) {
          this.triggerXpEffect(event.amount);
        } else if (event.type === 'BADGE_UNLOCK') {
          this.triggerBadgeReveal(event);
        } else if (event.type === 'LEVEL_UP' && event.newLevel) {
          this.triggerLevelUp(event.newLevel);
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  private triggerXpEffect(amount: number): void {
    const id = this.nextId++;
    this.activeXpGains.push({ id, amount });
    setTimeout(() => {
      this.activeXpGains = this.activeXpGains.filter(x => x.id !== id);
    }, 3000);
  }

  private triggerBadgeReveal(event: RewardEvent): void {
    this.activeBadge = event;
  }

  private triggerLevelUp(level: number): void {
    this.activeLevelUp = level;
    setTimeout(() => this.activeLevelUp = null, 5000);
  }

  closeBadge(): void {
    this.activeBadge = null;
  }
}
