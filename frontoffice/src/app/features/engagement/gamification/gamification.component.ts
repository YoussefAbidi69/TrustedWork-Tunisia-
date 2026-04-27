import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { GamificationService } from '../services/gamification.service';
import { GrowthProfileDTO, BadgeDTO } from '../models/engagement.models';

@Component({
  selector: 'app-gamification',
  templateUrl: './gamification.component.html',
  styleUrls: ['./gamification.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class GamificationComponent implements OnInit {
  profile: GrowthProfileDTO | null = null;
  badges: BadgeDTO[] = [];
  engagementScore = 0;
  loading = true;
  analytics: any = null;
  churnPrediction: any = null;

  constructor(
    private gamService: GamificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.gamService.profile$.subscribe(p => {
      this.profile = p;
      if(p) {
        this.loading = false;
        // Appel au modèle ML pour la prédiction de churn
        this.gamService.getChurnPrediction(p.userId).subscribe({
          next: (prediction) => {
            this.churnPrediction = prediction;
          }
        });
      }
    });
    this.gamService.badges$.subscribe(b => {
      this.badges = b;
    });
    
    // Initial Load
    this.gamService.refreshProfile();
    this.gamService.refreshBadges();

    this.gamService.getEngagementScore().subscribe({
      next: (s) => { this.engagementScore = s.engagementScore; }
    });

    this.gamService.getAnalytics().subscribe({
      next: (data) => {
        this.analytics = data;
        if (this.profile) {
          this.profile.influenceScore = data.influenceScore;
          // On garde ça pour compatibilité au cas où
          this.profile.churnRisk = data.churnRisk;
        }
      }
    });
  }

  getXpToNextLevel(): number {
    if (!this.profile) return 500;
    return 500 - (this.profile.xpPoints % 500);
  }

  getXpProgress(): number {
    if (!this.profile) return 0;
    return ((this.profile.xpPoints % 500) / 500) * 100;
  }

  getScorePercent(): number {
    return Math.round(this.engagementScore * 100);
  }

  getRarityClass(rarity: string): string {
    const cls: Record<string, string> = {
      COMMON: 'rarity-common',
      RARE: 'rarity-rare',
      EPIC: 'rarity-epic',
      LEGENDARY: 'rarity-legendary'
    };
    return cls[rarity] || '';
  }

  goToRecommendation(item: any, type: 'event' | 'challenge'): void {
    if (type === 'event') {
      this.router.navigate(['/app/engagement/events'], { queryParams: { highlight: item.id } });
    } else {
      this.router.navigate(['/app/engagement/missions'], { queryParams: { highlight: item.id } });
    }
  }

  getRiskColor(prediction: any): string {
    if (!prediction) return '#94A3B8';
    if (prediction.risk_color) return prediction.risk_color;
    
    const colors: Record<string, string> = {
      'HIGH': '#ef4444',
      'MEDIUM': '#f59e0b',
      'LOW': '#10b981'
    };
    return colors[prediction.risk_label] || '#94A3B8';
  }
}

