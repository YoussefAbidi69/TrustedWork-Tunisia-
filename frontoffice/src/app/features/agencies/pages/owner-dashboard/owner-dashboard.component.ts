import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { AgencyService } from '../../services/agency.service';
import { AgencyMemberService } from '../../services/agency-member.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Agency, AgencyMember } from '../../../../core/models/agency.model';

@Component({
  selector: 'app-owner-dashboard',
  templateUrl: './owner-dashboard.component.html',
  styleUrls: ['./owner-dashboard.component.css']
})
export class OwnerDashboardComponent implements OnInit {
  agency: Agency | null = null;
  allOwnedAgencies: Agency[] = [];
  loading = true;
  activeTab = 'overview'; // 'overview' | 'requests' | 'members'
  
  members: AgencyMember[] = [];
  requests: any[] = []; // Adjust type to match backend DTO for join requests
  toastMessage: string | null = null;
  userId: number | null = null;
  
  constructor(
    private agencyService: AgencyService,
    private memberService: AgencyMemberService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}
 
  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.userId = user.userId;

    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.activeTab = params['tab'];
      }
    });

    this.loadOwnedAgencies();
  }

  loadOwnedAgencies(): void {
    if (!this.userId) return;
    this.loading = true;
    this.agencyService.getAgenciesByCreator(this.userId).subscribe({
      next: (agencies) => {
        this.allOwnedAgencies = agencies || [];
        if (this.allOwnedAgencies.length > 0) {
          // Select first by default if nothing selected
          if (!this.agency) {
            this.selectAgency(this.allOwnedAgencies[0]);
          }
        } else {
          this.router.navigate(['/app/agencies']);
        }
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  selectAgency(agency: Agency): void {
    this.agency = agency;
    this.loadMembers();
    this.loadRequests();
  }

  loadMembers(): void {
    if (!this.agency) return;
    this.memberService.getMembersByAgency(this.agency.id).subscribe({
      next: (data) => this.members = data,
      error: (err) => console.error(err)
    });
  }

  loadRequests(): void {
    if (!this.agency || !this.userId) return;
    this.agencyService.getJoinRequests(this.agency.id, this.userId).subscribe({
      next: (data) => this.requests = data,
      error: (err) => console.error(err)
    });
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  handleRequest(requestId: number, status: 'ACCEPTED' | 'DECLINED'): void {
    if (!this.agency || !this.userId) return;
    this.agencyService.respondToJoinRequest(this.agency.id, requestId, this.userId, status).subscribe({
      next: () => {
        // Remove the request from list
        this.requests = this.requests.filter(r => r.id !== requestId);
        this.toastMessage = `Demande ${status === 'ACCEPTED' ? 'acceptée' : 'déclinée'}.`;
        if (status === 'ACCEPTED') this.loadMembers(); // Refresh members list
        // Hide toast after 3s
        setTimeout(() => this.toastMessage = null, 3000);
      },
      error: (err) => {
        console.error(err);
        this.toastMessage = "Erreur lors de l'opération.";
        setTimeout(() => this.toastMessage = null, 3000);
      }
    });
  }
}
