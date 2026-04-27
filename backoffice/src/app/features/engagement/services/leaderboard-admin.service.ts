import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LeaderboardDTO } from '../models/engagement.models';

const BASE = 'http://localhost:8086/api';

@Injectable({ providedIn: 'root' })
export class LeaderboardAdminService {
  constructor(private http: HttpClient) {}

  getGlobal(): Observable<LeaderboardDTO[]> {
    return this.http.get<LeaderboardDTO[]>(`${BASE}/leaderboard`);
  }

  recompute(): Observable<void> {
    return this.http.post<void>(`${BASE}/leaderboard/recompute`, {});
  }
}
