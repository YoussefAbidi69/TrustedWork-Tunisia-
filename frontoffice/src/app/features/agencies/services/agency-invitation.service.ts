import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { AgencyInvitation, MemberRole } from '../../../core/models/agency.model';

@Injectable({
  providedIn: 'root'
})
export class AgencyInvitationService {

  constructor(private api: ApiService) { }

  createInvitation(agencyId: number, senderId: number, receiverId: number, message?: string): Observable<AgencyInvitation> {
    return this.api.post<AgencyInvitation>(`/agencies/${agencyId}/invitations`, { senderId, receiverId, message });
  }

  getInvitationsByAgency(agencyId: number): Observable<AgencyInvitation[]> {
    return this.api.get<AgencyInvitation[]>(`/agencies/${agencyId}/invitations`);
  }

  getMyInvitations(userId: number): Observable<AgencyInvitation[]> {
    return this.api.get<AgencyInvitation[]>(`/invitations/received?userId=${userId}`);
  }

  acceptInvitation(id: number): Observable<AgencyInvitation> {
    return this.api.patch<AgencyInvitation>(`/invitations/${id}/accept`, {});
  }

  declineInvitation(id: number): Observable<AgencyInvitation> {
    return this.api.patch<AgencyInvitation>(`/invitations/${id}/decline`, {});
  }

  deleteInvitation(id: number, agencyId: number): Observable<void> {
    return this.api.delete<void>(`/agencies/${agencyId}/invitations/${id}`);
  }
}
