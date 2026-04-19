import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { AgencyInvitation, MemberRole } from '../../../core/models/agency.model';

@Injectable({
  providedIn: 'root'
})
export class AgencyInvitationService {

  constructor(private api: ApiService) { }

  getMyInvitations(userId: number): Observable<AgencyInvitation[]> {
    return this.api.get<AgencyInvitation[]>(`/agency-invitations/user/${userId}`);
  }

  acceptInvitation(invitationId: number): Observable<AgencyInvitation> {
    return this.api.put<AgencyInvitation>(`/agency-invitations/${invitationId}/status`, { status: 'ACCEPTED' });
  }

  declineInvitation(invitationId: number): Observable<AgencyInvitation> {
    return this.api.put<AgencyInvitation>(`/agency-invitations/${invitationId}/status`, { status: 'DECLINED' });
  }

  getInvitationsByAgency(agencyId: number): Observable<AgencyInvitation[]> {
    return this.api.get<AgencyInvitation[]>(`/agency-invitations/agency/${agencyId}`);
  }

  createInvitation(agencyId: number, senderId: number, receiverId: number): Observable<AgencyInvitation> {
    return this.api.post<AgencyInvitation>(`/agency-invitations/agency/${agencyId}`, {
      senderId,
      receiverId,
      proposedRole: MemberRole.MEMBER
    });
  }

  deleteInvitation(invitationId: number): Observable<void> {
    return this.api.delete<void>(`/agency-invitations/${invitationId}`);
  }
}

