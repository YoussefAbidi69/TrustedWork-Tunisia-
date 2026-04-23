import { Component, OnInit } from '@angular/core';
import { LeaderboardDTO } from '../models/engagement.models';
import { LeaderboardService } from '../services/leaderboard.service';
import { GamificationAdminService } from '../services/gamification-admin.service';
import { UserService, UserDTO } from '../../../core/services/user.service';

@Component({
  selector: 'app-leaderboard-admin',
  templateUrl: './leaderboard-admin.component.html',
  styleUrls: ['./leaderboard-admin.component.css']
})
export class LeaderboardAdminComponent implements OnInit {

  entries: LeaderboardDTO[] = [];
  displayedEntries: LeaderboardDTO[] = [];
  governorates: string[] = [];
  selectedGov = '';
  userMap: Map<number, UserDTO> = new Map();
  loading = true;
  recomputing = false;
  modelStats: any = null;
  featureImportanceList: {key: string, value: number}[] = [];

  constructor(
    private lbService: LeaderboardService,
    private gamAdminService: GamificationAdminService,
    private userService: UserService
  ) { }

  ngOnInit(): void {
    this.refresh();
    this.loadModelStats();
  }

  loadModelStats(): void {
    this.gamAdminService.getModelStats().subscribe({
      next: (stats) => {
        this.modelStats = stats;
        if (stats && stats.feature_importance) {
          this.featureImportanceList = Object.keys(stats.feature_importance).map(key => ({
            key: key.replace(/_/g, ' '),
            value: stats.feature_importance[key]
          })).sort((a, b) => b.value - a.value); // Tri décroissant
        }
      },
      error: (err) => console.error('Failed to load ML stats', err)
    });
  }

  refresh(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        users.forEach(u => this.userMap.set(u.id, u));
        this.fetchGlobal();
      },
      error: () => this.fetchGlobal()
    });
  }

  fetchGlobal(): void {
    this.lbService.getGlobal().subscribe({
      next: (data) => {
        this.entries = data;
        // Extract unique governorates
        this.governorates = [...new Set(data.map(e => e.governorate).filter(Boolean))].sort();
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onGovChange(): void {
    this.applyFilter();
  }

  applyFilter(): void {
    if (!this.selectedGov) {
      this.displayedEntries = [...this.entries];
    } else {
      this.displayedEntries = this.entries.filter(e => e.governorate === this.selectedGov);
    }
  }

  getAverageScore(): number {
    if (this.displayedEntries.length === 0) return 0;
    const total = this.displayedEntries.reduce((sum, e) => sum + (e.engagementScore || 0), 0);
    return Math.round(total / this.displayedEntries.length);
  }

  recompute(): void {
    if (confirm('Calculate all ranks based on latest engagement scores?')) {
      this.recomputing = true;
      this.lbService.recompute().subscribe({
        next: () => {
          this.recomputing = false;
          this.refresh();
        },
        error: () => this.recomputing = false
      });
    }
  }

  getUserName(id: number): string {
    const u = this.userMap.get(id);
    return u ? `${u.firstName} ${u.lastName}` : `User #${id}`;
  }

  getParticipant(id: number): UserDTO | undefined {
    return this.userMap.get(id);
  }

  // --- Dynamic Styling for XAI Chart ---
  getFeatureColor(value: number): string {
    if (value > 20) return '#EF4444'; // Red for highly critical
    if (value > 10) return '#F59E0B'; // Orange for important
    if (value > 4) return '#3B82F6';  // Blue for medium
    return '#8B5CF6';                 // Purple for low
  }

  getFeatureGradient(value: number): string {
    if (value > 20) return 'linear-gradient(90deg, #EF4444, #F87171)';
    if (value > 10) return 'linear-gradient(90deg, #F59E0B, #FBBF24)';
    if (value > 4) return 'linear-gradient(90deg, #3B82F6, #60A5FA)';
    return 'linear-gradient(90deg, #8B5CF6, #A855F7)';
  }
}
