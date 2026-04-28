import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { RecommendationFilter, RecommendationResponse } from '../models/recommendation.model';
import { HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class RecommendationService {

  constructor(private api: ApiService) { }

  getRecommendedFreelancers(agencyId: number, filters: RecommendationFilter, page: number, size: number): Observable<{success: boolean, data: RecommendationResponse}> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters.minScore !== undefined) params = params.set('minScore', filters.minScore.toString());
    if (filters.availability) params = params.set('availability', filters.availability);
    if (filters.sortBy) params = params.set('sortBy', filters.sortBy);
    if (filters.search) params = params.set('search', filters.search);
    if (filters.skills && filters.skills.length > 0) {
      params = params.set('skills', filters.skills.join(','));
    }
    if ((filters as any).refresh) {
      params = params.set('refresh', 'true');
    }

    return this.api.get<{success: boolean, data: RecommendationResponse}>(`/agencies/${agencyId}/recommended-freelancers?${params.toString()}`);
  }

  sendInvitation(agencyId: number, receiverId: number, senderId: number, role: string, message?: string): Observable<any> {
    const payload = {
      receiverId,
      senderId,
      proposedRole: role,
      message: message || ''
    };
    return this.api.post(`/agencies/${agencyId}/invitations`, payload);
  }
}
