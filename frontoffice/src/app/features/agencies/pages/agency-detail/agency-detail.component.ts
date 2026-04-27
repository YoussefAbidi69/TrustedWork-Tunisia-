import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AgencyService } from '../../services/agency.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Agency } from '../../../../core/models/agency.model';

@Component({
  selector: 'app-agency-detail',
  templateUrl: './agency-detail.component.html',
  styleUrls: ['./agency-detail.component.css']
})
export class AgencyDetailComponent implements OnInit, OnDestroy {
  agency: Agency | null = null;
  loading = true;
  activeTab = 'overview';
  userId: number | null = null;
  isMemberOrOwner = false;
  isLeadOfThisAgency = false;
  joinRequestSent = false;
  joining = false;
  quitting = false;

  tabs: { id: string; label: string; icon: string; leadOnly?: boolean; memberOnly?: boolean }[] = [
    { id: 'overview',     label: 'Vue d\'ensemble', icon: 'fa-layer-group' },
    { id: 'members',      label: 'Membres',         icon: 'fa-users' },
    { id: 'projects',     label: 'Projets',          icon: 'fa-diagram-project' },
    { id: 'kanban',       label: 'T\u00e2ches',           icon: 'fa-tasks',        memberOnly: true },
    { id: 'invitations',  label: 'Invitations',      icon: 'fa-paper-plane',  leadOnly: true },
    { id: 'analytics',   label: 'Analytique',       icon: 'fa-chart-line',   leadOnly: true },
    { id: 'requests',     label: 'Demandes d\'adhésion',  icon: 'fa-user-clock', leadOnly: true }
  ];

  get visibleTabs() {
    return this.tabs.filter(tab => {
        if (tab.leadOnly && !this.isLeadOfThisAgency) return false;
        if (tab.memberOnly && !this.isMemberOrOwner) return false;
        return true;
    });
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private agencyService: AgencyService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.userId = authUser.userId;
    }
    
    this.route.params.subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.loadAgencyAndContext(id);
      }
    });

    // Handle query params for tabs
    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.activeTab = params['tab'];
      }
    });
  }

  loadAgencyAndContext(id: number): void {
    this.loading = true;
    
    if (!this.userId) {
      // If not logged in, just load agency
      this.agencyService.getAgencyById(id).subscribe({
        next: (data) => {
          this.agency = data;
          this.loading = false;
        },
        error: () => this.router.navigate(['/app/agencies'])
      });
      return;
    }

    // Check both agency details AND if user is a member
    forkJoin({
      agency: this.agencyService.getAgencyById(id),
      context: this.agencyService.getMyAgencyContext(this.userId)
    }).subscribe({
      next: (res) => {
        this.agency = res.agency;
        const membership = res.context?.memberships?.find(m => m.agencyId === id);
        this.isMemberOrOwner = !!membership;
        this.isLeadOfThisAgency = membership?.role === 'LEAD';
        this.agencyService.setAgencyRole(membership?.role || null);
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.router.navigate(['/app/agencies']);
      }
    });
  }

  requestJoin(): void {
    if (!this.agency || !this.userId || this.isMemberOrOwner || this.joinRequestSent || this.joining) return;
    this.joining = true;
    this.agencyService.requestToJoin(this.agency.id, this.userId).subscribe({
      next: () => {
        this.joinRequestSent = true;
        this.joining = false;
      },
      error: (err) => {
        console.error('Request join error:', err);
        this.joinRequestSent = true;
        this.joining = false;
      }
    });
  }

  setActiveTab(tabId: string): void {
    this.activeTab = tabId;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: tabId },
      queryParamsHandling: 'merge'
    });
  }

  quitAgency(): void {
    if (!this.agency || !this.userId || this.quitting) return;
    
    if (confirm('Êtes-vous sûr de vouloir quitter cette agence ?')) {
      this.quitting = true;
      this.agencyService.quitAgency(this.agency.id, this.userId).subscribe({
        next: () => {
          this.quitting = false;
          alert('Vous avez quitté l\'agence avec succès.');
          this.router.navigate(['/app/agencies']);
        },
        error: (err) => {
          this.quitting = false;
          const msg = err?.error?.message || 'Une erreur est survenue lors du départ de l\'agence.';
          alert(msg);
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.agencyService.setAgencyRole(null);
  }
}
