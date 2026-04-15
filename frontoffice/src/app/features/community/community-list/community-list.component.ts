import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { CommunityService } from '../../../core/services/community.service';
import { Community } from '../../../core/models/community.model';

@Component({
  selector: 'app-community-list',
  templateUrl: './community-list.component.html',
  styleUrls: ['./community-list.component.css']
})
export class CommunityListComponent implements OnInit {
  loading = false;
  error = '';
  communities: Community[] = [];
  listFilter: 'all' | 'mine' = 'all';

  constructor(
    public authService: AuthService,
    private communityService: CommunityService
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.error = '';
    this.communityService.getAll().subscribe({
      next: (list) => {
        this.communities = list;
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les communautés.';
        this.loading = false;
      }
    });
  }

  get filteredCommunities(): Community[] {
    const uid = this.authService.getCurrentAuthUser()?.userId;
    if (this.listFilter === 'mine' && uid != null) {
      return this.communities.filter((c) => c.createdBy === uid);
    }
    return this.communities;
  }

  truncate(text: string, max: number): string {
    if (!text) return '';
    return text.length <= max ? text : `${text.slice(0, max)}…`;
  }
}
