import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { switchMap, map, catchError } from 'rxjs/operators';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { Course, Section, Lesson, Comment, Vote, VoteType } from '../../../core/models/community.model';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';
import { messageFromApiHttpError } from '../../../core/utils/api-error-message.util';
import {
  courseDownloadFilename,
  triggerBlobDownload
} from '../../../core/utils/file-download.util';

export interface CourseNode {
  section: Section;
  lessons: Lesson[];
  expanded?: boolean;
}

@Component({
  selector: 'app-course-detail',
  templateUrl: './course-detail.component.html',
  styleUrls: ['./course-detail.component.css']
})
export class CourseDetailComponent implements OnInit {
  course: Course | null = null;
  curriculum: CourseNode[] = [];
  comments: Comment[] = [];
  
  loading = true;
  error = '';
  downloadBusy = false;
  downloadError = '';
  commentsLoading = false;
  commentText = '';
  reportOpen = false;
  reportReason = '';
  reportDescription = '';
  reportSubmitted = false;

  readonly VoteType = VoteType;

  private courseId: number | null = null;
  private readonly autoDownload: boolean;

  constructor(
    public route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private courseService: CourseService,
    private sanitizer: DomSanitizer
  ) {
    const routePath = this.route.snapshot.routeConfig?.path ?? '';
    this.autoDownload = routePath.endsWith('download');
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const parsed = idParam ? Number(idParam) : NaN;
    if (!Number.isFinite(parsed)) {
      this.error = 'Course not found.';
      this.loading = false;
      return;
    }

    this.courseId = parsed;

    // Load Course -> Sections -> Lessons recursively
    this.courseService.getCourse(parsed).pipe(
      switchMap((course) => {
        this.course = course;
        return this.courseService.getSections(course.id).pipe(
          switchMap((sections) => {
            if (!sections || sections.length === 0) {
              return of([]);
            }
            // Fetch lessons for each section
            const lessonRequests = sections.map(sec => 
              this.courseService.getLessons(sec.id).pipe(
                map(lessons => {
                  lessons.forEach(l => {
                    if (l.type === 'QUIZ') {
                      try {
                        const parsed = JSON.parse(l.content);
                        (l as any).parsedQuiz = parsed;
                        (l as any).quizState = { selectedOptionIndex: -1, showAnswer: false };
                      } catch {
                        (l as any).parsedQuiz = { question: 'Invalid quiz format', options: [] };
                        (l as any).quizState = { selectedOptionIndex: -1, showAnswer: false };
                      }
                    }
                  });
                  return { section: sec, lessons: lessons, expanded: true } as CourseNode;
                }),
                catchError(() => of({ section: sec, lessons: [], expanded: false } as CourseNode))
              )
            );
            return forkJoin(lessonRequests);
          }),
          catchError(() => of([]))
        );
      })
    ).subscribe({
      next: (fullCurriculum) => {
        // Sort sections just in case
        this.curriculum = fullCurriculum.sort((a, b) => a.section.orderIndex - b.section.orderIndex);
        this.loading = false;
        
        if (this.autoDownload) {
          this.downloadCourse();
        }
        
        // Load engagement features
        if (this.courseId) {
          this.loadComments();
        }
      },
      error: () => {
        this.error = 'Unable to load this course right now.';
        this.loading = false;
      }
    });
  }

  toggleSection(node: CourseNode) {
    node.expanded = !node.expanded;
  }

  getSafeUrl(url: string | undefined): SafeResourceUrl | null {
    if (!url) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  getLessonIcon(type: string): string {
    switch (type) {
        case 'VIDEO': return 'fa-video';
        case 'PDF': return 'fa-file-pdf';
        case 'CODE': return 'fa-code';
        case 'QUIZ': return 'fa-question-circle';
        default: return 'fa-file-alt';
    }
  }

  selectQuizOption(lesson: any, optIdx: number): void {
    if (lesson.quizState.showAnswer) return;
    lesson.quizState.selectedOptionIndex = optIdx;
  }

  checkQuizAnswer(lesson: any): void {
    if (lesson.quizState.selectedOptionIndex >= 0) {
      lesson.quizState.showAnswer = true;
    }
  }


  downloadCourse(): void {
    if (!this.course || this.downloadBusy) {
      return;
    }
    const uid = this.authService.getCurrentAuthUser()?.userId;
    if (uid == null) {
      void this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    this.downloadBusy = true;
    this.downloadError = '';
    this.courseService.downloadCourse(this.course.id, uid).subscribe({
      next: (blob) => {
        triggerBlobDownload(blob, courseDownloadFilename(this.course!.id, blob));
        this.downloadBusy = false;
      },
      error: (err: HttpErrorResponse) => {
        this.downloadBusy = false;
        void (async () => {
          const serverMsg = await messageFromApiHttpError(err);
          this.downloadError = serverMsg ?? 'Download failed. Please try again.';
        })();
      }
    });
  }

  get downloadRoute(): string[] {
    if (this.courseId == null) {
      return ['/community'];
    }
    return ['/community', 'course', String(this.courseId), 'download'];
  }

  // Course Engagement Features
  loadComments(): void {
    if (!this.courseId) return;
    this.commentsLoading = true;
    this.courseService.getComments(this.courseId).subscribe({
      next: (comments) => {
        this.comments = comments;
        this.commentsLoading = false;
      },
      error: () => {
        this.commentsLoading = false;
      }
    });
  }

  addComment(): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    const content = this.commentText.trim();
    
    if (!this.courseId || !userId || !content) return;
    
    this.courseService.addComment(this.courseId, content, userId).subscribe({
      next: (comment) => {
        this.comments.push(comment);
        this.commentText = '';
      }
    });
  }

  vote(type: VoteType): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (!this.courseId || !userId) return;
    
    this.courseService.vote(this.courseId, type, userId).subscribe({
      next: () => {
        // Vote successful
      }
    });
  }

  toggleReport(): void {
    this.reportOpen = !this.reportOpen;
  }

  submitReport(): void {
    const reason = this.reportReason.trim();
    const description = this.reportDescription.trim();
    const userId = this.authService.getCurrentAuthUser()?.userId;
    
    if (!this.courseId || !reason || !description || !userId) return;
    
    this.courseService.report(this.courseId, reason, description, userId).subscribe({
      next: () => {
        this.reportSubmitted = true;
        this.reportOpen = false;
      }
    });
  }

  cancelReport(): void {
    this.reportOpen = false;
  }
}
