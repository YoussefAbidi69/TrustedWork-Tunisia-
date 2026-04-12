import { Component, OnInit } from '@angular/core';
import { FreelancerProfile } from '../../../core/models/freelancer.model';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';

@Component({
  selector: 'app-profiles-list',
  templateUrl: './profiles-list.component.html',
  styleUrls: ['./profiles-list.component.css']
})
export class ProfilesListComponent implements OnInit {

  profiles: FreelancerProfile[] = [];
  filteredProfiles: FreelancerProfile[] = [];
  loading = true;
  errorMsg = '';

  searchTerm = '';
  selectedRegion = '';
  selectedStatus = '';

  regions: string[] = [];

  constructor(private profileService: FreelancerProfileService) {}

  ngOnInit(): void {
    this.loadProfiles();
  }

  loadProfiles(): void {
    this.loading = true;
    this.errorMsg = '';

    this.profileService.getAllProfiles().subscribe({
      next: (data) => {
        this.profiles = data;
        this.filteredProfiles = data;
        this.regions = [...new Set(data.map(p => p.region).filter(r => !!r))].sort();
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors du chargement des profils';
        this.loading = false;
        console.error(err);
      }
    });
  }

  applyFilters(): void {
    let result = [...this.profiles];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(p =>
        (p.headline || '').toLowerCase().includes(term) ||
        (p.bio || '').toLowerCase().includes(term)
      );
    }

    if (this.selectedRegion) {
      result = result.filter(p => p.region === this.selectedRegion);
    }

    if (this.selectedStatus) {
      result = result.filter(p => p.availabilityStatus === this.selectedStatus);
    }

    this.filteredProfiles = result;
  }

  getStatusBadge(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'badge-success';
      case 'BUSY':
        return 'badge-warning';
      case 'ON_VACATION':
        return 'badge-danger';
      default:
        return 'badge-muted';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'Disponible';
      case 'BUSY':
        return 'Occupé';
      case 'ON_VACATION':
        return 'En vacances';
      default:
        return status || '—';
    }
  }

  getScoreClass(score: number): string {
    if (score >= 80) return 'text-success';
    if (score >= 50) return 'text-warning';
    return 'text-danger';
  }
}