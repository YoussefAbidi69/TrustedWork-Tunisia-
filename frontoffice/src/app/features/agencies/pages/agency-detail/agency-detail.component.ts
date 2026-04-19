import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AgencyService } from '../../services/agency.service';
import { Agency } from '../../../../core/models/agency.model';

@Component({
  selector: 'app-agency-detail',
  templateUrl: './agency-detail.component.html',
  styleUrls: ['./agency-detail.component.css']
})
export class AgencyDetailComponent implements OnInit {
  agency: Agency | null = null;
  loading = true;
  activeTab = 'overview';

  tabs = [
    { id: 'overview', label: 'Vue d\'ensemble', icon: 'fa-layer-group' },
    { id: 'members', label: 'Membres', icon: 'fa-users' },
    { id: 'projects', label: 'Projets', icon: 'fa-diagram-project' },
    { id: 'kanban', label: 'Tâches', icon: 'fa-tasks' },
    { id: 'invitations', label: 'Invitations', icon: 'fa-paper-plane' },
    { id: 'analytics', label: 'Analytique', icon: 'fa-chart-line' },
    { id: 'logs', label: 'Historique', icon: 'fa-history' }
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private agencyService: AgencyService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.loadAgency(id);
      }
    });

    // Handle query params for tabs
    this.route.queryParams.subscribe(params => {
      if (params['tab']) {
        this.activeTab = params['tab'];
      }
    });
  }

  loadAgency(id: number): void {
    this.loading = true;
    this.agencyService.getAgencyById(id).subscribe({
      next: (data) => {
        this.agency = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.router.navigate(['/app/agencies']);
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
}
