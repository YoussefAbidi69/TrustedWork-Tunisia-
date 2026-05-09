import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { forkJoin, catchError, of } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';

interface StatCard {
  label: string;
  value: string | number;
  icon:  string;
  color: string;
  sub?:  string;
}

interface TrendBar {
  label: string;
  value: number;
  max:   number;
  color: string;
}

@Component({
  selector: 'app-analytics',
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.css']
})
export class AnalyticsComponent implements OnInit, AfterViewInit {

  // ════════════════════════════════════════
  // VIEW CHILD - Canvas Radar
  // ════════════════════════════════════════
  @ViewChild('radarCanvas') radarCanvas!: ElementRef<HTMLCanvasElement>;

  loading = true;
  userId  = 0;

  // ── Stats principales ──
  totalViews        = 0;
  uniqueViewers     = 0;
  viewsLast7Days    = 0;
  totalEndorsements = 0;
  averageRating     = 0;
  totalReviews      = 0;
  skillCount        = 0;
  completeness      = 0;
  trustLevel        = 1;

  // ── Tendance vues ──
  viewTrend: 'EN_HAUSSE' | 'STABLE' | 'EN_BAISSE' = 'STABLE';

  // ── Top skills par endorsements ──
  topSkills: { name: string; endorsementCount: number; authenticityScore: number }[] = [];

  // ── Barre de profil DNA ──
  dnaAxes: TrendBar[] = [];

  constructor(
    private authService: AuthService,
    private profileService: FreelancerProfileService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    if (!user?.userId) { this.loading = false; return; }
    this.userId = user.userId;
    this.loadAnalytics();
  }

  // ════════════════════════════════════════
  // AFTER VIEW INIT - Dessine le Radar
  // ════════════════════════════════════════
  ngAfterViewInit(): void {
    setTimeout(() => {
      if (!this.loading && this.radarCanvas) {
        this.drawDNARadar();
      }
    }, 500);
  }

  /**
   * Calcule la moyenne des axes DNA pour l'affichage central du radar
   */
  calculateDNAAverage(): number {
    if (!this.dnaAxes || this.dnaAxes.length === 0) {
      return 0;
    }
    const total = this.dnaAxes.reduce((sum, axis) => sum + axis.value, 0);
    return Math.round(total / this.dnaAxes.length);
  }

  // ════════════════════════════════════════
  // DESSIN DU RADAR CHART STYLE FIFA UT
  // ════════════════════════════════════════
  private drawDNARadar(): void {
    const canvas = this.radarCanvas?.nativeElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    const maxRadius = Math.min(centerX, centerY) - 50;
    const axes = this.dnaAxes;
    const numAxes = axes.length;

    if (numAxes === 0) return;

    const angleStep = (Math.PI * 2) / numAxes;

    // Clear
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // ═══ NIVEAUX CONCENTRIQUES (Dégradé FIFA) ═══
    const levels = [
      { radius: maxRadius * 0.25, color: '#dc2626', alpha: 0.9 },
      { radius: maxRadius * 0.50, color: '#ea580c', alpha: 0.7 },
      { radius: maxRadius * 0.75, color: '#ca8a04', alpha: 0.5 },
      { radius: maxRadius * 1.00, color: '#16a34a', alpha: 0.3 },
    ];

    levels.forEach((level) => {
      ctx.beginPath();
      for (let i = 0; i <= numAxes; i++) {
        const angle = angleStep * i - Math.PI / 2;
        const x = centerX + Math.cos(angle) * level.radius;
        const y = centerY + Math.sin(angle) * level.radius;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.fillStyle = level.color;
      ctx.globalAlpha = level.alpha;
      ctx.fill();
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.15)';
      ctx.lineWidth = 1;
      ctx.stroke();
    });

    ctx.globalAlpha = 1;

    // ═══ LIGNES DES AXES ═══
    for (let i = 0; i < numAxes; i++) {
      const angle = angleStep * i - Math.PI / 2;
      const x = centerX + Math.cos(angle) * maxRadius;
      const y = centerY + Math.sin(angle) * maxRadius;
      ctx.beginPath();
      ctx.moveTo(centerX, centerY);
      ctx.lineTo(x, y);
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.2)';
      ctx.lineWidth = 1;
      ctx.stroke();
    }

    // ═══ POLYGONE DE DONNÉES ═══
    ctx.beginPath();
    const dataPoints: {x: number, y: number}[] = [];

    for (let i = 0; i < numAxes; i++) {
      const angle = angleStep * i - Math.PI / 2;
      const value = Math.min(100, Math.max(0, axes[i].value || 0)) / 100;
      const radius = value * maxRadius;
      const x = centerX + Math.cos(angle) * radius;
      const y = centerY + Math.sin(angle) * radius;
      dataPoints.push({x, y});
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }

