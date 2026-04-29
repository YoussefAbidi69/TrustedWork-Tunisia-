import { Component, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../core/services/auth.service';
import { CommunityService } from '../../../core/services/community.service';
import { CourseService } from '../../../core/services/course.service';
import { PostService } from '../../../core/services/post.service';
import { Comment, Post, PostStatus, PostType, VoteType } from '../../../core/models/community.model';
import { messageFromApiHttpError } from '../../../core/utils/api-error-message.util';
import { CommentInputComponent } from '../comment-input/comment-input.component';

@Component({
  selector: 'app-post-detail',
  templateUrl: './post-detail.component.html',
  styleUrls: ['./post-detail.component.css']
})
export class PostDetailComponent implements OnInit {
  @ViewChild(CommentInputComponent) commentInput?: CommentInputComponent;

  post: Post | null = null;
  communityName = '';
  comments: Comment[] = [];
  loading = true;
  commentsLoading = false;
  submittingComment = false;
  commentError = '';
  error = '';
  readonly VoteType = VoteType;
  readonly PostType = PostType;
  readonly PostStatus = PostStatus;

  editMode = false;
  saveError = '';
  publishError = '';
  deleteError = '';
  deleting = false;
  courseDownloadError = '';
  courseDownloadBusy = false;
  coursePdfUploading = false;
  coursePdfError = '';
  coursePdfDisplayName = '';
  editForm: FormGroup;

  private postId: number | null = null;

  constructor(
    public route: ActivatedRoute,
    private router: Router,
    public authService: AuthService,
    private postService: PostService,
    private courseService: CourseService,
    private communityService: CommunityService,
    private fb: FormBuilder
  ) {
    this.editForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3)]],
      content: ['', [Validators.required, Validators.minLength(50)]],
      type: [PostType.INFO, Validators.required],
      mediaUrl: [''],
      fileUrl: ['']
    });
  }

  get isCourseEdit(): boolean {
    return this.editForm.get('type')?.value === PostType.COURSE;
  }

  get isPostOwner(): boolean {
    const u = this.authService.getCurrentAuthUser()?.userId;
    return u != null && this.post != null && this.post.createdBy === u;
  }

  get canDownloadCourse(): boolean {
    return (
      this.post != null &&
      this.post.type === PostType.COURSE &&
      this.post.status === PostStatus.PUBLISHED
    );
  }

  get ef() {
    return this.editForm.controls;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (Number.isNaN(id)) {
      this.error = 'Post introuvable.';
      this.loading = false;
      return;
    }

    this.postId = id;
    this.loadPost(id);
  }

  vote(type: VoteType): void {
    if (this.postId == null) return;
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) return;
    this.postService.vote(this.postId, type, userId).subscribe({
      next: () => {
        this.postService.getById(this.postId!, userId).subscribe({
          next: (p) => (this.post = p)
        });
      },
      error: () => {}
    });
  }

  downloadCourseFile(): void {
    if (!this.post || !this.canDownloadCourse) return;
    const uid = this.authService.getCurrentAuthUser()?.userId;
    if (uid == null) {
      void this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.courseDownloadError = '';
    this.courseDownloadBusy = true;
    this.courseService.downloadCoursePostToDevice(this.post, uid).subscribe({
      next: () => {
        this.courseDownloadBusy = false;
      },
      error: (err: HttpErrorResponse) => {
        this.courseDownloadBusy = false;
        void (async () => {
          const serverMsg = await messageFromApiHttpError(err);
          if (err.status === 403) {
            this.courseDownloadError =
              serverMsg ??
              'Download not allowed. As author you can always download. Otherwise share a course once from Contributions, then try again.';
            return;
          }
          this.courseDownloadError =
            serverMsg ??
            'Could not download the file. Check that the post has a valid file URL and ms-community is running.';
        })();
      }
    });
  }

  async sharePost(): Promise<void> {
    if (!this.post) return;
    const title = this.post.title;
    const text = this.post.content?.slice(0, 280) ?? '';
    const url = typeof window !== 'undefined' ? window.location.href : '';
    try {
      if (navigator.share) {
        await navigator.share({ title, text, url });
      } else if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(`${title}\n${url}`);
      }
    } catch {
      /* user cancelled or clipboard unavailable */
    }
  }

  startEdit(): void {
    if (!this.post) return;
    const fu = this.post.fileUrl || '';
    this.coursePdfDisplayName = fu ? (fu.split('/').pop() || 'PDF') : '';
    this.coursePdfError = '';
    this.editForm.patchValue({
      title: this.post.title,
      content: this.post.content,
      type: this.post.type,
      mediaUrl: this.post.mediaUrl || '',
      fileUrl: fu
    });
    this.editMode = true;
    this.saveError = '';
    this.publishError = '';
  }

  cancelEdit(): void {
    this.editMode = false;
    this.saveError = '';
    this.coursePdfError = '';
    this.coursePdfUploading = false;
  }

  setEditType(t: PostType): void {
    if (t !== PostType.COURSE) {
      this.editForm.patchValue({ type: t, fileUrl: '' }, { emitEvent: false });
      this.coursePdfDisplayName = '';
      this.coursePdfError = '';
    } else {
      this.editForm.patchValue({ type: t });
    }
  }

  onCoursePdfEditSelected(event: Event): void {
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
        this.editForm.patchValue({ fileUrl });
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

  clearCoursePdfEdit(fileInput: HTMLInputElement | null): void {
    this.editForm.patchValue({ fileUrl: '' });
    this.coursePdfDisplayName = '';
    this.coursePdfError = '';
    if (fileInput) {
      fileInput.value = '';
    }
  }

  saveEdits(): void {
    if (!this.post || this.postId == null) return;
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const v = this.editForm.getRawValue();
    const payload: Partial<Post> = {
      title: v.title,
      content: v.content,
      type: v.type,
      mediaUrl: v.mediaUrl || '',
      fileUrl: v.type === PostType.COURSE ? (v.fileUrl || '') : '',
      createdBy: this.post.createdBy,
      communityId: this.post.communityId,
      status: this.post.status,
      isAiGenerated: this.post.isAiGenerated,
      isValidated: this.post.isValidated,
      reportCount: this.post.reportCount
    };
    this.postService.update(this.postId, payload as any).subscribe({
      next: (p) => {
        const uid = this.authService.getCurrentAuthUser()?.userId;
        this.postService.getById(p.id, uid).subscribe({
          next: (full) => {
            this.post = full;
            this.editMode = false;
          },
          error: () => {
            this.post = p;
            this.editMode = false;
          }
        });
      },
      error: () => {
        this.saveError = 'Impossible d’enregistrer les modifications.';
      }
    });
  }

  confirmAndDeletePost(): void {
    if (this.postId == null || !this.post || !this.isPostOwner) return;
    if (!confirm('Delete this post permanently? Comments and votes will be removed.')) {
      return;
    }
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) return;
    this.deleteError = '';
    this.deleting = true;
    this.postService.delete(this.postId, userId).subscribe({
      next: () => {
        this.deleting = false;
        this.router.navigate([''], { relativeTo: this.route.parent });
      },
      error: (err: HttpErrorResponse) => {
        this.deleting = false;
        if (err.status === 403) {
          this.deleteError = 'You can only delete your own posts.';
        } else {
          this.deleteError = 'Could not delete the post. Try again.';
        }
      }
    });
  }

  publishPost(): void {
    if (this.postId == null || !this.post || this.post.status !== PostStatus.DRAFT) return;
    this.publishError = '';
    this.postService.publish(this.postId).subscribe({
      next: (p) => {
        const uid = this.authService.getCurrentAuthUser()?.userId;
        this.postService.getById(p.id, uid).subscribe({
          next: (full) => (this.post = full),
          error: () => (this.post = p)
        });
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 422) {
          const msg =
            (err.error && (err.error.message || err.error.error)) || 'Publication refusée.';
          this.publishError = typeof msg === 'string' ? msg : String(msg);
        } else {
          this.publishError = 'Erreur lors de la publication.';
        }
      }
    });
  }

  onSubmitComment(content: string): void {
    if (this.postId == null || this.submittingComment) return;

    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) return;

    this.submittingComment = true;
    this.commentError = '';
    this.postService.addComment(this.postId, content, userId).subscribe({
      next: (c) => {
        this.comments = [...this.comments, c];
        this.commentInput?.clear();
        this.submittingComment = false;
      },
      error: () => {
        this.commentError = 'Could not post your comment. Check that you are signed in and the API is running.';
        this.submittingComment = false;
      }
    });
  }

  private loadPost(postId: number): void {
    this.loading = true;
    this.error = '';

    const voterId = this.authService.getCurrentAuthUser()?.userId;
    this.postService.getById(postId, voterId).subscribe({
      next: (post) => {
        this.post = post;
        this.loadCommunityName(post.communityId);
        this.loadComments(postId);
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger le post.';
        this.loading = false;
      }
    });
  }

  private loadComments(postId: number): void {
    this.commentsLoading = true;
    this.postService
      .getComments(postId)
      .pipe(finalize(() => (this.commentsLoading = false)))
      .subscribe({
        next: (comments) => {
          this.comments = comments;
        },
        error: () => {
          this.comments = [];
        }
      });
  }

  private loadCommunityName(communityId: number): void {
    this.communityName = `Community #${communityId}`;
    this.communityService.getById(communityId).subscribe({
      next: (community) => {
        this.communityName = community.name;
      },
      error: () => {
        this.communityName = `Community #${communityId}`;
      }
    });
  }
}
