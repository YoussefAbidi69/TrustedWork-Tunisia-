import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { AgencyMember, MemberStatus } from '../../../core/models/agency.model';

@Injectable({
  providedIn: 'root'
})
export class AgencyMemberService {
  constructor(private api: ApiService) {}

  getMembersByAgency(agencyId: number): Observable<AgencyMember[]> {
    return this.api.get<AgencyMember[]>(`/agency-members/agency/${agencyId}`);
  }

  getActiveMembersByAgency(agencyId: number): Observable<AgencyMember[]> {
    return this.api.get<AgencyMember[]>(`/agency-members/agency/${agencyId}/active`);
  }

  getMemberById(id: number): Observable<AgencyMember> {
    return this.api.get<AgencyMember>(`/agency-members/${id}`);
  }

  addMember(agencyId: number, userId: number, role: string): Observable<AgencyMember> {
    return this.api.post<AgencyMember>(`/agency-members/agency/${agencyId}`, { userId, role });
  }

  updateMember(id: number, member: Partial<AgencyMember>): Observable<AgencyMember> {
    return this.api.put<AgencyMember>(`/agency-members/${id}`, member);
  }

  removeMember(id: number): Observable<void> {
    return this.api.delete<void>(`/agency-members/${id}`);
  }
}
