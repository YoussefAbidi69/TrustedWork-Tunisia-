import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Post, PostStatus, PostType, VoteType } from '../../../core/models/community.model';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';
import { CommunityFeedPostItem } from '../models/community-feed-item.model';

type PostCardItem = Post | CommunityFeedPostItem;

@Component({
  selector: 'app-post-card',
  templateUrl: './post-card.component.html',
  styleUrls: ['./post-card.component.css']
})
export class PostCardComponent {
  @Input() post!: PostCardItem;
  @Input() communityName = '';
  @Input() commentCount = 0;
  /** Community shell `ActivatedRoute` (parent of feed) for relative `posts/:id` links. */
  @Input({ required: true }) shell!: ActivatedRoute;

  @Output() vote = new EventEmitter<{ postId: number; type: VoteType }>();

  readonly VoteType = VoteType;
  readonly PostType = PostType;
  readonly PostStatus = PostStatus;

  downloadBusy = false;

  constructor(
    private authService: AuthService,
    private courseService: CourseService,
    private router: Router
  ) {}

  onVote(type: VoteType): void {
    this.vote.emit({ postId: this.post.id, type });
  }

  get showCourseDownload(): boolean {
    return this.isCoursePost(this.post) && this.post.status === PostStatus.PUBLISHED;
  }

  onDownloadCourse(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (!this.isCoursePost(this.post)) {
      return;
    }
    const uid = this.authService.getCurrentAuthUser()?.userId;
    if (uid == null) {
      void this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.downloadBusy = true;
    this.courseService.downloadCoursePostToDevice(this.post, uid).subscribe({
      next: () => {
        this.downloadBusy = false;
      },
      error: () => {
        this.downloadBusy = false;
      }
    });
  }

  get preview(): string {
    const content = this.post?.content ?? '';
    return content.length > 220 ? `${content.slice(0, 220)}…` : content;
  }

  get voteDifference(): number {
    const up = this.post.upvoteCount ?? 0;
    const down = this.post.downvoteCount ?? 0;
    return up - down;
  }

  private isCoursePost(post: PostCardItem): post is Post {
    return post.type === PostType.COURSE;
  }
}
