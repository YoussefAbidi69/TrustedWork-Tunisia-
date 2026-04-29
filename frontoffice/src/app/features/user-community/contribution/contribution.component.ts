import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { ContributionService } from '../../../core/services/contribution.service';
import { PostService } from '../../../core/services/post.service';
import { CourseService } from '../../../core/services/course.service';
import { Contribution, Post, PostStatus, PostType } from '../../../core/models/community.model';
import { messageFromApiHttpError } from '../../../core/utils/api-error-message.util';

@Component({
  selector: 'app-contribution',
  templateUrl: './contribution.component.html',
  styleUrls: ['./contribution.component.css']
})
export class ContributionComponent implements OnInit {
  loading = true;
  error = '';
  contribution: Contribution = { id: 0, userId: 0, sharedCourseCount: 0 };
  coursePosts: Post[] = [];
  userId: number | null = null;
  shareBusy = false;
  downloadError = '';

  constructor(
    public route: ActivatedRoute,
    private authService: AuthService,
    private contributionService: ContributionService,
    private postService: PostService,
    private courseService: CourseService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    const uid = user?.userId;
    if (uid == null) {
      this.loading = false;
      return;
    }
    this.userId = uid;
    forkJoin({
      contrib: this.contributionService.getByUserId(uid).pipe(
        catchError(() =>
          of({ id: 0, userId: uid, sharedCourseCount: 0 } as Contribution)
        )
      ),
      posts: this.postService
        .getAll({ type: PostType.COURSE, status: PostStatus.PUBLISHED })
        .pipe(catchError(() => of([] as Post[])))
    }).subscribe({
      next: ({ contrib, posts }) => {
        this.contribution = contrib;
        this.coursePosts = posts;
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les contributions.';
        this.loading = false;
      }
    });
  }

  recordShare(): void {
    const uid = this.userId;
    if (uid == null || this.shareBusy) return;
    this.shareBusy = true;
    this.contributionService.recordShare(uid).subscribe({
      next: (updated) => {
        this.contribution = updated;
        this.shareBusy = false;
      },
      error: () => {
        this.shareBusy = false;
      }
    });
  }

  downloadPost(post: Post): void {
    const uid = this.userId;
    if (uid == null) return;
    this.downloadError = '';
    this.courseService.downloadCoursePostToDevice(post, uid).subscribe({
      next: () => {},
      error: (err: HttpErrorResponse) => {
        void (async () => {
          const serverMsg = await messageFromApiHttpError(err);
          this.downloadError =
            err.status === 403
              ? serverMsg ??
                'Download not allowed. Record a course share first if this is not your post.'
              : serverMsg ?? 'Download failed. Check ms-community and the file URL.';
        })();
      }
    });
  }
}
