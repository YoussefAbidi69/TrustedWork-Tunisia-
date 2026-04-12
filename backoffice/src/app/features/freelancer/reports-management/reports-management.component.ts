import { Component, OnInit } from '@angular/core';
import { ProfileReport } from '../../../core/models/freelancer.model';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';

@Component({
  selector: 'app-reports-management',
  templateUrl: './reports-management.component.html',
  styleUrls: ['./reports-management.component.css']
})
export class ReportsManagementComponent implements OnInit {

  reports: ProfileReport[] = [];
  loading = true;
  errorMsg = '';
  successMsg = '';

  constructor(private profileService: FreelancerProfileService) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading = true;
    this.errorMsg = '';
    this.successMsg = '';

    this.profileService.getPendingReports().subscribe({
      next: (data) => {
        this.reports = data;
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors du chargement des signalements';
        this.loading = false;
        console.error(err);
      }
    });
  }

  resolve(reportId: number, status: 'RESOLVED' | 'REJECTED'): void {
    this.profileService.resolveReport(reportId, status).subscribe({
      next: () => {
        this.reports = this.reports.filter(r => r.id !== reportId);
        this.successMsg = `Signalement #${reportId} → ${status === 'RESOLVED' ? 'Résolu' : 'Rejeté'}`;
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (err) => {
        this.errorMsg = `Erreur lors du traitement du signalement #${reportId}`;
        console.error(err);
      }
    });
  }

  getProfileId(report: ProfileReport): number | null {
    return report.profile?.id ?? null;
  }
}