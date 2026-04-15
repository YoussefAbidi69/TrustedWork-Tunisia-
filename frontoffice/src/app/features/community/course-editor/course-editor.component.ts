import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';
import { LessonType } from '../../../core/models/community.model';

interface LessonDraft {
  title: string;
  pdfUrl: string;
}

interface SectionDraft {
  title: string;
  lessons: LessonDraft[];
}

@Component({
  selector: 'app-course-editor',
  templateUrl: './course-editor.component.html',
  styleUrls: ['./course-editor.component.css']
})
export class CourseEditorComponent implements OnInit {
  communityId: number | null = null;
  title = '';
  description = '';
  published = false;
  sections: SectionDraft[] = [];
  saving = false;
  saveError = '';

  readonly LessonType = LessonType;

  constructor(
    public route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private courseService: CourseService
  ) {}

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap.get('communityId');
    const cid = q ? Number(q) : NaN;
    if (Number.isFinite(cid)) {
      this.communityId = cid;
    }
    if (this.sections.length === 0) {
      this.addSection();
    }
  }

  addSection(): void {
    this.sections.push({
      title: `Module ${this.sections.length + 1}`,
      lessons: [{ title: 'Lesson 1', pdfUrl: '' }]
    });
  }

  addLesson(section: SectionDraft): void {
    section.lessons.push({ title: `Lesson ${section.lessons.length + 1}`, pdfUrl: '' });
  }

  removeLesson(section: SectionDraft, index: number): void {
    section.lessons.splice(index, 1);
    if (section.lessons.length === 0) {
      section.lessons.push({ title: 'Lesson 1', pdfUrl: '' });
    }
  }

  removeSection(index: number): void {
    this.sections.splice(index, 1);
    if (this.sections.length === 0) {
      this.addSection();
    }
  }

  async save(): Promise<void> {
    this.saveError = '';
    const uid = this.authService.getCurrentAuthUser()?.userId;
    if (this.communityId == null) {
      this.saveError = 'Missing community. Open this page from a community (Create learning course).';
      return;
    }
    if (uid == null) {
      this.saveError = 'You must be signed in.';
      return;
    }
    const t = this.title.trim();
    if (t.length < 3) {
      this.saveError = 'Title must be at least 3 characters.';
      return;
    }
    for (const sec of this.sections) {
      if (!sec.title.trim()) {
        this.saveError = 'Each module needs a title.';
        return;
      }
      for (const les of sec.lessons) {
        if (!les.title.trim()) {
          this.saveError = 'Each lesson needs a title.';
          return;
        }
        if (!les.pdfUrl.trim()) {
          this.saveError = 'PDF lessons need an external PDF URL (or upload elsewhere and paste the link).';
          return;
        }
      }
    }

    this.saving = true;
    try {
      const course = await firstValueFrom(
        this.courseService.createCourse({
          title: t,
          description: this.description.trim(),
          authorId: uid,
          communityId: this.communityId,
          published: this.published
        })
      );

      for (let sIdx = 0; sIdx < this.sections.length; sIdx++) {
        const sec = this.sections[sIdx];
        const section = await firstValueFrom(
          this.courseService.createSection(course.id, {
            title: sec.title.trim(),
            orderIndex: sIdx
          })
        );
        for (let lIdx = 0; lIdx < sec.lessons.length; lIdx++) {
          const les = sec.lessons[lIdx];
          await firstValueFrom(
            this.courseService.createLesson(section.id, {
              title: les.title.trim(),
              type: LessonType.PDF,
              pdfUrl: les.pdfUrl.trim(),
              content: '',
              orderIndex: lIdx
            })
          );
        }
      }

      const shell = this.route.parent;
      if (shell) {
        await this.router.navigate(['courses', course.id], { relativeTo: shell });
      }
    } catch (e: unknown) {
      this.saveError =
        typeof e === 'object' && e !== null && 'message' in e
          ? String((e as { message?: string }).message)
          : 'Could not save the course.';
    } finally {
      this.saving = false;
    }
  }

  cancel(): void {
    const shell = this.route.parent;
    if (this.communityId != null && shell) {
      void this.router.navigate(['communities', this.communityId], { relativeTo: shell });
    } else if (shell) {
      void this.router.navigate([''], { relativeTo: shell });
    }
  }
}
