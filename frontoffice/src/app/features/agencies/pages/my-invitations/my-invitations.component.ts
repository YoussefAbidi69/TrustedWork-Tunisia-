import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AgencyInvitationService } from '../../services/agency-invitation.service';
import { AgencyInvitation, InvitationStatus } from '../../../../core/models/agency.model';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-my-invitations',
  templateUrl: './my-invitations.component.html',
  styleUrls: ['./my-invitations.component.css']
})
export class MyInvitationsComponent implements OnInit {
  invitations: AgencyInvitation[] = [];
  loading = true;
  actionLoadingId: number | null = null;
  successMessage = '';
  errorMessage = '';

  constructor(
    private invitationService: AgencyInvitationService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadInvitations();
  }

  loadInvitations(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user) {
      this.loading = false;
      return;
    }

    this.loading = true;
    this.invitationService.getMyInvitations(user.userId).subscribe({
      next: (data) => {
        // Filter to show only PENDING invitations to accept/decline
        this.invitations = data.filter(inv => inv.status === InvitationStatus.PENDING);
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Erreur lors du chargement des invitations.';
        this.loading = false;
      }
    });
  }

  accept(id: number): void {
    this.actionLoadingId = id;
    this.errorMessage = '';
    this.successMessage = '';

    this.invitationService.acceptInvitation(id).subscribe({
      next: () => {
        this.successMessage = 'Invitation acceptée avec succès ! Redirection...';
        this.actionLoadingId = null;
        this.loadInvitations();
        
        // Auto-refresh logic: emit an event or just reload the app to fetch real-time sidebar, 
        // passing down the newly accepted agency context!
        setTimeout(() => {
          this.router.navigate(['/app/agencies']).then(() => {
             window.location.reload();
          });
        }, 1500);
      },
      error: () => {
        this.errorMessage = "Une erreur est survenue lors de l'acceptation.";
        this.actionLoadingId = null;
      }
    });
  }

  decline(id: number): void {
    this.actionLoadingId = id;
    this.errorMessage = '';
    this.successMessage = '';

    this.invitationService.declineInvitation(id).subscribe({
      next: () => {
        this.successMessage = 'Invitation refusée.';
        this.actionLoadingId = null;
        this.loadInvitations();
      },
      error: () => {
        this.errorMessage = "Une erreur est survenue lors du refus.";
        this.actionLoadingId = null;
      }
    });
  }
}
