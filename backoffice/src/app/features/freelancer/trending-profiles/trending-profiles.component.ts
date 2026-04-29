import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { FreelancerProfile, Skill } from '../../../core/models/freelancer.model';

interface ProfileCard {
  profileId:        number;
  userId:           number;
  headline:         string;
  region:           string;
  availabilityStatus: string;
  totalViews:       number;
  completenessScore: number;
  riskScore:        number;
  suspended:        boolean;
  hourlyRate:       number;
  // Calculés
  totalEndorsements: number;
  averageRating:    number;
  skillCount:       number;
  initials:         string;
  trendScore:       number; // score composite pour le classement
}

type TabId = 'views' | 'rating' | 'endorsements' | 'risk';

@Component({
  selector:    'app-trending-profiles',
  templateUrl: './trending-profiles.component.html',
  styleUrls:   ['./trending-profiles.component.css']
})
export class TrendingProfilesComponent implements OnInit {

  loading = true;
  errorMsg = '';

  activeTab: TabId = 'views';

  // Données brutes
  allProfiles: ProfileCard[] = [];

  // Classements calculés
  topByViews:        ProfileCard[] = [];
  topByRating:       ProfileCard[] = [];
  topByEndorsements: ProfileCard[] = [];
  atRisk:            ProfileCard[] = [];

  // Stats globales
  totalViews        = 0;
  avgViews          = 0;
  maxViews          = 0;
  suspendedCount    = 0;
  highRiskCount     = 0;

  tabs: { id: TabId; label: string; icon: string; color: string }[] = [
    { id: 'views',        label: 'Plus visités',   icon: 'fa-eye',          color: '#3b82f6' },
    { id: 'rating',       label: 'Meilleures notes', icon: 'fa-star',       color: '#f59e0b' },
    { id: 'endorsements', label: 'Plus endorsés',  icon: 'fa-thumbs-up',    color: '#22c55e' },
    { id: 'risk',         label: 'Profils à risque', icon: 'fa-triangle-exclamation', color: '#ef4444' }
  ];

