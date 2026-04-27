import { Component, Input, OnInit } from '@angular/core';
import { AgencyInvitationService } from '../../services/agency-invitation.service';
import { AgencyInvitation, InvitationStatus } from '../../../../core/models/agency.model';

@Component({
  selector: 'app-agency-invitations',
  templateUrl: './agency-invitations.component.html',
  styleUrls: ['./agency-invitations.component.css']
})
export class AgencyInvitationsComponent implements OnInit {
  @Input() agencyId!: number;
  invitations: AgencyInvitation[] = [];
  loading = true;

  constructor(private invitationService: AgencyInvitationService) {}

  ngOnInit(): void {
    if (this.agencyId) {
      this.loadInvitations();
    }
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

  cancelInvitation(id: number): void {
    if (confirm('Voulez-vous vraiment annuler cette invitation ?')) {
      this.invitationService.deleteInvitation(id, this.agencyId).subscribe({
        next: () => {
          // Remove the row on success
          this.invitations = this.invitations.filter(i => i.id !== id);
        },
        error: (err) => alert(err.error?.message || 'Erreur lors de l\'annulation')
      });
    }
  }

  getStatusBadgeClass(status: string): string {
    switch(status.toString()) {
      case 'ACCEPTED': return 'badge--success';
      case 'DECLINED': return 'badge--danger';
      case 'CANCELLED': return 'badge--neutral';
      case 'EXPIRED': return 'badge--neutral';
      default: return 'badge--warning';
    }
  }
}
