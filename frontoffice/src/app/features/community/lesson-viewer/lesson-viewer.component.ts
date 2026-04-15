import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { combineLatest, forkJoin, of, Subscription } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';
import { Lesson, LessonType, Section } from '../../../core/models/community.model';

@Component({
  selector: 'app-lesson-viewer',
  templateUrl: './lesson-viewer.component.html',
  styleUrls: ['./lesson-viewer.component.css']
})
export class LessonViewerComponent implements OnInit, OnDestroy {
  courseId: number | null = null;
  lessonId: number | null = null;
  lesson: Lesson | null = null;
  lessons: Lesson[] = [];
  loading = true;
  error = '';
  userId: number | null = null;
  isCompleted = false;
  markBusy = false;
  quizQuestions: any[] = [];
  quizSubmitted = false;
  selectedAnswers: Record<number, string> = {};
  safeVideoUrl: SafeResourceUrl | null = null;
  safePdfUrl: SafeResourceUrl | null = null;

  readonly LessonType = LessonType;

  private sub = new Subscription();

  constructor(
    public route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private courseService: CourseService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    this.userId = user?.userId ?? null;

    this.sub.add(
      combineLatest([this.route.paramMap, this.route.queryParamMap]).subscribe(
        ([params, query]) => {
          const cid = params.get('id');
          const courseId = cid ? Number(cid) : NaN;
          const lid = query.get('lessonId');
          const lessonId = lid ? Number(lid) : NaN;
          if (Number.isNaN(courseId) || Number.isNaN(lessonId)) {
            this.error = 'Leçon introuvable.';
            this.loading = false;
            return;
          }
          this.loadLesson(courseId, lessonId);
        }
      )
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  private loadLesson(courseId: number, lessonId: number): void {
    this.loading = true;
    this.error = '';
    this.courseId = courseId;
    this.lessonId = lessonId;
    this.lesson = null;
    this.quizSubmitted = false;
    this.selectedAnswers = {};
    this.quizQuestions = [];

    this.courseService
      .getSections(courseId)
      .pipe(
        catchError(() => of([] as Section[])),
        switchMap((sections) =>
          forkJoin(
            sections.map((s) =>
              this.courseService.getLessons(s.id).pipe(catchError(() => of([] as Lesson[])))
            )
          ).pipe(
            map((lessonGroups) => {
              const flat: Lesson[] = [];
              lessonGroups.forEach((group) => {
                flat.push(
                  ...[...group].sort(
                    (a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)
                  )
                );
              });
              return flat;
            })
          )
        )
      )
      .subscribe({
        next: (flat) => {
          this.lessons = flat;
          const found = flat.find((l) => l.id === lessonId) || null;
          this.lesson = found;
          if (!found) {
            this.error =
              flat.length === 0
                ? 'This course has no lessons yet.'
                : 'Lesson not found.';
            this.loading = false;
            return;
          }
          this.refreshSafeUrls();
          const uid = this.userId;
          if (uid != null) {
            this.courseService.getProgress(uid, lessonId).subscribe({
              next: (p) => {
                this.isCompleted = p.completed === true;
                this.loading = false;
              },
              error: () => {
                this.isCompleted = false;
                this.loading = false;
              }
            });
          } else {
            this.loading = false;
          }
          if (found.type === LessonType.QUIZ) {
            this.courseService.getQuiz(found.content).subscribe({
              next: (q) => (this.quizQuestions = q || []),
              error: () => (this.quizQuestions = [])
            });
          }
        },
        error: () => {
          this.error = 'Impossible de charger la leçon.';
          this.loading = false;
        }
      });
  }

  private refreshSafeUrls(): void {
    const l = this.lesson;
    if (!l) return;
    if (l.videoUrl) {
      this.safeVideoUrl = this.sanitizer.bypassSecurityTrustResourceUrl(l.videoUrl);
    } else {
      this.safeVideoUrl = null;
    }
    if (l.pdfUrl) {
      this.safePdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(l.pdfUrl);
    } else {
      this.safePdfUrl = null;
    }
  }

  markComplete(): void {
    const uid = this.userId;
    const lid = this.lessonId;
    if (uid == null || lid == null || this.markBusy) return;
    this.markBusy = true;
    this.courseService.markComplete(uid, lid).subscribe({
      next: () => {
        this.isCompleted = true;
        this.markBusy = false;
      },
      error: () => {
        this.markBusy = false;
      }
    });
  }

  get nextLesson(): Lesson | null {
    const lid = this.lessonId;
    if (lid == null || this.lessons.length === 0) return null;
    const idx = this.lessons.findIndex((l) => l.id === lid);
    if (idx < 0 || idx >= this.lessons.length - 1) return null;
    return this.lessons[idx + 1];
  }

  get isLastLesson(): boolean {
    return this.nextLesson === null;
  }

  goNext(): void {
    const n = this.nextLesson;
    const cid = this.courseId;
    const shell = this.route.parent;
    if (!n || cid == null || !shell) return;
    this.router.navigate(['courses', cid, 'learn'], {
      relativeTo: shell,
      queryParams: { lessonId: n.id }
    });
  }

  backToCourse(): void {
    const cid = this.courseId;
    const shell = this.route.parent;
    if (cid == null || !shell) return;
    this.router.navigate(['courses', cid], { relativeTo: shell });
  }

  submitQuiz(): void {
    this.quizSubmitted = true;
  }

  selectOption(qIndex: number, opt: string): void {
    if (this.quizSubmitted) return;
    this.selectedAnswers = { ...this.selectedAnswers, [qIndex]: opt };
  }

  isOptionCorrect(q: any, opt: string): boolean {
    if (q == null) return false;
    if (Array.isArray(q.correctAnswers)) {
      return q.correctAnswers.includes(opt);
    }
    if (q.correctAnswer != null) {
      return q.correctAnswer === opt;
    }
    if (q.answer != null) {
      return q.answer === opt;
    }
    if (typeof q.correctIndex === 'number' && Array.isArray(q.options)) {
      return q.options[q.correctIndex] === opt;
    }
    return false;
  }
}
