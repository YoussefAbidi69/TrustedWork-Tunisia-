import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AgencyService } from '../../services/agency.service';
import { AgencyContextDto, AgencyMembershipSummary } from '../../../../core/models/agency.model';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-agency-list',
  templateUrl: './agency-list.component.html',
  styleUrls: ['./agency-list.component.css']
})
export class AgencyListComponent implements OnInit {
  context: AgencyContextDto | null = null;
  loading = true;
  error: string | null = null;
  userId: number | null = null;

  constructor(
    private agencyService: AgencyService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.userId = authUser.userId;
      this.loadAgencyContext();
    }
  }

  loadAgencyContext(): void {
    if (!this.userId) return;
    this.loading = true;
    this.agencyService.getMyAgencyContext(this.userId).subscribe({
      next: (data) => {
        this.context = data;
        this.loading = false;
        
        // Auto-enter if only 1 agency membership
        if (data.hasMemberships && data.memberships.length === 1) {
          const agencyId = data.memberships[0].agencyId;
          this.router.navigate(['/app/agencies', agencyId]);
        } else if (!data.hasMemberships) {
          // If no memberships, automatically route to the create agency page
          this.router.navigate(['/app/agencies/new']);
        }
      },
      error: (err) => {
        this.error = "Erreur lors du chargement des agences.";
        this.loading = false;
        console.error(err);
      }
    });
  }

  getActiveCount(): number {
    if (!this.context || !this.context.memberships) return 0;
    return this.context.memberships.filter(m => m.status === 'ACTIVE').length;
  }
}
