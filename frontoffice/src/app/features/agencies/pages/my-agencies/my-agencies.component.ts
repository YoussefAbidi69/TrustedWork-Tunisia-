import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AgencyService } from '../../services/agency.service';
import { AgencyContextDto } from '../../../../core/models/agency.model';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-my-agencies',
  templateUrl: './my-agencies.component.html',
  styleUrls: ['./my-agencies.component.css']
})
export class MyAgenciesComponent implements OnInit {
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
        // Filter out LEAD roles to strictly show memberships where the user is just a MEMBER?
        // Prompt says: "Filtered to the user's memberships via GET /agencies?role=member"
        // Since we are using context.memberships, we can just filter them directly:
        const memberOnly = data.memberships.filter(m => m.role === 'MEMBER');
        this.context = {
          hasMemberships: memberOnly.length > 0,
          memberships: memberOnly
        };
        this.loading = false;
      },
      error: (err) => {
        this.error = "Erreur lors du chargement de vos agences.";
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
