import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { Agency, AgencyRequest, AgencyContextDto, AgencyJoinRequest, MemberRole, AgencyAnalytics } from '../../../core/models/agency.model';

@Injectable({
  providedIn: 'root'
})
export class AgencyService {
  private roleSubject = new BehaviorSubject<string | null>(null);
  currentAgencyRole$ = this.roleSubject.asObservable();

  constructor(private api: ApiService) {}

  setAgencyRole(role: string | null) {
    this.roleSubject.next(role);
  }

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

  deleteAgency(id: number, userId: number): Observable<void> {
    return this.api.delete<void>(`/agencies/${id}?userId=${userId}`);
  }

  getMyAgencyContext(userId: number): Observable<AgencyContextDto> {
    return this.api.get<AgencyContextDto>(`/agencies/my-context/${userId}`);
  }

  getMyAgencies(userId: number): Observable<Agency[]> {
    return this.api.get<Agency[]>(`/agencies/my-agencies/${userId}`);
  }

  getAvailableFreelancers(agencyId: number, userId: number, search?: string): Observable<any[]> {
    let url = `/agencies/${agencyId}/available-freelancers?userId=${userId}`;
    if (search) {
      url += `&search=${encodeURIComponent(search)}`;
    }
    return this.api.get<any[]>(url);
  }

  searchUsersByEmail(agencyId: number, email: string): Observable<any[]> {
    return this.api.get<any[]>(`/users/search?email=${encodeURIComponent(email)}&agencyId=${agencyId}`);
  }

  updateMemberRole(agencyId: number, userId: number, role: string, requesterId: number): Observable<any> {
    return this.api.patch<any>(`/agencies/${agencyId}/members/${userId}?requesterId=${requesterId}`, { role });
  }

  getJoinRequests(agencyId: number, ownerId: number, status?: string): Observable<AgencyJoinRequest[]> {
    let url = `/agencies/${agencyId}/requests?ownerId=${ownerId}`;
    if (status) url += `&status=${status}`;
    return this.api.get<AgencyJoinRequest[]>(url);
  }

  respondToJoinRequest(agencyId: number, requestId: number, ownerId: number, status: 'ACCEPTED' | 'DECLINED'): Observable<AgencyJoinRequest> {
    return this.api.patch<AgencyJoinRequest>(`/agencies/${agencyId}/requests/${requestId}?ownerId=${ownerId}`, { status });
  }

  requestToJoin(agencyId: number, requesterId: number, message?: string): Observable<any> {
    const payload = {
      requesterId: requesterId,
      message: message
    };
    return this.api.post<any>(`/agencies/${agencyId}/requests`, payload);
  }

  quitAgency(agencyId: number, userId: number): Observable<any> {
    return this.api.post<any>(`/agencies/${agencyId}/quit?userId=${userId}`, {});
  }

  getAgencyAnalytics(agencyId: number, userId: number): Observable<AgencyAnalytics> {
    return this.api.get<AgencyAnalytics>(`/agencies/${agencyId}/analytics?userId=${userId}`);
  }
}
