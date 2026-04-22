import { Component, OnInit } from '@angular/core';
import { LeaderboardDTO } from '../models/engagement.models';
import { LeaderboardService } from '../services/leaderboard.service';
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

  constructor(
    private lbService: LeaderboardService,
    private userService: UserService
  ) { }

  ngOnInit(): void {
    this.refresh();
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
}
