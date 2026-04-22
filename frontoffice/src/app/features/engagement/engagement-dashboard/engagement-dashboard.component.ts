import { Component, OnInit } from '@angular/core';
import { GamificationService } from '../services/gamification.service';
import { EventService } from '../services/event.service';
import { LeaderboardService } from '../services/leaderboard.service';
import { GrowthProfileDTO, EventDTO, LeaderboardDTO } from '../models/engagement.models';

@Component({
  selector: 'app-engagement-dashboard',
  templateUrl: './engagement-dashboard.component.html',
  styleUrls: ['./engagement-dashboard.component.css']
})
export class EngagementDashboardComponent implements OnInit {
  profile: GrowthProfileDTO | null = null;
  upcomingEvents: EventDTO[] = [];
  topLeaders: LeaderboardDTO[] = [];
  score = 0;
  loading = true;

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
    return Math.round(this.score * 100);
  }
}