    ctx.closePath();
    ctx.fillStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.fill();
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2.5;
    ctx.lineJoin = 'round';
    ctx.shadowColor = '#ffffff';
    ctx.shadowBlur = 15;
    ctx.stroke();
    ctx.shadowBlur = 0;

    // ═══ POINTS DES DONNÉES ═══
    dataPoints.forEach((point, i) => {
      ctx.beginPath();
      ctx.arc(point.x, point.y, 7, 0, Math.PI * 2);
      ctx.fillStyle = axes[i].color || '#ffffff';
      ctx.shadowColor = axes[i].color || '#ffffff';
      ctx.shadowBlur = 15;
      ctx.fill();
      ctx.shadowBlur = 0;

      ctx.beginPath();
      ctx.arc(point.x, point.y, 4, 0, Math.PI * 2);
      ctx.fillStyle = '#ffffff';
      ctx.fill();
    });
  }

  // ════════════════════════════════════════
  // CHARGEMENT DES DONNÉES
  // ════════════════════════════════════════
  private loadAnalytics(): void {
    this.loading = true;

    this.profileService.getProfileByUserId(this.userId)
      .pipe(catchError(() => of(null)))
      .subscribe((profile: any) => {

        if (!profile?.id) {
          this.loading = false;
          return;
        }

        const profileId = profile.id;

        forkJoin({
          analytics:     this.profileService.getProfileViewsAnalytics(profileId).pipe(catchError(() => of(null))),
          skills:        this.profileService.getMySkills(this.userId).pipe(catchError(() => of([]))),
          completeness:  this.profileService.getCompleteness(this.userId).pipe(catchError(() => of(null))),
          trustData:     this.profileService.getUserTrustLevel(this.userId).pipe(catchError(() => of({ trustLevel: 1 }))),
          reviewSummary: this.profileService.getReviewSummary(profileId).pipe(catchError(() => of(null)))
        }).subscribe({
          next: ({ analytics, skills, completeness, trustData, reviewSummary }) => {

            // ── Vues ──
            if (analytics) {
              this.totalViews     = analytics.totalViews     ?? profile.totalViews ?? 0;
              this.uniqueViewers  = analytics.uniqueViewers  ?? 0;
              this.viewsLast7Days = analytics.viewsLast7Days ?? 0;
            } else {
              this.totalViews = profile.totalViews ?? 0;
            }
            this.computeViewTrend();

            // ── Reviews ──
            if (reviewSummary) {
              this.averageRating = reviewSummary.averageRating ?? 0;
              this.totalReviews  = reviewSummary.totalReviews  ?? 0;
            }

            // ── Skills ──
            const allSkills = skills || [];
            this.skillCount        = allSkills.length;
            this.totalEndorsements = allSkills.reduce(
              (acc: number, s: any) => acc + (s.endorsementCount || 0), 0
            );
            this.topSkills = allSkills
              .sort((a: any, b: any) => (b.endorsementCount || 0) - (a.endorsementCount || 0))
              .slice(0, 5)
              .map((s: any) => ({
                name:              s.name,
                endorsementCount:  s.endorsementCount  || 0,
                authenticityScore: Math.round((s.authenticityScore || 0) * 100)
              }));

            // ── Completeness + TrustLevel ──
            this.completeness = completeness?.score ?? 0;
            this.trustLevel   = Number((trustData as any)?.trustLevel ?? 1);

            // ── Profil DNA ──
            this.computeProfileDNA(allSkills);

            this.loading = false;

            // Redessiner le Radar après chargement
            setTimeout(() => {
              if (this.radarCanvas) {
                this.drawDNARadar();
              }
            }, 100);

          },
          error: () => { this.loading = false; }
        });
      });
  }

  /**
   * Profil DNA — 4 axes calculés depuis les données réelles.
   *
   * Scoring basé sur le niveau de chaque compétence :
   *   JUNIOR=25 | INTERMEDIATE=50 | CONFIRMED=75 | EXPERT=100
   *
   * Technical : BACKEND, DEVOPS, CLOUD, SECURITY, DATA, AI, FULLSTACK
   * Creative  : FRONTEND, DESIGN, MOBILE, FULLSTACK
   * Reliable  : moyenne des authenticityScore
   * FastDel.  : moyenne d'endorsements × 20 (plafonné à 100)
   */
  private computeProfileDNA(skills: any[]): void {
    const zero = { max: 100 };
    if (skills.length === 0) {
      this.dnaAxes = [
        { label: 'Technical',     value: 0, ...zero, color: '#3b82f6' },
        { label: 'Creative',      value: 0, ...zero, color: '#8b5cf6' },
        { label: 'Reliable',      value: 0, ...zero, color: '#22c55e' },
        { label: 'Fast Delivery', value: 0, ...zero, color: '#f59e0b' }
      ];
      return;
    }

    // Level → numeric score
    const levelScore: Record<string, number> = {
      JUNIOR: 25, INTERMEDIATE: 50, CONFIRMED: 75, EXPERT: 100
    };
    const lvl = (s: any): number => levelScore[s.level] ?? 50;

    // ── Technical ──────────────────────────────────────────────────
    // FULLSTACK counts here too; DATA & AI are engineering disciplines
    const technicalCats = new Set(['BACKEND', 'DEVOPS', 'CLOUD', 'SECURITY', 'DATA', 'AI', 'FULLSTACK']);
    const technicalSkills = skills.filter(s => technicalCats.has(s.category));
    const technical = technicalSkills.length
      ? Math.round(technicalSkills.reduce((acc, s) => acc + lvl(s), 0) / technicalSkills.length)
      : 0;

    // ── Creative ───────────────────────────────────────────────────
    // FULLSTACK also counts (covers frontend knowledge)
    const creativeCats = new Set(['FRONTEND', 'DESIGN', 'MOBILE', 'FULLSTACK']);
    const creativeSkills = skills.filter(s => creativeCats.has(s.category));
    const creative = creativeSkills.length
      ? Math.round(creativeSkills.reduce((acc, s) => acc + lvl(s), 0) / creativeSkills.length)
      : 0;

    // ── Reliable (authenticityScore) ───────────────────────────────
    const avgAuth = skills.reduce((acc, s) => acc + (s.authenticityScore || 0), 0) / skills.length;
    // backend may send 0-1 or 0-100 — normalise safely
    const reliable = Math.min(100, Math.round(avgAuth > 1 ? avgAuth : avgAuth * 100));

    // ── Fast Delivery (endorsements) ───────────────────────────────
    const avgEnd = this.totalEndorsements / skills.length;
    const fastDelivery = Math.min(100, Math.round(avgEnd * 20));

    this.dnaAxes = [
      { label: 'Technical',     value: technical,    max: 100, color: '#3b82f6' },
      { label: 'Creative',      value: creative,     max: 100, color: '#8b5cf6' },
      { label: 'Reliable',      value: reliable,     max: 100, color: '#22c55e' },
      { label: 'Fast Delivery', value: fastDelivery, max: 100, color: '#f59e0b' }
    ];
  }

  private computeViewTrend(): void {
    if (this.viewsLast7Days === 0 && this.totalViews === 0) {
      this.viewTrend = 'STABLE';
      return;
    }
    const ratio = this.totalViews > 0 ? this.viewsLast7Days / this.totalViews : 0;
    if (ratio > 0.4)      this.viewTrend = 'EN_HAUSSE';
    else if (ratio < 0.1) this.viewTrend = 'EN_BAISSE';
    else                  this.viewTrend = 'STABLE';
  }

  get trendIcon(): string {
    return this.viewTrend === 'EN_HAUSSE' ? '📈'
         : this.viewTrend === 'EN_BAISSE' ? '📉' : '➡️';
  }

  get trendColor(): string {
    return this.viewTrend === 'EN_HAUSSE' ? '#22c55e'
         : this.viewTrend === 'EN_BAISSE' ? '#ef4444' : '#f59e0b';
  }

  get statCards(): StatCard[] {
    return [
      { label: 'Vues totales',      value: this.totalViews,       icon: 'fa-eye',        color: '#3b82f6', sub: `${this.viewsLast7Days} cette semaine` },
      { label: 'Visiteurs uniques', value: this.uniqueViewers,    icon: 'fa-users',      color: '#8b5cf6', sub: 'Visiteurs distincts' },
      { label: 'Endorsements',      value: this.totalEndorsements, icon: 'fa-thumbs-up', color: '#22c55e', sub: `Sur ${this.skillCount} compétences` },
      { label: 'Note moyenne',      value: this.averageRating ? this.averageRating.toFixed(1) + ' ★' : 'N/A', icon: 'fa-star', color: '#f59e0b', sub: `${this.totalReviews} avis` },
      { label: 'Trust Level',       value: this.trustLevel + '/5', icon: 'fa-shield-halved', color: '#06b6d4', sub: 'Score de confiance' },
      { label: 'Profil complet',    value: this.completeness + '%', icon: 'fa-circle-check', color: '#10b981', sub: 'Score de complétude' }
    ];
  }

  getBarWidth(value: number, max: number): string {
    return Math.round((value / max) * 100) + '%';
  }

  getAuthenticityColor(score: number): string {
    if (score >= 70) return '#22c55e';
    if (score >= 40) return '#f59e0b';
    return '#ef4444';
  }

  downloadPdf(): void {
  if (!this.userId) {
    console.error('UserId manquant pour export PDF');
    return;
  }

  this.profileService.exportMyCv(this.userId);
}
}