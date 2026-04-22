import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GrowthProfileDTO, BadgeDTO } from '../models/engagement.models';

const BASE = 'http://localhost:8086/api/gamification/admin';

@Injectable({ providedIn: 'root' })
export class GamificationAdminService {
  constructor(private http: HttpClient) {}

  getUserProfile(userId: number): Observable<GrowthProfileDTO> {
    return this.http.get<GrowthProfileDTO>(`${BASE}/user/${userId}/profile`);
  }

  getProfiles(): Observable<GrowthProfileDTO[]> {
    return this.http.get<GrowthProfileDTO[]>(`${BASE}/profiles`);
  }

  getUserBadges(userId: number): Observable<BadgeDTO[]> {
    return this.http.get<BadgeDTO[]>(`${BASE}/user/${userId}/badges`);
  }

  getUserScore(userId: number): Observable<{ engagementScore: number }> {
    return this.http.get<{ engagementScore: number }>(`${BASE}/user/${userId}/score`);
  }

  removeUserBadge(userId: number, badgeId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/user/${userId}/badges/${badgeId}`);
  }
}
