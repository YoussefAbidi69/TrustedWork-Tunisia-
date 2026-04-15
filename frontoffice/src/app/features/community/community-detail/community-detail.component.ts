import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { CommunityService } from '../../../core/services/community.service';
import { PostService } from '../../../core/services/post.service';
import { CourseService } from '../../../core/services/course.service';
import { Community, Course, Post, PostStatus, PostType, VoteType } from '../../../core/models/community.model';
import { messageFromApiHttpError } from '../../../core/utils/api-error-message.util';

@Component({
  selector: 'app-community-detail',
  templateUrl: './community-detail.component.html',
  styleUrls: ['./community-detail.component.css']
})
export class CommunityDetailComponent implements OnInit {
  community: Community | null = null;
  posts: Post[] = [];
  courses: Course[] = [];
  loading = true;
  error = '';
  reportOpen: Record<number, boolean> = {};
  reportReason: Record<number, string> = {};
  reportDescription: Record<number, string> = {};
  reportSubmitted: Record<number, boolean> = {};
  courseDownloadError = '';

  readonly PostType = PostType;
  readonly PostStatus = PostStatus;
  readonly VoteType = VoteType;

  private communityId: number | null = null;

  constructor(
    public authService: AuthService,
    public route: ActivatedRoute,
    private router: Router,
    private communityService: CommunityService,
    private postService: PostService,
    private courseService: CourseService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (Number.isNaN(id)) {
      this.error = 'Communauté introuvable.';
      this.loading = false;
      return;
    }
    this.communityId = id;
    const voterId = this.authService.getCurrentAuthUser()?.userId;
    forkJoin({
      community: this.communityService.getById(id).pipe(
        catchError(() => of(null as Community | null))
      ),
      posts: this.postService
        .getAll({
          communityId: id,
          ...(voterId != null ? { voterId } : {})
        })
        .pipe(catchError(() => of([] as Post[]))),
      courses: this.courseService.listByCommunity(id).pipe(catchError(() => of([] as Course[])))
    }).subscribe({
      next: ({ community, posts, courses }) => {
        this.community = community;
        this.posts = posts;
        this.courses = courses;
        if (!community) this.error = 'Communauté introuvable.';
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger la communauté.';
        this.loading = false;
      }
    });
  }

  toggleReport(postId: number): void {
    this.reportOpen[postId] = !this.reportOpen[postId];
  }

  submitReport(postId: number): void {
    const reason = (this.reportReason[postId] || '').trim();
    const description = (this.reportDescription[postId] || '').trim();
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (!reason || !description || userId == null) return;
    this.postService.report(postId, reason, description, userId).subscribe({
      next: () => {
        this.reportSubmitted[postId] = true;
        this.reportOpen[postId] = false;
      }
    });
  }

  cancelReport(postId: number): void {
    this.reportOpen[postId] = false;
  }

  vote(postId: number, type: VoteType): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) {
      return;
    }
    this.postService.vote(postId, type, userId).subscribe({
      next: () => {
        this.postService.getById(postId, userId).subscribe({
          next: (updated) => {
            const idx = this.posts.findIndex((p) => p.id === postId);
            if (idx >= 0) {
              this.posts = [...this.posts.slice(0, idx), updated, ...this.posts.slice(idx + 1)];
            }
          }
        });
      },
      error: (err: HttpErrorResponse) => console.error(err)
    });
  }

  canDownload(post: Post): boolean {
    return post.type === PostType.COURSE && post.status === PostStatus.PUBLISHED;
  }

  downloadCoursePost(post: Post): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) {
      void this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.courseDownloadError = '';
    this.courseService.downloadCoursePostToDevice(post, userId).subscribe({
      next: () => {},
      error: (err: HttpErrorResponse) => {
        void (async () => {
          const serverMsg = await messageFromApiHttpError(err);
          this.courseDownloadError =
            err.status === 403
              ? serverMsg ??
                'Download not allowed. Share a course once from Contributions (unless this is your post).'
              : serverMsg ?? 'Download failed. Check ms-community and the post file URL.';
        })();
      }
    });
  }

  truncate(text: string, max: number): string {
    if (!text) return '';
    return text.length <= max ? text : `${text.slice(0, max)}…`;
  }

  createPostLink(): string[] {
    return this.communityId != null ? ['../posts', 'new'] : ['../browse'];
  }

  createPostQueryParams(): Record<string, string> | null {
    return this.communityId != null ? { communityId: String(this.communityId) } : null;
  }
}
