import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { WebsocketService } from '../../../core/services/websocket.service';
import { UserService, UserDTO } from '../../../core/services/user.service';

@Component({
  selector: 'app-reports-management',
  templateUrl: './reports-management.component.html',
  styleUrls: ['./reports-management.component.css']
})
export class ReportsManagementComponent implements OnInit, OnDestroy {

  reports: any[] = [];
  filteredReports: any[] = [];
  pagedReports: any[] = [];

  loading = false;

  selectedStatus: 'ALL' | 'PENDING' | 'IN_REVIEW' | 'RESOLVED' | 'REJECTED' = 'ALL';
  selectedCategory: 'ALL' | 'FAKE_SKILLS' | 'SPAM' | 'IDENTITY_THEFT' | 'INAPPROPRIATE_CONTENT' | 'OTHER' = 'ALL';
  searchQuery = '';

  currentPage = 1;
  pageSize = 10;

  notificationMessage = '';
  showNotification = false;

  private userNameCache: Map<number, string> = new Map();
  private usersLoaded = false;

  constructor(
    private freelancerProfileService: FreelancerProfileService,
    private websocketService: WebsocketService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadAllUsersIntoCache();
    this.loadReports();

    this.websocketService.connect(() => {
      this.websocketService.subscribeToReports((message: any) => {
        switch (message?.type) {
          case 'NEW_REPORT':
            this.showRealtimeNotification('Nouveau signalement reçu');
            break;
          case 'REPORT_STATUS_UPDATED':
            this.showRealtimeNotification('Statut mis à jour');
            break;
          case 'PROFILE_SUSPENDED':
            this.showRealtimeNotification('Profil suspendu automatiquement');
            break;
          default:
            this.showRealtimeNotification('Mise à jour en temps réel');
            break;
        }
        this.loadReports();
      });
    });
  }

  ngOnDestroy(): void {
    this.websocketService.disconnect();
  }

  // ───────────────────── USERS CACHE ─────────────────────

  private loadAllUsersIntoCache(): void {
    if (this.usersLoaded) return;

    this.userService.getAllUsers().pipe(
      catchError(() => of([] as UserDTO[]))
    ).subscribe((users: UserDTO[]) => {
      users.forEach(user => {
        const name =
          `${user.firstName || ''} ${user.lastName || ''}`.trim() ||
          user.email ||
          `Utilisateur ${user.id}`;

        this.userNameCache.set(user.id, name);
      });

      this.usersLoaded = true;
      this.applyFilters();
    });
  }

  getReporterName(reporterId: number): string {
    return this.userNameCache.get(reporterId) || `Utilisateur ${reporterId}`;
  }

  // ───────────────────── LOAD REPORTS ─────────────────────

  loadReports(): void {
    this.loading = true;

    this.freelancerProfileService.getAllReports().subscribe({
      next: (data: any[]) => {
        this.reports = this.sortReports(data || []);
        this.currentPage = 1;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading reports:', err);
        this.reports = [];
        this.filteredReports = [];
        this.pagedReports = [];
        this.loading = false;
      }
    });
  }

  refreshReports(): void {
    this.loadReports();
  }

  // ───────────────────── FALLBACK HELPERS ─────────────────────
  // Compatibles ancien format + nouveau DTO enrichi

  getFreelancerName(report: any): string {
    return (
      report?.freelancerName ||
      report?.profile?.headline ||
      report?.profile?.fullName ||
      report?.profile?.name ||
      'Profil freelancer'
    );
  }

  getFreelancerUserId(report: any): number | string {
    return (
      report?.freelancerUserId ??
      report?.profile?.userId ??
      0
    );
  }

  getFreelancerRegion(report: any): string {
    return report?.profile?.region || 'N/A';
  }

  getReporterDisplayName(report: any): string {
    return (
      report?.reporterName ||
      this.getReporterName(report?.reporterId) ||
      'Unknown user'
    );
  }

