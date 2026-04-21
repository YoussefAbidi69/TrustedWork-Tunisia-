import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProjectRequest, FreelancerRecommendation } from '../models/recommendation.model';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private apiUrl = 'http://localhost:8085/api/recommendations';

  constructor(private http: HttpClient) { }

  getRecommendations(request: ProjectRequest): Observable<FreelancerRecommendation[]> {
    return this.http.post<FreelancerRecommendation[]>(this.apiUrl, request);
  }

  healthCheck(): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/health`);
  }
}