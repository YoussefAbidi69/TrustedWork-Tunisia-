import { Component, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { CommunityFeedCourseItem } from '../models/community-feed-item.model';
import { AuthService } from '../../../core/services/auth.service';
import { CourseService } from '../../../core/services/course.service';
import { courseDownloadFilename, triggerBlobDownload } from '../../../core/utils/file-download.util';

@Component({
  selector: 'app-course-card',
  templateUrl: './course-card.component.html',
  styleUrls: ['./course-card.component.css']
})
export class CourseCardComponent {
  @Input({ required: true }) course!: CommunityFeedCourseItem;
  @Input() communityName = '';
  @Input({ required: true }) shell!: ActivatedRoute;

  downloadBusy = false;

  constructor(
    private authService: AuthService,
    private courseService: CourseService,
    private router: Router
  ) {}

  get preview(): string {
    const description = this.course?.description ?? '';
    return description.length > 220 ? `${description.slice(0, 220)}…` : description;
  }

  get canDownload(): boolean {
    if (this.course.canDownload != null) {
      return this.course.canDownload;
    }
    return this.course.published;
  }

  onDownloadCourse(event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    const uid = this.authService.getCurrentAuthUser()?.userId;
    if (uid == null) {
      void this.router.navigate(['/auth/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }

    this.downloadBusy = true;
    this.courseService.downloadCourse(this.course.id, uid).subscribe({
      next: (blob) => {
        triggerBlobDownload(blob, courseDownloadFilename(this.course.id, blob));
        this.downloadBusy = false;
      },
      error: () => {
        this.downloadBusy = false;
      }
    });
  }
}