  getRiskScore(report: any): number {
    const raw =
      report?.riskScore ??
      report?.profile?.riskScore ??
      report?.profile?.completenessScore ??
      0;

    const score = Number(raw);
    return Number.isFinite(score) ? score : 0;
  }

  isSuspended(report: any): boolean {
    return !!(
      report?.suspended ??
      report?.profile?.suspended ??
      false
    );
  }

  getProfileId(report: any): number | null {
    return report?.profileId ?? report?.profile?.id ?? null;
  }

  // ───────────────────── SEARCH / FILTERS ─────────────────────

  onSearchChange(): void {
    this.currentPage = 1;
    this.applyFilters();
  }

  filterByStatus(status: 'ALL' | 'PENDING' | 'IN_REVIEW' | 'RESOLVED' | 'REJECTED'): void {
    this.selectedStatus = status;
    this.currentPage = 1;
    this.applyFilters();
  }

  filterByCategory(category: 'ALL' | 'FAKE_SKILLS' | 'SPAM' | 'IDENTITY_THEFT' | 'INAPPROPRIATE_CONTENT' | 'OTHER'): void {
    this.selectedCategory = category;
    this.currentPage = 1;
    this.applyFilters();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.selectedStatus = 'ALL';
    this.selectedCategory = 'ALL';
    this.currentPage = 1;
    this.applyFilters();
  }

  get hasActiveFilters(): boolean {
    return this.searchQuery.trim() !== ''
      || this.selectedStatus !== 'ALL'
      || this.selectedCategory !== 'ALL';
  }

  applyFilters(): void {
    let result = [...this.reports];

    if (this.selectedStatus !== 'ALL') {
      result = result.filter(r => r.status === this.selectedStatus);
    }

    if (this.selectedCategory !== 'ALL') {
      result = result.filter(r => r.category === this.selectedCategory);
    }

    const q = this.searchQuery.trim().toLowerCase();
    if (q) {
      result = result.filter(r => {
        const description = (r?.description || '').toLowerCase();
        const freelancerName = this.getFreelancerName(r).toLowerCase();
        const freelancerRegion = this.getFreelancerRegion(r).toLowerCase();
        const reporterName = this.getReporterDisplayName(r).toLowerCase();

        return (
          description.includes(q) ||
          freelancerName.includes(q) ||
          freelancerRegion.includes(q) ||
          reporterName.includes(q)
        );
      });
    }

    this.filteredReports = this.sortReports(result);
    this.updatePage();
  }

  private sortReports(reports: any[]): any[] {
    return [...reports].sort((a, b) => {
      const riskA = this.getRiskScore(a);
      const riskB = this.getRiskScore(b);

      if (riskB !== riskA) {
        return riskB - riskA;
      }

      const dateA = new Date(a?.createdAt || 0).getTime();
      const dateB = new Date(b?.createdAt || 0).getTime();

      return dateB - dateA;
    });
  }

  // ───────────────────── PAGINATION ─────────────────────

  get totalPages(): number {
    return Math.ceil(this.filteredReports.length / this.pageSize) || 1;
  }

  get pages(): number[] {
    const total = this.totalPages;
    const current = this.currentPage;
    const delta = 2;
    const range: number[] = [];

    for (let i = Math.max(1, current - delta); i <= Math.min(total, current + delta); i++) {
      range.push(i);
    }

    return range;
  }

  updatePage(): void {
    const start = (this.currentPage - 1) * this.pageSize;
    this.pagedReports = this.filteredReports.slice(start, start + this.pageSize);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePage();
  }

  get startIndex(): number {
    return this.filteredReports.length === 0 ? 0 : (this.currentPage - 1) * this.pageSize + 1;
  }

