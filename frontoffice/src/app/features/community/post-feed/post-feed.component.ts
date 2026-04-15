import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap, Router, convertToParamMap } from '@angular/router';
import { combineLatest, forkJoin, of, Subject, Observable } from 'rxjs';
import { catchError, map, takeUntil } from 'rxjs/operators';

import { Community, Post, VoteType } from '../../../core/models/community.model';
import { CommunityService } from '../../../core/services/community.service';
import { PostService } from '../../../core/services/post.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-post-feed',
  templateUrl: './post-feed.component.html',
  styleUrls: ['./post-feed.component.css']
})
export class PostFeedComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';

  posts: Post[] = [];
  communities: Community[] = [];
  commentCountByPost: Record<number, number> = {};
  activeCommunityId: number | null = null;

  myPosts$: Observable<Post[]>;

  private readonly destroy$ = new Subject<void>();

  constructor(
    public readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly postService: PostService,
    private readonly communityService: CommunityService,
    private readonly authService: AuthService
  ) {
    this.myPosts$ = of([]);
  }

  get shell(): ActivatedRoute {
    return this.route.parent!;
  }

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentAuthUser();
    if (currentUser) {
      this.myPosts$ = this.postService.getAll({ voterId: currentUser.userId }).pipe(
        map((posts) => posts.filter((post) => post.createdBy === currentUser.userId)),
        catchError(() => of([] as Post[]))
      );
    }

    const parentParamMap$ = this.route.parent ? this.route.parent.paramMap : of(convertToParamMap({}));
    const grandParentParamMap$ = this.route.parent?.parent
      ? this.route.parent.parent.paramMap
      : of(convertToParamMap({}));

    combineLatest([this.route.paramMap, parentParamMap$, grandParentParamMap$])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([paramMap, parentParamMap, grandParentParamMap]) => {
        const rawCommunityId = this.resolveCommunityIdParam([
          paramMap,
          parentParamMap,
          grandParentParamMap,
          ...this.route.snapshot.pathFromRoot.map((snapshot) => snapshot.paramMap)
        ]);
        const parsed = rawCommunityId != null ? Number(rawCommunityId) : NaN;

        this.activeCommunityId = Number.isFinite(parsed) ? parsed : null;
        this.loadFeed();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSelectCommunity(communityId: number | null): void {
    const parent = this.route.parent;
    if (!parent) return;
    if (communityId == null) {
      this.router.navigate([''], { relativeTo: parent });
      return;
    }
    this.router.navigate(['communities', communityId], { relativeTo: parent });
  }

  onVote(payload: { postId: number; type: VoteType }): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) {
      return;
    }
    this.postService.vote(payload.postId, payload.type, userId).subscribe({
      next: () => {
        this.postService.getById(payload.postId, userId).subscribe({
          next: (updated) => {
            const idx = this.posts.findIndex((p) => p.id === updated.id);
            if (idx >= 0) {
              this.posts = [...this.posts.slice(0, idx), updated, ...this.posts.slice(idx + 1)];
            }
          }
        });
      },
      error: () => {}
    });
  }

  goCreateCommunity(): void {
    const parent = this.route.parent;
    if (!parent) return;
    this.router.navigate(['create'], { relativeTo: parent });
  }

  goCreatePost(): void {
    const parent = this.route.parent;
    if (!parent) return;
    const cid = this.activeCommunityId;
    if (cid != null) {
      this.router.navigate(['posts', 'new'], {
        relativeTo: parent,
        queryParams: { communityId: cid }
      });
    } else {
      this.router.navigate(['posts', 'new'], { relativeTo: parent });
    }
  }

  communityNameById(communityId: number): string {
    const community = this.communities.find((item) => item.id === communityId);
    return community?.name ?? `Community #${communityId}`;
  }

  getCommunityName(): string {
    if (this.activeCommunityId == null) {
      return 'All posts';
    }
    return this.communityNameById(this.activeCommunityId);
  }

  trackByPostId(_index: number, post: Post): number {
    return post.id;
  }

  private loadFeed(): void {
    this.loading = true;
    this.error = '';

    const voterId = this.authService.getCurrentAuthUser()?.userId;
    const postQuery =
      this.activeCommunityId != null || voterId != null
        ? {
            ...(this.activeCommunityId != null ? { communityId: this.activeCommunityId as number } : {}),
            ...(voterId != null ? { voterId } : {})
          }
        : undefined;

    forkJoin({
      communities: this.communityService.getAll().pipe(catchError(() => of([] as Community[]))),
      posts: this.postService.getAll(postQuery).pipe(catchError(() => of([] as Post[])))
    }).subscribe({
      next: ({ communities, posts }) => {
        this.communities = communities;

        if (this.activeCommunityId == null && posts.length === 0 && communities.length > 0) {
          this.loadAllCommunityPosts(communities);
          return;
        }

        this.posts = posts;
        this.loadCommentCounts(posts);
      },
      error: () => {
        this.error = 'Unable to load the post feed right now.';
        this.loading = false;
      }
    });
  }

  private loadAllCommunityPosts(communities: Community[]): void {
    const voterId = this.authService.getCurrentAuthUser()?.userId;
    const postRequests = communities.map((community) =>
      this.postService
        .getAll({
          communityId: community.id,
          ...(voterId != null ? { voterId } : {})
        })
        .pipe(catchError(() => of([] as Post[])))
    );

    forkJoin(postRequests).subscribe({
      next: (postGroups) => {
        const deduped = new Map<number, Post>();
        postGroups.flat().forEach((post) => {
          deduped.set(post.id, post);
        });

        this.posts = Array.from(deduped.values());
        this.loadCommentCounts(this.posts);
      },
      error: () => {
        this.posts = [];
        this.loadCommentCounts([]);
      }
    });
  }

  private resolveCommunityIdParam(paramMaps: ParamMap[]): string | null {
    for (const paramMap of paramMaps) {
      const value = paramMap.get('communityId');
      if (value != null) {
        return value;
      }
    }

    return null;
  }

  private loadCommentCounts(posts: Post[]): void {
    if (posts.length === 0) {
      this.commentCountByPost = {};
      this.loading = false;
      return;
    }

    const commentRequests = posts.map((post) =>
      this.postService.getComments(post.id).pipe(
        map((comments) => ({ postId: post.id, count: comments.length })),
        catchError(() => of({ postId: post.id, count: 0 }))
      )
    );

    forkJoin(commentRequests).subscribe({
      next: (commentMeta) => {
        this.commentCountByPost = commentMeta.reduce<Record<number, number>>((acc, item) => {
          acc[item.postId] = item.count;
          return acc;
        }, {});
        this.loading = false;
      },
      error: () => {
        this.commentCountByPost = {};
        this.loading = false;
      }
    });
  }
}