  constructor(
    private profileService: FreelancerProfileService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {

    this.loading  = true;
    this.errorMsg = '';

    this.profileService.getAllProfiles().pipe(
      catchError(() => of([] as FreelancerProfile[]))
    ).subscribe((profiles: FreelancerProfile[]) => {

      if (!profiles.length) {
        this.loading = false;
        return;
      }

      // Charger skills de chaque profil pour calculer endorsements
      const skillRequests = profiles.map(p =>
        this.profileService.getSkillsByUserId(p.userId).pipe(catchError(() => of([])))
      );

      // Charger rating moyen de chaque profil
      const ratingRequests = profiles.map(p =>
        this.profileService.getAverageRating(p.id).pipe(catchError(() => of(0)))
      );

      forkJoin({
        skillsPerProfile: forkJoin(skillRequests),
        ratingsPerProfile: forkJoin(ratingRequests)
      }).subscribe({
        next: ({ skillsPerProfile, ratingsPerProfile }) => {

          this.allProfiles = profiles.map((p, i) => {
            const skills: Skill[] = (skillsPerProfile[i] as Skill[]) || [];
            const totalEndorsements = skills.reduce(
              (acc, s: any) => acc + (s.endorsementCount || 0), 0
            );
            const averageRating = Number((ratingsPerProfile[i] as any) || 0);

            return {
              profileId:          p.id,
              userId:             p.userId,
              headline:           p.headline         || 'Freelancer',
              region:             p.region           || '—',
              availabilityStatus: p.availabilityStatus || 'AVAILABLE',
              totalViews:         p.totalViews        || 0,
              completenessScore:  p.completenessScore || 0,
              riskScore:          (p as any).riskScore || 0,
              suspended:          !!(p as any).suspended,
              hourlyRate:         p.hourlyRate        || 0,
              totalEndorsements,
              averageRating,
              skillCount:         skills.length,
              initials:           this.buildInitials(p.headline || ''),
              trendScore:         this.computeTrendScore(p, totalEndorsements, averageRating)
            };
          });

          this.computeRankings();
          this.computeGlobalStats();
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
    });
  }

  /**
   * Score composite de tendance (jury-friendly — formule explicable) :
   *   40% vues + 30% rating + 20% endorsements + 10% complétude
   */
  private computeTrendScore(
    p: FreelancerProfile,
    endorsements: number,
    rating: number
  ): number {
    const views       = Math.min((p.totalViews        || 0) / 100, 100) * 0.4;
    const ratingScore = (rating / 5)                                    * 30;
    const endoScore   = Math.min(endorsements / 10, 10)                 * 2;
    const complete    = ((p.completenessScore || 0) / 100)              * 10;
    return Math.round(views + ratingScore + endoScore + complete);
  }

  private computeRankings(): void {
    // Top 8 par vues
    this.topByViews = [...this.allProfiles]
      .sort((a, b) => b.totalViews - a.totalViews)
      .slice(0, 8);

    // Top 8 par note moyenne
    this.topByRating = [...this.allProfiles]
      .filter(p => p.averageRating > 0)
      .sort((a, b) => b.averageRating - a.averageRating)
      .slice(0, 8);

    // Top 8 par endorsements
    this.topByEndorsements = [...this.allProfiles]
      .sort((a, b) => b.totalEndorsements - a.totalEndorsements)
      .slice(0, 8);

    // Profils à risque — suspendus + risk score élevé
    this.atRisk = [...this.allProfiles]
      .filter(p => p.suspended || p.riskScore >= 20)
      .sort((a, b) => b.riskScore - a.riskScore)
      .slice(0, 8);
  }

  private computeGlobalStats(): void {
    const views      = this.allProfiles.map(p => p.totalViews);
    this.totalViews  = views.reduce((a, b) => a + b, 0);
    this.maxViews    = Math.max(...views, 0);
    this.avgViews    = this.allProfiles.length
      ? Math.round(this.totalViews / this.allProfiles.length)
      : 0;
    this.suspendedCount = this.allProfiles.filter(p => p.suspended).length;
    this.highRiskCount  = this.allProfiles.filter(p => p.riskScore >= 60).length;
  }

  private buildInitials(headline: string): string {
    const parts = (headline || '').trim().split(/\s+/).filter(Boolean);
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return 'FR';
  }

  // ── Getters liste active ──
  get activeList(): ProfileCard[] {
    switch (this.activeTab) {
      case 'views':        return this.topByViews;
      case 'rating':       return this.topByRating;
      case 'endorsements': return this.topByEndorsements;
      case 'risk':         return this.atRisk;
    }
  }

  get activeTab$(): { id: TabId; label: string; icon: string; color: string } {
    return this.tabs.find(t => t.id === this.activeTab)!;
  }

  // ── Navigation ──
  goToProfile(profileId: number): void {
    this.router.navigate(['/admin/freelancers', profileId]);
  }

  // ── Helpers UI ──
  getViewsBarWidth(views: number): string {
    if (!this.maxViews) return '0%';
    return Math.round((views / this.maxViews) * 100) + '%';
  }

  getRiskClass(score: number): string {
    if (score >= 80) return 'risk-critical';
    if (score >= 60) return 'risk-high';
    if (score >= 40) return 'risk-medium';
    return 'risk-low';
  }

  getRiskLabel(score: number): string {
    if (score >= 80) return 'Critique';
    if (score >= 60) return 'Élevé';
    if (score >= 40) return 'Modéré';
    return 'Faible';
  }

  getAvailabilityColor(status: string): string {
    switch (status) {
      case 'AVAILABLE':   return '#22c55e';
      case 'BUSY':        return '#f59e0b';
      case 'ON_VACATION': return '#ef4444';
      default:            return '#64748b';
    }
  }

  getAvailabilityLabel(status: string): string {
    switch (status) {
      case 'AVAILABLE':   return 'Disponible';
      case 'BUSY':        return 'Occupé';
      case 'ON_VACATION': return 'En vacances';
      default:            return status;
    }
  }

  getStars(rating: number): ('full' | 'empty')[] {
    return Array.from({ length: 5 }, (_, i) => i < Math.round(rating) ? 'full' : 'empty');
  }

  trackById(_: number, p: ProfileCard): number { return p.profileId; }
}