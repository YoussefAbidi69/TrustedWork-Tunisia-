import { Component, Input, OnInit } from '@angular/core';
import { AgencyService } from '../../services/agency.service';
import { AuthService } from '../../../../core/services/auth.service';
import { AgencyJoinRequest } from '../../../../core/models/agency.model';

@Component({
  selector: 'app-agency-join-requests',
  templateUrl: './agency-join-requests.component.html',
  styleUrls: ['./agency-join-requests.component.css']
})
export class AgencyJoinRequestsComponent implements OnInit {
  @Input() agencyId!: number;

  requests: AgencyJoinRequest[] = [];
  loading = true;
  actionLoadingId: number | null = null;
  errorMessage = '';
  successMessage = '';
  filterStatus: string = 'PENDING';

  private userId: number | null = null;

  constructor(
    private agencyService: AgencyService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    if (user) this.userId = user.userId;
    this.loadRequests();
  }

  loadRequests(): void {
    if (!this.userId) return;
    this.loading = true;
    this.errorMessage = '';

    this.agencyService.getJoinRequests(this.agencyId, this.userId, this.filterStatus || undefined)
      .subscribe({
        next: (data) => {
          this.requests = data;
          this.loading = false;
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Impossible de charger les demandes.';
          this.loading = false;
        }
      });
  }

  setFilter(status: string): void {
    this.filterStatus = status;
    this.loadRequests();
  }

  respond(requestId: number, agencyId: number, status: 'ACCEPTED' | 'DECLINED'): void {
    if (!this.userId || this.actionLoadingId === requestId) return;

    this.actionLoadingId = requestId;
    this.errorMessage = '';
    this.successMessage = '';

    this.agencyService.respondToJoinRequest(agencyId, requestId, this.userId, status)
      .subscribe({
        next: (updated) => {
          const idx = this.requests.findIndex(r => r.id === requestId);
          if (idx !== -1) {
            if (this.filterStatus === 'PENDING') {
              // Remove it from the pending view immediately
              this.requests.splice(idx, 1);
            } else {
              this.requests[idx] = updated;
            }
          }
          this.successMessage = status === 'ACCEPTED'
            ? 'Demande accept\u00e9e \u2014 le membre a rejoint l\'agence.'
            : 'Demande refus\u00e9e.';
          this.actionLoadingId = null;
          setTimeout(() => this.successMessage = '', 3000);
        },
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Une erreur est survenue.';
          this.actionLoadingId = null;
        }
      });
  }

  getInitials(req: AgencyJoinRequest): string {
    const first = req.requesterFirstName?.charAt(0) || '';
    const last = req.requesterLastName?.charAt(0) || '';
    return (first + last).toUpperCase() || '?';
  }

  getFullName(req: AgencyJoinRequest): string {
    if (req.requesterFirstName || req.requesterLastName) {
      return `${req.requesterFirstName || ''} ${req.requesterLastName || ''}`.trim();
    }
    return `Utilisateur #${req.requesterId}`;
  }
}
