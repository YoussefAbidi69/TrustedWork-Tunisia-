import { Component, Input, OnInit } from '@angular/core';
import { AgencyInvitationService } from '../../services/agency-invitation.service';
import { AgencyInvitation, InvitationStatus } from '../../../../core/models/agency.model';
import { AgencyService } from '../../services/agency.service';
import { AuthService } from '../../../../core/services/auth.service';
import { PublicUserDTO } from '../../../../core/models/user.model';

@Component({
  selector: 'app-agency-invitations',
  templateUrl: './agency-invitations.component.html',
  styleUrls: ['./agency-invitations.component.css']
})
export class AgencyInvitationsComponent implements OnInit {
  @Input() agencyId!: number;
  invitations: AgencyInvitation[] = [];
  availableFreelancers: PublicUserDTO[] = [];
  
  loading = true;
  loadingFreelancers = true;
  invitingId: number | null = null;
  userId: number | null = null;

  constructor(
    private invitationService: AgencyInvitationService,
    private agencyService: AgencyService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.userId = authUser.userId || (authUser as any).id;
      console.log('[AgencyInvitations] Initialized with userId =', this.userId);
    }

    if (this.agencyId) {
      this.loadInvitations();
      this.loadAvailableFreelancers();
    }
  }

  loadAvailableFreelancers(): void {
    this.loadingFreelancers = true;
    this.agencyService.getAvailableFreelancers(this.agencyId).subscribe({
      next: (data) => {
        this.availableFreelancers = data;
        this.loadingFreelancers = false;
      },
      error: (err) => {
        console.error(err);
        this.loadingFreelancers = false;
      }
    });
  }

  loadInvitations(): void {
    this.loading = true;
    this.invitationService.getInvitationsByAgency(this.agencyId).subscribe({
      next: (data) => {
        this.invitations = data;
        this.loading = false;
      },
      error: (err: any) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  sendInvitationByUserId(freelancerId: number): void {
    console.log('[AgencyInvitations] inviteFreelancer clicked! targetUserId =', freelancerId);
    console.log('[AgencyInvitations] current context: userId =', this.userId, ' agencyId =', this.agencyId);

    if (!this.userId || !this.agencyId) {
      console.warn('[AgencyInvitations] ABORTING: Missing userId or agencyId!');
      return;
    }

    this.invitingId = freelancerId;
    this.invitationService.createInvitation(this.agencyId, this.userId, freelancerId).subscribe({
      next: () => {
        console.log('[AgencyInvitations] SUCCESS! Invitation sent.');
        this.invitingId = null;
        this.loadInvitations();
        this.loadAvailableFreelancers(); // Update list to remove invited user
        alert('Invitation envoyée avec succès !');
      },
      error: (err: any) => {
        console.error('[AgencyInvitations] ERROR sending invitation:', err);
        this.invitingId = null;
        const errMsg = err.error?.message || err.message || 'Erreur inconnue';
        alert('Erreur: ' + errMsg);
      }
    });
  }

  isAlreadyInvited(freelancerId: number): boolean {
    return this.invitations.some(inv => 
      inv.receiverId === freelancerId && 
      (inv.status.toString() === 'PENDING' || inv.status.toString() === 'ACCEPTED')
    );
  }

  cancelInvitation(id: number): void {
    this.invitationService.deleteInvitation(id).subscribe({
      next: () => {
        this.loadInvitations();
        this.loadAvailableFreelancers(); // Put them back in available list
      },
      error: (err) => console.error(err)
    });
  }

  getStatusClass(status: string): string {
    switch(status) {
      case InvitationStatus.ACCEPTED: return 'status--accepted';
      case InvitationStatus.REJECTED: return 'status--rejected';
      case InvitationStatus.EXPIRED: return 'status--expired';
      default: return 'status--pending';
    }
  }
}
