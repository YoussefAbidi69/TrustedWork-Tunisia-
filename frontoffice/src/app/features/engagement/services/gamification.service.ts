import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, Subject, tap } from 'rxjs';
import { GrowthProfileDTO, BadgeDTO } from '../models/engagement.models';

const BASE = '/api';

export interface RewardEvent {
  type: 'XP_GAIN' | 'BADGE_UNLOCK' | 'LEVEL_UP';
  amount?: number;
  badge?: BadgeDTO;
  newLevel?: number;
  message?: string;
}

@Injectable({ providedIn: 'root' })
export class GamificationService {
  private profileSubject = new BehaviorSubject<GrowthProfileDTO | null>(null);
  public profile$ = this.profileSubject.asObservable();

  private badgesSubject = new BehaviorSubject<BadgeDTO[]>([]);
  public badges$ = this.badgesSubject.asObservable();

  public rewards$ = new Subject<RewardEvent>();

  constructor(private http: HttpClient) {}

  // --- Core Data Fetchers ---

  refreshProfile(): void {
    this.http.get<GrowthProfileDTO>(`${BASE}/gamification/profile`).subscribe(p => {
      const oldLevel = this.profileSubject.value?.level;
      if (oldLevel && p.level > oldLevel) {
        this.rewards$.next({ type: 'LEVEL_UP', newLevel: p.level });
      }
      this.profileSubject.next(p);
    });
  }

  refreshBadges(): void {
    this.http.get<BadgeDTO[]>(`${BASE}/gamification/badges`).subscribe(newBadges => {
      const currentBadges = this.badgesSubject.value;
      if (currentBadges.length > 0 && newBadges.length > currentBadges.length) {
        // Detect the new badge (safely pick the last one or diff)
        const lastBadge = newBadges[newBadges.length - 1];
        this.rewards$.next({ type: 'BADGE_UNLOCK', badge: lastBadge });
      }
      this.badgesSubject.next(newBadges);
    });
  }

  // --- Reward Emitters (Manually triggered from components) ---

  notifyXpGain(amount: number, reason: string): void {
    this.rewards$.next({ type: 'XP_GAIN', amount, message: reason });
    this.refreshProfile();
    // After XP gain, always check if a badge was earned
    setTimeout(() => this.refreshBadges(), 1000);
  }

  // --- Passive Getters (for backward compat or one-offs) ---

  getMyProfile(): Observable<GrowthProfileDTO> {
    return this.http.get<GrowthProfileDTO>(`${BASE}/gamification/profile`).pipe(
      tap(p => this.profileSubject.next(p))
    );
  }

  getMyBadges(): Observable<BadgeDTO[]> {
    return this.http.get<BadgeDTO[]>(`${BASE}/gamification/badges`).pipe(
      tap(b => this.badgesSubject.next(b))
    );
  }

  getEngagementScore(): Observable<{ engagementScore: number }> {
    return this.http.get<{ engagementScore: number }>(`${BASE}/gamification/score`);
  }

  getAnalytics(): Observable<any> {
    return this.http.get<any>(`${BASE}/analytics/me`);
  }

  getChurnPrediction(userId: number): Observable<any> {
    return this.http.get<any>(`${BASE}/analytics/churn-prediction/${userId}`);
  }

  getModelStats(): Observable<any> {
    return this.http.get<any>(`${BASE}/analytics/model/stats`);
  }
}

