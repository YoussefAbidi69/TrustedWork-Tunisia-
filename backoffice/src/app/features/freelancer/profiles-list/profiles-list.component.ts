import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { FreelancerProfile } from '../../../core/models/freelancer.model';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';

interface FreelancerProfileViewModel extends FreelancerProfile {
  fullName: string;
  initials: string;
}

@Component({
  selector: 'app-profiles-list',
  templateUrl: './profiles-list.component.html',
  styleUrls: ['./profiles-list.component.css']
})
export class ProfilesListComponent implements OnInit {

  profiles: FreelancerProfileViewModel[] = [];
  filteredProfiles: FreelancerProfileViewModel[] = [];
  paginatedProfiles: FreelancerProfileViewModel[] = [];

  loading = true;
  errorMsg = '';

  searchTerm = '';
  selectedRegion = '';
  selectedStatus: 'AVAILABLE' | 'BUSY' | 'ON_VACATION' | '' = '';
  minRate: number | null = null;
  maxRate: number | null = null;

  regions: string[] = [];

  // Pagination
  currentPage = 1;
  pageSize = 8;
  pageSizeOptions = [5, 8, 10, 15, 20];

  constructor(
    private profileService: FreelancerProfileService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadProfiles();
  }

  loadProfiles(): void {
    this.loading = true;
    this.errorMsg = '';

    this.profileService.searchProfiles({
      region: this.selectedRegion || undefined,
      availability: this.selectedStatus || undefined,
      minRate: this.minRate,
      maxRate: this.maxRate
    }).subscribe({
      next: (data) => {
        this.regions = [...new Set(
          data
            .map(p => p.region)
            .filter((r): r is string => !!r && r.trim().length > 0)
        )].sort((a, b) => a.localeCompare(b));

        if (data.length === 0) {
          this.profiles = [];
          this.filteredProfiles = [];
          this.paginatedProfiles = [];
          this.loading = false;
          return;
        }

        const userRequests = data.map(profile =>
          this.http.get<any>(`http://localhost:8081/api/identity/users/${profile.userId}`).pipe(
            catchError(() => of({ firstName: '', lastName: '' }))
          )
        );

        forkJoin(userRequests).subscribe({
          next: (users) => {
            this.profiles = data.map((profile, index) => {
              const user = users[index] || {};
              const firstName = (user.firstName || '').trim();
              const lastName = (user.lastName || '').trim();
              const fullName = `${firstName} ${lastName}`.trim();

              return {
                ...profile,
                fullName: fullName || profile.headline || `Freelancer #${profile.userId}`,
                initials: this.buildInitials(firstName, lastName, profile.headline)
              };
            });

            this.applyFilters(false);
            this.loading = false;
          },
          error: () => {
            this.profiles = data.map(profile => ({
              ...profile,
              fullName: profile.headline || `Freelancer #${profile.userId}`,
              initials: profile.headline?.charAt(0)?.toUpperCase() || 'F'
            }));

            this.applyFilters(false);
            this.loading = false;
          }
        });
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors du chargement des profils';
        this.loading = false;
        console.error(err);
      }
    });
  }

  applyFilters(reloadFromBackend: boolean = true): void {
    if (reloadFromBackend) {
      this.loadProfiles();
      return;
    }

    let result = [...this.profiles];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(p =>
        (p.fullName || '').toLowerCase().includes(term) ||
        (p.headline || '').toLowerCase().includes(term) ||
        (p.bio || '').toLowerCase().includes(term) ||
        (p.region || '').toLowerCase().includes(term)
      );
    }

    this.filteredProfiles = result;
    this.currentPage = 1;
    this.updatePaginatedProfiles();
  }

  onServerFilterChange(): void {
    this.currentPage = 1;
    this.loadProfiles();
  }

  onPageSizeChange(): void {
    this.currentPage = 1;
    this.updatePaginatedProfiles();
  }

  updatePaginatedProfiles(): void {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.paginatedProfiles = this.filteredProfiles.slice(startIndex, endIndex);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePaginatedProfiles();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePaginatedProfiles();
    }
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePaginatedProfiles();
    }
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredProfiles.length / this.pageSize));
  }

  get startItem(): number {
    if (this.filteredProfiles.length === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get endItem(): number {
    return Math.min(this.currentPage * this.pageSize, this.filteredProfiles.length);
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedRegion = '';
    this.selectedStatus = '';
    this.minRate = null;
    this.maxRate = null;
    this.currentPage = 1;
    this.loadProfiles();
  }

  buildInitials(firstName: string, lastName: string, fallback?: string): string {
    const f = (firstName || '').trim();
    const l = (lastName || '').trim();

    if (f && l) return `${f.charAt(0)}${l.charAt(0)}`.toUpperCase();
    if (f) return f.charAt(0).toUpperCase();
    return fallback?.charAt(0)?.toUpperCase() || 'F';
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