import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChallengeDTO } from '../models/engagement.models';

const BASE = 'http://localhost:8086/api/challenges';

@Injectable({ providedIn: 'root' })
export class ChallengeService {
  constructor(private http: HttpClient) {}

  getActiveChallenges(): Observable<ChallengeDTO[]> {
    return this.http.get<ChallengeDTO[]>(BASE);
  }

  joinChallenge(id: number): Observable<any> {
    return this.http.post<any>(`${BASE}/${id}/join`, {});
  }

  succeedChallenge(id: number): Observable<any> {
    return this.http.post<any>(`${BASE}/${id}/succeed`, {});
  }

  claimReward(id: number): Observable<any> {
    return this.http.post<any>(`${BASE}/${id}/claim`, {});
  }
}
