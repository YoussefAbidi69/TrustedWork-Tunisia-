import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { merge, Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { CommunityService } from '../../../core/services/community.service';
import { PostService } from '../../../core/services/post.service';
import { Community, PostStatus, PostType } from '../../../core/models/community.model';
import { messageFromApiHttpError } from '../../../core/utils/api-error-message.util';

@Component({
  selector: 'app-post-create',
  templateUrl: './post-create.component.html',
  styleUrls: ['./post-create.component.css']
})
export class PostCreateComponent implements OnInit, OnDestroy {
  submitted = false;
  publishError = '';
  saveError = '';
  communities: Community[] = [];
  communitiesLoading = true;
  communitiesError = '';
  communitySelect = new FormControl<number | null>(null);

  coursePdfUploading = false;
  coursePdfError = '';
  /** Original filename after a successful upload (for display). */
  coursePdfDisplayName = '';
  aiCoursePlaceholderNotice = '';

  readonly PostType = PostType;
  form: FormGroup;

  private readonly destroy$ = new Subject<void>();
  /** Resolved target community: nested path, parent /communities/:communityId, or ?communityId= */
  private routeCommunityId: number | null = null;

  constructor(
    private fb: FormBuilder,
    public route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private postService: PostService,
    private communityService: CommunityService
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3)]],
      content: ['', [Validators.required, Validators.minLength(50)]],
      type: [PostType.INFO, Validators.required],
      mediaUrl: [''],
      fileUrl: [''],
      publishNow: [false]
    });
  }

  ngOnInit(): void {
    merge(this.route.paramMap, this.route.queryParamMap)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.syncCommunityFromRoute());

    this.form
      .get('type')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((t) => {
        if (t !== PostType.COURSE) {
          this.form.patchValue({ fileUrl: '' }, { emitEvent: false });
          this.coursePdfDisplayName = '';
          this.coursePdfError = '';
        }
      });

    this.loadCommunities();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get f() {
    return this.form.controls;
  }

  get isCourse(): boolean {
    return this.form.get('type')?.value === PostType.COURSE;
  }

  /** When true, user must pick a community in the dropdown */
  get showCommunityPicker(): boolean {
    return this.routeCommunityId == null;
  }

  get effectiveCommunityId(): number | null {
    if (this.routeCommunityId != null) {
      return this.routeCommunityId;
    }
    const v = this.communitySelect.value;
    return v != null && Number.isFinite(v) ? v : null;
  }

  /** Name from loaded list when available (e.g. after getAll). */
  get resolvedCommunityName(): string {
    const id = this.effectiveCommunityId;
    if (id == null) {
      return '';
    }
    const c = this.communities.find((x) => x.id === id);
    return c?.name?.trim() ?? '';
  }

  retryLoadCommunities(): void {
    this.loadCommunities();
  }

  onGenerateCourseWithAi(): void {
    this.aiCoursePlaceholderNotice =
      'AI course generation is not available yet. Upload a PDF for now, or save as draft and try again later.';
  }

  dismissAiNotice(): void {
    this.aiCoursePlaceholderNotice = '';
  }

  onCoursePdfSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    const lower = file.name.toLowerCase();
    if (!lower.endsWith('.pdf')) {
      this.coursePdfError = 'Please choose a PDF file.';
      input.value = '';
      return;
    }
    this.coursePdfError = '';
    this.coursePdfUploading = true;
    this.postService.uploadCoursePdf(file).subscribe({
      next: ({ fileUrl }) => {
        this.form.patchValue({ fileUrl });
        this.coursePdfDisplayName = file.name;
        this.coursePdfUploading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.coursePdfUploading = false;
        void (async () => {
          const serverMsg = await messageFromApiHttpError(err);
          const fallback =
            typeof err.error?.message === 'string'
              ? err.error.message
              : typeof err.error?.error === 'string'
                ? err.error.error
                : err.message;
          this.coursePdfError =
            serverMsg ??
            (typeof fallback === 'string'
              ? fallback
              : 'Upload failed. Ensure ms-community is running and FILEPOST_API_KEY is set on the server.');
        })();
        input.value = '';
      }
    });
  }

  clearCoursePdf(fileInput: HTMLInputElement | null): void {
    this.form.patchValue({ fileUrl: '' });
    this.coursePdfDisplayName = '';
    this.coursePdfError = '';
    if (fileInput) {
      fileInput.value = '';
    }
  }

  private loadCommunities(): void {
    this.communitiesLoading = true;
    this.communitiesError = '';
    this.communityService
      .getAll()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.communitiesLoading = false;
        })
      )
      .subscribe({
        next: (list) => {
          this.communities = list ?? [];
          this.syncCommunityFromRoute();
        },
        error: () => {
          this.communities = [];
          this.communitiesError =
            'Communities could not be loaded. Check that ms-community is running and CORS allows this origin.';
        }
      });
  }

  /**
   * Resolves community id from:
   * 1) Route `communities/:id/posts/new` (param `id`)
   * 2) Parent app route `communities/:communityId` (e.g. /communities/5/posts/new)
   * 3) Query `?communityId=`
   */
  private syncCommunityFromRoute(): void {
    const snapshot = this.route.snapshot;
    const fromQuery = snapshot.queryParamMap.get('communityId');
    const path = snapshot.routeConfig?.path ?? '';
    const onNestedCreatePath =
      path.includes('communities') && path.includes('posts') && path.includes('new');
    const idOnNested = onNestedCreatePath ? snapshot.paramMap.get('id') : null;

    let ancestorCommunityId: string | null = null;
    let r: ActivatedRoute | null = this.route.parent;
    while (r) {
      const c = r.snapshot.paramMap.get('communityId');
      if (c != null) {
        ancestorCommunityId = c;
        break;
      }
      r = r.parent;
    }

    let resolved: number | null = null;
    if (idOnNested != null) {
      const n = Number(idOnNested);
      if (Number.isFinite(n)) {
        resolved = n;
      }
    }
    if (resolved == null && ancestorCommunityId != null) {
      const n = Number(ancestorCommunityId);
      if (Number.isFinite(n)) {
        resolved = n;
      }
    }
    if (resolved == null && fromQuery != null) {
      const n = Number(fromQuery);
      if (Number.isFinite(n)) {
        resolved = n;
      }
    }

    this.routeCommunityId = resolved;

    if (this.routeCommunityId != null) {
      this.communitySelect.setValue(this.routeCommunityId, { emitEvent: false });
      this.communitySelect.clearValidators();
    } else {
      this.communitySelect.setValidators([Validators.required]);
    }
    this.communitySelect.updateValueAndValidity({ emitEvent: false });
  }

  onSubmit(): void {
    this.submitted = true;
    this.publishError = '';
    this.saveError = '';
    const communityId = this.effectiveCommunityId;
    if (this.form.invalid || communityId == null) {
      this.communitySelect.markAsTouched();
      return;
    }
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) {
      this.saveError = 'You must be signed in to create a post.';
      return;
    }

    const raw = this.form.getRawValue();
    if (raw.type === PostType.COURSE && raw.publishNow) {
      const fu = (raw.fileUrl || '').trim();
      if (!fu) {
        this.saveError =
          'Course posts must include a PDF before publishing. Upload a file or save as draft and add one before you publish.';
        return;
      }
    }

    const payload: Record<string, unknown> = {
      title: raw.title,
      content: raw.content,
      type: raw.type,
      mediaUrl: raw.mediaUrl || '',
      fileUrl: this.isCourse ? raw.fileUrl || '' : '',
      communityId,
      createdBy: userId,
      status: PostStatus.DRAFT
    };

    this.postService.create(payload as any).subscribe({
      next: (post) => {
        if (raw.publishNow) {
          this.postService.publish(post.id).subscribe({
            next: () =>
              this.router.navigate(['communities', String(communityId)], {
                relativeTo: this.route.parent
              }),
            error: (err: HttpErrorResponse) => {
              if (err.status === 422) {
                const msg =
                  (err.error && (err.error.message || err.error.error)) ||
                  'Publication refusée.';
                this.publishError = typeof msg === 'string' ? msg : String(msg);
              } else {
                this.publishError = 'Erreur lors de la publication.';
              }
            }
          });
        } else {
          this.router.navigate(['communities', String(communityId)], {
            relativeTo: this.route.parent
          });
        }
      },
      error: (err: HttpErrorResponse) => {
        this.submitted = true;
        this.saveError =
          err.error?.message || err.message || 'Could not create the post. Check the API and your data.';
      }
    });
  }
}