  get endIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.filteredReports.length);
  }

  // ───────────────────── STATS ─────────────────────

  get pendingCount(): number {
    return this.reports.filter(r => r.status === 'PENDING').length;
  }

  get inReviewCount(): number {
    return this.reports.filter(r => r.status === 'IN_REVIEW').length;
  }

  get resolvedCount(): number {
    return this.reports.filter(r => r.status === 'RESOLVED').length;
  }

  // ───────────────────── ACTIONS ─────────────────────

  viewProfile(report: any): void {
    const profileId = this.getProfileId(report);

    if (!profileId) {
      this.showRealtimeNotification('Impossible d’ouvrir le profil.');
      return;
    }

    this.router.navigate(['/admin/freelancers', profileId]);
  }

  markInReview(report: any): void {
    this.updateStatus(report, 'IN_REVIEW');
  }

  resolveReport(report: any): void {
    this.updateStatus(report, 'RESOLVED');
  }

  rejectReport(report: any): void {
    this.updateStatus(report, 'REJECTED');
  }

  updateStatus(report: any, status: 'IN_REVIEW' | 'RESOLVED' | 'REJECTED'): void {
    if (!report?.id) return;

    this.freelancerProfileService.updateReportStatus(report.id, status).subscribe({
      next: (updated: any) => {
        const index = this.reports.findIndex(r => r.id === updated.id);
        if (index !== -1) {
          this.reports[index] = updated;
          this.reports = this.sortReports(this.reports);
        }
        this.applyFilters();
        this.showRealtimeNotification(`Statut mis à jour → ${this.getStatusLabel(status)}`);
      },
      error: (err) => {
        console.error('Update failed:', err);
      }
    });
  }

  // ───────────────────── UI HELPERS ─────────────────────

  getReporterInitials(report: any): string {
    const name = this.getReporterDisplayName(report);
    return this.getInitialsFromName(name, 'RU');
  }

  getFreelancerInitials(report: any): string {
    const name = this.getFreelancerName(report);
    return this.getInitialsFromName(name, 'FR');
  }

  private getInitialsFromName(name: string, fallback: string): string {
    if (!name || !name.trim()) return fallback;

    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }

    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'badge badge-warning';
      case 'IN_REVIEW':
        return 'badge badge-info';
      case 'RESOLVED':
        return 'badge badge-success';
      case 'REJECTED':
        return 'badge badge-danger';
      default:
        return 'badge badge-muted';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'En attente';
      case 'IN_REVIEW':
        return 'En cours';
      case 'RESOLVED':
        return 'Résolu';
      case 'REJECTED':
        return 'Rejeté';
      default:
        return status;
    }
  }

  getCategoryClass(category: string): string {
    switch (category) {
      case 'FAKE_SKILLS':
        return 'reason-badge reason-badge--danger';
      case 'SPAM':
        return 'reason-badge reason-badge--warning';
      case 'IDENTITY_THEFT':
        return 'reason-badge reason-badge--danger';
      case 'INAPPROPRIATE_CONTENT':
        return 'reason-badge reason-badge--warning';
      default:
        return 'reason-badge reason-badge--muted';
    }
  }

  getCategoryLabel(category: string): string {
    switch (category) {
      case 'FAKE_SKILLS':
        return 'Compétences fausses';
      case 'SPAM':
        return 'Spam';
      case 'IDENTITY_THEFT':
        return 'Usurpation d’identité';
      case 'INAPPROPRIATE_CONTENT':
        return 'Contenu inapproprié';
      default:
        return 'Autre';
    }
  }

  getRiskClass(report: any): string {
    const score = this.getRiskScore(report);

    if (score >= 80) return 'badge badge-danger';
    if (score >= 60) return 'badge badge-warning';
    if (score >= 40) return 'badge badge-info';
    return 'badge badge-success';
  }

  getSuspensionClass(report: any): string {
    return this.isSuspended(report)
      ? 'badge badge-danger'
      : 'badge badge-success';
  }

  getSuspensionLabel(report: any): string {
    return this.isSuspended(report) ? 'Suspendu' : 'Actif';
  }

  trackByReportId(index: number, report: any): number {
    return report.id;
  }

  private showRealtimeNotification(message: string): void {
    this.notificationMessage = message;
    this.showNotification = true;

    setTimeout(() => {
      this.showNotification = false;
      this.notificationMessage = '';
    }, 3000);
  }
}