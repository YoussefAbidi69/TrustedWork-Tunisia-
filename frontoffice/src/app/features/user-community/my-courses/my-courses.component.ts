import { Component, OnInit } from '@angular/core';
import { Course } from '../../../core/models/community.model';
import { CourseService } from '../../../core/services/course.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-my-courses',
  templateUrl: './my-courses.component.html',
  styleUrls: ['./my-courses.component.css']
})
export class MyCoursesComponent implements OnInit {
  drafts: Course[] = [];
  published: Course[] = [];
  loading = true;
  error = '';
  activeTab: 'all' | 'drafts' | 'published' = 'all';

  constructor(
    private courseService: CourseService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (!userId) {
      this.error = 'You must be logged in to view your courses.';
      this.loading = false;
      return;
    }

    this.courseService.getMyCourses(userId).subscribe({
      next: (courses) => {
        this.drafts = courses.filter((c) => !c.published);
        this.published = courses.filter((c) => c.published);
        this.loading = false;
      },
      error: () => {
        this.error = 'Could not load your courses. Please try again later.';
        this.loading = false;
      }
    });
  }

  setTab(tab: 'all' | 'drafts' | 'published') {
    this.activeTab = tab;
  }
}
