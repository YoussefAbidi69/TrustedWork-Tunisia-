import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Observable } from 'rxjs';
import { ProjectRequest, FreelancerRecommendation } from '../models/recommendation.model';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {
  private endpoint = '/v1/recommendations';

  constructor(private api: ApiService) { }

  getRecommendations(request: ProjectRequest): Observable<FreelancerRecommendation[]> {
    return this.api.post<FreelancerRecommendation[]>(this.endpoint, request);
  }

  healthCheck(): Observable<boolean> {
    return this.api.get<boolean>(`${this.endpoint}/health`);
  }
}
