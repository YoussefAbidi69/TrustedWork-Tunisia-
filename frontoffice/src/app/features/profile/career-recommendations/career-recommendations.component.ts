import { Component, OnInit } from '@angular/core';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthService } from '../../../core/services/auth.service';
import { CareerPathResponse, SkillGapResponse } from '../../../core/models/freelancer.model';

/**
 * Composant Career Recommendations — parcours de carrière + skill gap
 * Algorithme Rule-based AI adapté au marché tunisien
 */
@Component({
  selector: 'app-career-recommendations',
  templateUrl: './career-recommendations.component.html',
  styleUrls: ['./career-recommendations.component.css']
})
export class CareerRecommendationsComponent implements OnInit {
  careerPath: CareerPathResponse | null = null;
  skillGap: SkillGapResponse | null = null;

  isLoadingCareer = false;
  isLoadingGap = false;
  errorMessage = '';

  constructor(
    private profileService: FreelancerProfileService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCareerPath();
    this.loadSkillGap();
  }

  get currentUserId(): number {
    return this.authService.getCurrentAuthUser()!.userId;
  }

  get isLoading(): boolean {
    return this.isLoadingCareer || this.isLoadingGap;
  }

  get detectedSkillsCount(): number {
    return this.careerPath?.currentSkills?.length || 0;
  }

  get missingSkillsCount(): number {
    return this.careerPath?.missingSkills?.length || 0;
  }

  get nextStepsCount(): number {
    return this.careerPath?.nextSteps?.length || 0;
  }

  loadCareerPath(): void {
    this.isLoadingCareer = true;
    this.profileService.getCareerPath(this.currentUserId).subscribe({
      next: (data) => {
        this.careerPath = data;
        this.isLoadingCareer = false;
      },
      error: () => {
        this.errorMessage = 'Erreur lors du chargement des recommandations.';
        this.isLoadingCareer = false;
      }
    });
  }

  loadSkillGap(): void {
    this.isLoadingGap = true;
    this.profileService.getSkillGaps(this.currentUserId).subscribe({
      next: (data) => {
        this.skillGap = data;
        this.isLoadingGap = false;
      },
      error: () => {
        this.isLoadingGap = false;
      }
    });
  }

  getPathIcon(path: string): string {
    const icons: { [key: string]: string } = {
      'Backend Java': '☕',
      'Frontend': '🎨',
      'Data': '📊',
      'DevOps': '⚙️',
      'Mobile': '📱'
    };
    return icons[path] || '🚀';
  }

  getGapColor(index: number): string {
    const colors = ['#ef4444', '#f97316', '#eab308', '#22c55e', '#3b82f6'];
    return colors[index % colors.length];
  }

  getGapBadgeClass(gapCount: number): string {
    if (gapCount > 5) return 'badge-danger';
    if (gapCount > 2) return 'badge-warning';
    return 'badge-success';
  }

  trackByIndex(index: number): number {
    return index;
  }
}