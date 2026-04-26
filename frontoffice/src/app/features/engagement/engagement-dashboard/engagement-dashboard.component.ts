import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { GamificationService } from '../services/gamification.service';
import { EventService } from '../services/event.service';
import { LeaderboardService } from '../services/leaderboard.service';
import { GrowthProfileDTO, EventDTO, LeaderboardDTO } from '../models/engagement.models';

@Component({
  selector: 'app-engagement-dashboard',
  templateUrl: './engagement-dashboard.component.html',
  styleUrls: ['./engagement-dashboard.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class EngagementDashboardComponent implements OnInit {
  profile: GrowthProfileDTO | null = null;
  upcomingEvents: EventDTO[] = [];
  topLeaders: LeaderboardDTO[] = [];
  score = 0;
  loading = true;
  Math = Math;

  constructor(
    private gamService: GamificationService,
    private eventService: EventService,
    private lbService: LeaderboardService
  ) {}

  ngOnInit(): void {
    this.gamService.getMyProfile().subscribe({
      next: (p) => { this.profile = p; this.loading = false; }
    });
    this.gamService.getEngagementScore().subscribe({
      next: (s) => { this.score = s.engagementScore; }
    });
    this.eventService.getAllEvents().subscribe({
      next: (evts) => {
        this.upcomingEvents = evts
          .filter(e => e.status === 'UPCOMING')
          .slice(0, 3);
      }
    });
    this.lbService.getGlobal().subscribe({
      next: (lb) => { this.topLeaders = lb.slice(0, 5); }
    });
  }

  getXpProgress(): number {
    if (!this.profile) return 0;
    return ((this.profile.xpPoints % 500) / 500) * 100;
  }

  getScorePercent(): number {
    const score = this.profile?.engagementScore ?? 0;
    // Si le score est déjà entre 0 et 1 (ex: 0.22) → multiplier par 100
    // Si le score est déjà entre 0 et 100 → retourner directement
    if (score <= 1) {
      return Math.round(score * 100);
    }
    return Math.round(score);
  }
}
