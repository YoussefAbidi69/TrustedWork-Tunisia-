import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';
import { Course, Lesson, Progress, Section } from '../../../core/models/community.model';

interface LessonRow {
  lesson: Lesson;
  sectionTitle: string;
}

@Component({
  selector: 'app-course-detail',
  templateUrl: './course-detail.component.html',
  styleUrls: ['./course-detail.component.css']
})
export class CourseDetailComponent implements OnInit {
  course: Course | null = null;
  sections: Section[] = [];
  lessonRows: LessonRow[] = [];
  progressMap = new Map<number, Progress>();
  loading = true;
  error = '';
  userId: number | null = null;
  courseId: number | null = null;

  constructor(
    public route: ActivatedRoute,
    private authService: AuthService,
    private courseService: CourseService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (Number.isNaN(id)) {
      this.error = 'Cours introuvable.';
      this.loading = false;
      return;
    }
    this.courseId = id;
    const authUser = this.authService.getCurrentAuthUser();
    this.userId = authUser?.userId ?? null;

    this.courseService      .getCourse(id)
      .pipe(
        catchError(() => of(null as Course | null)),
        switchMap((course) => {
          this.course = course;
          if (!course) return of(null);
          return this.courseService.getSections(id).pipe(
            catchError(() => of([] as Section[])),
            switchMap((sections) => {
              this.sections = [...sections].sort(
                (a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)
              );
              if (this.sections.length === 0) return of([]);
              return forkJoin(
                this.sections.map((s) =>
                  this.courseService.getLessons(s.id).pipe(
                    map((lessons) => ({ section: s, lessons })),
                    catchError(() => of({ section: s, lessons: [] as Lesson[] }))
                  )
                )
              );
            })
          );
        })
      )
      .subscribe({
        next: (result) => {
          if (result === null) {
            this.error = 'Course not found.';
            this.loading = false;
            return;
          }
          const rows: LessonRow[] = [];
          for (const block of result) {
            const sorted = [...block.lessons].sort(
              (a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)
            );
            for (const lesson of sorted) {
              rows.push({ lesson, sectionTitle: block.section.title });
            }
          }
          this.lessonRows = rows;
          this.loadProgressForLessons();
        },
        error: () => {
          this.error = 'Impossible de charger le cours.';
          this.loading = false;
        }
      });
  }

  private loadProgressForLessons(): void {
    const uid = this.userId;
    if (uid == null || this.lessonRows.length === 0) {
      this.loading = false;
      return;
    }
    forkJoin(
      this.lessonRows.map((row) =>
        this.courseService.getProgress(uid, row.lesson.id).pipe(
          catchError(() => of(null as Progress | null))
        )
      )
    ).subscribe({
      next: (progressList) => {
        progressList.forEach((p, i) => {
          const lid = this.lessonRows[i].lesson.id;
          if (p) this.progressMap.set(lid, p);
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  get totalLessons(): number {
    return this.lessonRows.length;
  }

  get completedCount(): number {
    return this.lessonRows.filter(
      (r) => this.progressMap.get(r.lesson.id)?.completed === true
    ).length;
  }

  get canCertificate(): boolean {
    return this.totalLessons > 0 && this.completedCount === this.totalLessons;
  }

  issueCertificate(): void {
    const uid = this.userId;
    const cid = this.courseId;
    if (uid == null || cid == null) return;
    this.courseService.issueCertificate(uid, cid).subscribe();
  }

  lessonsForSection(section: Section): LessonRow[] {
    return this.lessonRows.filter((r) => r.lesson.sectionId === section.id);
  }
}
