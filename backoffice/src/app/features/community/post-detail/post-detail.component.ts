import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, PostDTO, CommentDTO, ReportDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-post-detail',
  templateUrl: './post-detail.component.html',
  styleUrls: ['./post-detail.component.css']
})
export class PostDetailComponent implements OnInit {
  post: PostDTO | null = null;
  comments: CommentDTO[] = [];
  reports: ReportDTO[] = [];
  loading = true;
  userMap: { [id: number]: string } = {};
  activeTab = 'comments';

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
    this.service.getPost(id).subscribe({
      next: (data) => {
        this.post = data;
        this.loadRelations(id);
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  loadRelations(id: number): void {
    this.service.getCommentsByPost(id).subscribe(data => this.comments = data || []);
    this.service.getReportsByPost(id).subscribe(data => this.reports = data || []);
    this.loading = false;
  }

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  deletePost(): void {
    if (!this.post || !confirm(`Delete post "${this.post.title}"?`)) return;
    this.service.deletePost(this.post.id, this.post.authorId || 0).subscribe({
      next: () => this.router.navigate(['/admin/community/posts']),
      error: (e: any) => console.error('Delete failed', e)
    });
  }
}
