import { Component, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { FreelancerProfile, Skill } from '../../../core/models/freelancer.model';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';

@Component({
  selector: 'app-platform-stats',
  templateUrl: './platform-stats.component.html',
  styleUrls: ['./platform-stats.component.css']
})
export class PlatformStatsComponent implements OnInit {

  profiles: FreelancerProfile[] = [];
  loading = true;
  errorMsg = '';

  // Stats globales
  totalProfiles = 0;
  avgCompleteness = 0;
  availableCount = 0;
  busyCount = 0;
  vacationCount = 0;

  // Stats détaillées
  topSkills: { name: string; count: number }[] = [];
  regionStats: { region: string; count: number }[] = [];
  projectTypeStats: { type: string; count: number }[] = [];

  // Classement régional
  availableRegions: string[] = [];
  selectedRegion = '';
  regionRanking: FreelancerProfile[] = [];
  rankingLoading = false;
  rankingError = '';

  constructor(private profileService: FreelancerProfileService) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.errorMsg = '';

    this.profileService.getAllProfiles().subscribe({
      next: (profiles) => {
        this.profiles = profiles;
        this.totalProfiles = profiles.length;

        if (profiles.length === 0) {
          this.resetStats();
          this.loading = false;
          return;
        }

        const regions = profiles
          .map(p => p.region)
          .filter((r): r is string => !!r && r.trim() !== '');
        this.availableRegions = [...new Set(regions)].sort();

        const skillRequests = profiles.map(profile =>
          this.profileService.getSkillsByUserId(profile.userId).pipe(
            catchError((err) => {
              console.error(`Erreur skills user ${profile.userId}`, err);
              return of([] as Skill[]);
            })
          )
        );

        forkJoin(skillRequests).subscribe({
          next: (skillsPerProfile) => {
            this.computeStats(profiles, skillsPerProfile);
            this.loading = false;
          },
          error: (err) => {
            this.errorMsg = 'Erreur lors du chargement des statistiques des compétences';
            this.loading = false;
            console.error(err);
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

  onRegionChange(region: string): void {
    this.selectedRegion = region;

    if (!region) {
      this.regionRanking = [];
      this.rankingError = '';
      return;
    }

    this.loadRegionRanking(region);
  }

  loadRegionRanking(region: string): void {
    this.rankingLoading = true;
    this.rankingError = '';
    this.regionRanking = [];

    this.profileService.getRankingByRegion(region).subscribe({
      next: (profiles) => {
        this.regionRanking = profiles;
        this.rankingLoading = false;
      },
      error: (err) => {
        this.rankingError = 'Impossible de charger le classement pour cette région.';
        this.rankingLoading = false;
        console.error(err);
      }
    });
  }

  private computeStats(profiles: FreelancerProfile[], skillsPerProfile: Skill[][]): void {
    const totalScore = profiles.reduce((sum, p) => sum + (p.completenessScore || 0), 0);
    this.avgCompleteness = Math.round(totalScore / profiles.length);

    this.availableCount = profiles.filter(p => p.availabilityStatus === 'AVAILABLE').length;
    this.busyCount = profiles.filter(p => p.availabilityStatus === 'BUSY').length;
    this.vacationCount = profiles.filter(p => p.availabilityStatus === 'ON_VACATION').length;

    const skillMap = new Map<string, number>();
    skillsPerProfile.forEach((skills: Skill[]) => {
      skills.forEach((skill: Skill) => {
        const name = skill.name?.trim();
        if (!name) return;
        skillMap.set(name, (skillMap.get(name) || 0) + 1);
      });
    });

    this.topSkills = Array.from(skillMap.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10);

    const regionMap = new Map<string, number>();
    profiles.forEach(p => {
      const region = p.region?.trim() || 'Non définie';
      regionMap.set(region, (regionMap.get(region) || 0) + 1);
    });

    this.regionStats = Array.from(regionMap.entries())
      .map(([region, count]) => ({ region, count }))
      .sort((a, b) => b.count - a.count);

    const projectTypeMap = new Map<string, number>();
    profiles.forEach(p => {
      const type = p.projectType || 'Non défini';
      projectTypeMap.set(type, (projectTypeMap.get(type) || 0) + 1);
    });

    this.projectTypeStats = Array.from(projectTypeMap.entries())
      .map(([type, count]) => ({ type, count }))
      .sort((a, b) => b.count - a.count);
  }

  private resetStats(): void {
    this.totalProfiles = 0;
    this.avgCompleteness = 0;
    this.availableCount = 0;
    this.busyCount = 0;
    this.vacationCount = 0;
    this.topSkills = [];
    this.regionStats = [];
    this.projectTypeStats = [];
    this.availableRegions = [];
    this.regionRanking = [];
    this.selectedRegion = '';
    this.rankingError = '';
  }

  getPercent(count: number): number {
    if (this.totalProfiles === 0) return 0;
    return Math.round((count / this.totalProfiles) * 100);
  }

  getScoreClass(score: number): string {
    if (score >= 80) return 'text-success';
    if (score >= 50) return 'text-warning';
    return 'text-danger';
  }

  getAvailabilityClass(status: string): string {
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

  getAvailabilityLabel(status: string): string {
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

  getProjectTypeLabel(type: string): string {
    switch (type) {
      case 'SHORT_TERM':
        return 'Court terme';
      case 'LONG_TERM':
        return 'Long terme';
      case 'BOTH':
        return 'Les deux';
      default:
        return type || 'Non défini';
    }
  }
}