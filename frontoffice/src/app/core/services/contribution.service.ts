import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contribution } from '../models/community.model';

@Injectable({
  providedIn: 'root'
})
export class ContributionService {
  private readonly baseUrl = `${environment.msCommunity}/api/contributions`;

  constructor(private http: HttpClient) {}

  getByUserId(userId: number): Observable<Contribution> {
    return this.http.get<Contribution>(`${this.baseUrl}/users/${userId}`);
  }

  recordShare(userId: number): Observable<Contribution> {
    return this.http.post<Contribution>(`${this.baseUrl}/users/${userId}/record`, {});
  }
}
