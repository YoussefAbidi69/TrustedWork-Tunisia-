import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, CommunityDTO, PostDTO, CourseDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-community-detail',
  templateUrl: './community-detail.component.html',
  styleUrls: ['./community-detail.component.css']
})
export class CommunityDetailComponent implements OnInit {
  community: CommunityDTO | null = null;
  posts: PostDTO[] = [];
  courses: CourseDTO[] = [];
  loading = true;
  userMap: { [id: number]: string } = {};
  activeTab = 'posts';

  constructor(private route: ActivatedRoute, private router: Router, private service: CommunityAdminService, private userService: UserService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.userService.getUserNameMap().subscribe(map => this.userMap = map);
    if (id) {
      this.loadData(Number(id));
    }
  }

  loadData(id: number): void {
    this.loading = true;
    this.service.getCommunity(id).subscribe({
      next: (data) => {
        this.community = data;
        this.loadRelations(id);
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  loadRelations(id: number): void {
    this.service.getPostsByCommunity(id).subscribe(data => this.posts = data || []);
    this.service.getCoursesByCommunity(id).subscribe(data => this.courses = data || []);
    this.loading = false;
  }

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  deleteCommunity(): void {
    if (!this.community || !confirm(`Delete community "${this.community.name}"? This cannot be undone.`)) return;
    this.service.deleteCommunity(this.community.id).subscribe({
      next: () => this.router.navigate(['/admin/community/communities']),
      error: (e: any) => console.error('Delete failed', e)
    });
  }
}
