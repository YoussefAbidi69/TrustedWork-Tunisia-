import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap, Router, convertToParamMap } from '@angular/router';
import { combineLatest, forkJoin, of, Subject, Observable } from 'rxjs';
import { catchError, map, takeUntil, tap } from 'rxjs/operators';

import { Community, Course, VoteType } from '../../../core/models/community.model';
import { CommunityService } from '../../../core/services/community.service';
import { CourseService } from '../../../core/services/course.service';
import { PostService } from '../../../core/services/post.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  CommunityFeedCourseItem,
  CommunityFeedItem,
  CommunityFeedPostItem,
  isCommunityFeedCourseItem,
  isCommunityFeedPostItem,
  normalizeCommunityFeed,
  normalizeCommunityFeedItem
} from '../models/community-feed-item.model';

@Component({
  selector: 'app-post-feed',
  templateUrl: './post-feed.component.html',
  styleUrls: ['./post-feed.component.css']
})
export class PostFeedComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';

  feedItems: CommunityFeedItem[] = [];
  communities: Community[] = [];
  commentCountByPost: Record<number, number> = {};
  activeCommunityId: number | null = null;

  myPosts$: Observable<CommunityFeedPostItem[]>;

  private readonly destroy$ = new Subject<void>();

  constructor(
    public readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly postService: PostService,
    private readonly courseService: CourseService,
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
        map((items) =>
          normalizeCommunityFeed(items as unknown[]).filter(
            (item): item is CommunityFeedPostItem =>
              isCommunityFeedPostItem(item) && item.createdBy === currentUser.userId
          )
        ),
        catchError(() => of([] as CommunityFeedPostItem[]))
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
            const normalized = normalizeCommunityFeedItem(updated, this.activeCommunityId);
            if (!normalized || !isCommunityFeedPostItem(normalized)) {
              return;
            }
            const idx = this.feedItems.findIndex(
              (item) => isCommunityFeedPostItem(item) && item.id === normalized.id
            );
            if (idx >= 0) {
              this.feedItems = [
                ...this.feedItems.slice(0, idx),
                normalized,
                ...this.feedItems.slice(idx + 1)
              ];
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
      this.router.navigate(['post', 'new'], {
        relativeTo: parent,
        queryParams: { communityId: cid }
      });
    } else {
      this.router.navigate(['post', 'new'], { relativeTo: parent });
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

  trackByFeedItemId(_index: number, item: CommunityFeedItem): string {
    return `${item.type}-${item.id}`;
  }

  isPostItem(item: CommunityFeedItem): item is CommunityFeedPostItem {
    return isCommunityFeedPostItem(item);
  }

  isCourseItem(item: CommunityFeedItem): item is CommunityFeedCourseItem {
    return isCommunityFeedCourseItem(item);
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
      communities: this.communityService.getAll().pipe(
        tap(c => console.log('[Feed] communities raw:', c)),
        catchError((err) => { console.error('[Feed] communities error:', err); return of([] as Community[]); })
      ),
      posts: this.postService.getAll(postQuery).pipe(
        tap(p => console.log('[Feed] posts raw:', p)),
        catchError((err) => { console.error('[Feed] posts error:', err); return of([] as unknown[]); })
      )
    }).subscribe({
      next: ({ communities, posts }) => {
        console.log('[Feed] forkJoin resolved — communities:', communities.length, 'posts:', posts.length);
        this.communities = communities;

        if (this.activeCommunityId != null) {
          this.courseService
            .listByCommunity(this.activeCommunityId)
            .pipe(catchError(() => of([] as Course[])))
            .subscribe({
              next: (courses) => {
                this.feedItems = this.mergeFeedItems(
                  normalizeCommunityFeed(posts as unknown[], this.activeCommunityId),
                  courses
                );
                this.loadCommentCounts(this.feedItems);
              },
              error: () => {
                this.feedItems = normalizeCommunityFeed(posts as unknown[], this.activeCommunityId);
                this.loadCommentCounts(this.feedItems);
              }
            });
          return;
        }

        this.loadAllCommunityFeed(communities, posts as unknown[]);
      },
      error: () => {
        this.error = 'Unable to load the post feed right now.';
        this.loading = false;
      }
    });
  }

  private loadAllCommunityFeed(communities: Community[], posts: unknown[]): void {
    console.log('[Feed] loadAllCommunityFeed — normalizing', posts.length, 'posts...');
    const normalized = normalizeCommunityFeed(posts, this.activeCommunityId);
    console.log('[Feed] normalized feedItems:', normalized);
    if (communities.length === 0) {
      this.feedItems = normalized;
      this.loadCommentCounts(this.feedItems);
      return;
    }

    const courseRequests = communities.map((community) =>
      this.courseService
        .listByCommunity(community.id)
        .pipe(catchError(() => of([] as Course[])))
    );

    forkJoin(courseRequests).subscribe({
      next: (courseGroups) => {
        const courses = courseGroups.flat();
        this.feedItems = this.mergeFeedItems(
          normalizeCommunityFeed(posts, this.activeCommunityId),
          courses
        );
        this.loadCommentCounts(this.feedItems);
      },
      error: () => {
        this.feedItems = normalizeCommunityFeed(posts, this.activeCommunityId);
        this.loadCommentCounts(this.feedItems);
      }
    });
  }

  private mergeFeedItems(posts: CommunityFeedItem[], courses: Course[]): CommunityFeedItem[] {
    const mappedCourses: CommunityFeedCourseItem[] = courses
      .filter((course) => course.id != null)
      .map((course) => ({
        id: course.id,
        type: 'COURSE',
        title: course.title || `Course #${course.id}`,
        description: course.description || '',
        published: !!course.published,
        communityId: course.communityId ?? 0,
        createdBy: course.authorId
      }));

    const deduped = new Map<string, CommunityFeedItem>();
    [...posts, ...mappedCourses].forEach((item) => {
      deduped.set(`${item.type}-${item.id}`, item);
    });

    return Array.from(deduped.values()).sort((a, b) => b.id - a.id);
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

  private loadCommentCounts(items: CommunityFeedItem[]): void {
    const posts = items.filter(isCommunityFeedPostItem);
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
