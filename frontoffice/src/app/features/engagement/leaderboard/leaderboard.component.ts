import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { LeaderboardService } from '../services/leaderboard.service';
import { LeaderboardDTO } from '../models/engagement.models';
import { AuthService } from '../../../core/services/auth.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

const USER_API = 'http://localhost:8081/api';

@Component({
  selector: 'app-leaderboard',
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.css']
})
export class LeaderboardComponent implements OnInit {
  entries: LeaderboardDTO[] = [];
  topThree: LeaderboardDTO[] = [];
  remainingEntries: LeaderboardDTO[] = [];
  loading = true;
  selectedGov = '';
  currentUserId: number | null = null;

  // Cache: userId → { firstName, lastName }
  private userCache: Map<number, { firstName: string; lastName: string }> = new Map();

  governorates = [
    'Tunis','Ariana','Ben Arous','Manouba','Nabeul','Zaghouan','Bizerte',
    'Béja','Jendouba','Kef','Siliana','Sousse','Monastir','Mahdia','Sfax',
    'Kairouan','Kasserine','Sidi Bouzid','Gabès','Medenine','Tataouine',
    'Gafsa','Tozeur','Kébili'
  ];

  constructor(
    private lbService: LeaderboardService,
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    this.currentUserId = user?.userId ?? null;
    this.loadGlobal();
  }

  loadGlobal(): void {
    this.loading = true;
    this.lbService.getGlobal().subscribe({
      next: (data) => this.enrichAndProcess(data),
      error: (err) => {
        console.error('Failed to load leaderboard:', err);
        this.loading = false;
      }
    });
  }

  onGovChange(): void {
    this.loading = true;
    if (!this.selectedGov) {
      this.loadGlobal();
    } else {
      this.lbService.getByGovernorate(this.selectedGov).subscribe({
        next: (data) => this.enrichAndProcess(data),
        error: () => { this.loading = false; }
      });
    }
  }

  /**
   * Fetch real names from user-service for each userId,
   * then sort by engagementScore DESC and assign correct ranks.
   */
  enrichAndProcess(data: LeaderboardDTO[]): void {
    if (!data || data.length === 0) {
      this.processEntries([]);
      this.loading = false;
      return;
    }

    // Fetch public profiles from the identity endpoint (which is open)
    const requests = data.map(entry =>
      this.http.get<any>(`${USER_API}/identity/users/${entry.userId}`).pipe(
        catchError((err) => {
          console.error(`Failed to fetch public profile for user ${entry.userId}:`, err);
          return of(null);
        })
      )
    );

    forkJoin(requests).subscribe({
      next: (users) => {
        users.forEach((u, i) => {
          if (u) {
            const firstName = u.firstName || u.firstname || u.prenom || '';
            const lastName  = u.lastName  || u.lastname  || u.nom   || '';
            this.userCache.set(data[i].userId, { firstName, lastName });
            data[i].firstName = firstName;
            data[i].lastName  = lastName;
          }
        });
        this.processEntries(data);
        this.loading = false;
      },
      error: () => {
        this.processEntries(data);
        this.loading = false;
      }
    });
  }

  processEntries(data: LeaderboardDTO[]): void {
    // Sort by engagementScore DESC (frontend safety net)
    const sorted = [...data].sort((a, b) => b.engagementScore - a.engagementScore);
    // Re-assign ranks 1-based in correct order
    sorted.forEach((e, i) => e.rank = i + 1);

    this.entries = sorted;

    const top = sorted.slice(0, 3);
    // Podium visual layout: [2nd place, 1st place, 3rd place]
    if (top.length === 3) {
      this.topThree = [top[1], top[0], top[2]];
    } else {
      this.topThree = [...top];
    }
    this.remainingEntries = sorted.slice(3);
  }

  getUserName(entry: LeaderboardDTO): string {
    const cached = this.userCache.get(entry.userId);
    if (cached && (cached.firstName || cached.lastName)) {
      return `${cached.firstName} ${cached.lastName}`.trim();
    }
    
    // Fallback if data is already in entry and not a placeholder
    if (entry.firstName && entry.lastName && !entry.firstName.includes('Champion')) {
      return `${entry.firstName} ${entry.lastName}`.trim();
    }

    return `Membre #${entry.userId}`;
  }

  isCurrentUser(entry: LeaderboardDTO): boolean {
    return entry.userId === this.currentUserId;
  }

  getRankSuffix(rank: number): string {
    if (rank === 1) return 'st';
    if (rank === 2) return 'nd';
    if (rank === 3) return 'rd';
    return 'th';
  }
}
