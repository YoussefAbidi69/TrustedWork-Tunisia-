import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChallengeDTO } from '../models/engagement.models';

const BASE = 'http://localhost:8086/api/challenges';

@Injectable({ providedIn: 'root' })
export class ChallengeAdminService {
  constructor(private http: HttpClient) {}

  getAllChallenges(): Observable<ChallengeDTO[]> {
    return this.http.get<ChallengeDTO[]>(`${BASE}/admin`);
  }

  createChallenge(challenge: ChallengeDTO): Observable<ChallengeDTO> {
    return this.http.post<ChallengeDTO>(`${BASE}/admin`, challenge);
  }

  deleteChallenge(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/admin/${id}`);
  }

  updateChallenge(id: number, challenge: ChallengeDTO): Observable<ChallengeDTO> {
    return this.http.put<ChallengeDTO>(`${BASE}/admin/${id}`, challenge);
  }
}
