import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LeaderboardDTO } from '../models/engagement.models';

const BASE = '/api';

@Injectable({ providedIn: 'root' })
export class LeaderboardService {
  constructor(private http: HttpClient) {}

  getGlobal(): Observable<LeaderboardDTO[]> {
    return this.http.get<LeaderboardDTO[]>(`${BASE}/leaderboard`);
  }

  getByGovernorate(gov: string): Observable<LeaderboardDTO[]> {
    return this.http.get<LeaderboardDTO[]>(`${BASE}/leaderboard/governorate/${gov}`);
  }

  recompute(): Observable<void> {
    return this.http.post<void>(`${BASE}/leaderboard/recompute`, {});
  }
}
