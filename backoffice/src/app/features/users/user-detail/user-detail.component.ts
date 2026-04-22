import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UserService, UserDTO } from '../../../core/services/user.service';
import { GamificationAdminService } from '../../engagement/services/gamification-admin.service';
import { BadgeDTO, GrowthProfileDTO } from '../../engagement/models/engagement.models';

@Component({
  selector: 'app-user-detail',
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.css'
})
export class UserDetailComponent implements OnInit {
  user: UserDTO | null = null;
  profile: GrowthProfileDTO | null = null;
  badges: BadgeDTO[] = [];
  engagementScore = 0;
  userId: number = 0;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private userService: UserService,
    private gamificationService: GamificationAdminService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.userId = +idParam;
      this.loadAllData();
    }
  }

  loadAllData(): void {
    this.loading = true;
    console.log('UserDetail: Loading data for userId:', this.userId);
    
    // 1. Fetch Basic User Profile
    this.userService.getUserById(this.userId).subscribe({
      next: user => {
        console.log('UserDetail: User profile loaded', user);
        this.user = user;
      },
      error: err => console.error('UserDetail: Error loading user profile', err)
    });

    // 2. Fetch Gamification Profile
    this.gamificationService.getUserProfile(this.userId).subscribe({
      next: profile => {
        console.log('UserDetail: Gamification profile loaded', profile);
        this.profile = profile;
      },
      error: err => console.error('UserDetail: Error loading gamification profile', err)
    });

    // 3. Fetch User Badges
    this.gamificationService.getUserBadges(this.userId).subscribe({
      next: badges => {
        console.log('UserDetail: Badges loaded', badges);
        this.badges = badges;
      },
      error: err => console.error('UserDetail: Error loading badges', err)
    });

    // 4. Fetch Engagement Score
    this.gamificationService.getUserScore(this.userId).subscribe({
      next: res => {
        console.log('UserDetail: Score loaded', res);
        this.engagementScore = res.engagementScore;
        this.loading = false;
      },
      error: err => {
        console.error('UserDetail: Error loading score', err);
        this.loading = false;
      }
    });
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return '';
    const s = status.toLowerCase();
    if (s === 'active') return 'status-active';
    if (s === 'suspended') return 'status-suspended';
    return 'status-other';
  }

  getProgressPercentage(): number {
    if (!this.profile) return 0;
    return Math.min(100, Math.round((this.profile.xpPoints / (this.profile.xpPoints + this.profile.xpToNextLevel)) * 100));
  }

  removeBadge(badgeId: number): void {
    if (confirm('Voulez-vous vraiment retirer ce badge à cet utilisateur ? Les XP associés seront déduits.')) {
      this.gamificationService.removeUserBadge(this.userId, badgeId).subscribe({
        next: () => {
          alert('Le badge a été retiré avec succès et les XP ont été déduits.');
          this.loadAllData();
        },
        error: err => {
          console.error('UserDetail: Error removing badge', err);
          alert('Erreur lors de la suppression du badge. Détails dans la console.');
        }
      });
    }
  }
}
