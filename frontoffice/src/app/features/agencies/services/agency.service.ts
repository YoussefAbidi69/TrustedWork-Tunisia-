import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { Agency, AgencyRequest, AgencyContextDto } from '../../../core/models/agency.model';

@Injectable({
  providedIn: 'root'
})
export class AgencyService {
  constructor(private api: ApiService) {}

  getAllAgencies(): Observable<Agency[]> {
    return this.api.get<Agency[]>('/agencies');
  }

  getAgencyById(id: number): Observable<Agency> {
    return this.api.get<Agency>(`/agencies/${id}`);
  }

  getAgenciesByCreator(creatorId: number): Observable<Agency[]> {
    return this.api.get<Agency[]>(`/agencies/creator/${creatorId}`);
  }

  createAgency(agency: AgencyRequest): Observable<Agency> {
    return this.api.post<Agency>('/agencies', agency);
  }

  updateAgency(id: number, agency: Partial<Agency>): Observable<Agency> {
    return this.api.put<Agency>(`/agencies/${id}`, agency);
  }

  deleteAgency(id: number): Observable<void> {
    return this.api.delete<void>(`/agencies/${id}`);
  }

  getMyAgencyContext(userId: number): Observable<AgencyContextDto> {
    return this.api.get<AgencyContextDto>(`/agencies/my-context/${userId}`);
  }

  getMyAgencies(userId: number): Observable<Agency[]> {
    return this.api.get<Agency[]>(`/agencies/my-agencies/${userId}`);
  }

  getAvailableFreelancers(agencyId: number): Observable<any[]> {
    return this.api.get<any[]>(`/agencies/${agencyId}/available-freelancers`);
  }
}
