import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, CourseDTO, SectionDTO, BlockDTO, CourseCommentDTO, CourseReportDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-course-detail',
  templateUrl: './course-detail.component.html',
  styleUrls: ['./course-detail.component.css']
})
export class CourseDetailComponent implements OnInit {
  course: CourseDTO | null = null;
  sections: SectionDTO[] = [];
  blocks: { [sectionId: number]: BlockDTO[] } = {};
  comments: CourseCommentDTO[] = [];
  reports: CourseReportDTO[] = [];
  
  loading = true;
  userMap: { [id: number]: string } = {};
  activeTab = 'sections';
  expandedSections: { [id: number]: boolean } = {};

  constructor(private route: ActivatedRoute, private router: Router, private service: CommunityAdminService, private userService: UserService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.userService.getUserNameMap().subscribe(map => this.userMap = map);
    if (id) {
      this.loadData(Number(id));
    }
  }

  loadData(id: number): void {
    this.loading = true;
    this.service.getCourse(id).subscribe({
      next: (data) => {
        this.course = data;
        this.loadRelations(id);
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  loadRelations(id: number): void {
    this.service.getSectionsByCourse(id).subscribe(data => {
      this.sections = data || [];
      // SectionResponse already embeds blocks — no extra calls needed
      this.sections.forEach(sec => {
        this.blocks[sec.id] = (sec as any).blocks || [];
      });
    });

    this.service.getCommentsByCourse(id).subscribe(data => this.comments = data || []);
    this.service.getReportsByCourse(id).subscribe(data => this.reports = data || []);
    this.loading = false;
  }

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  toggleSection(sectionId: number): void {
    this.expandedSections[sectionId] = !this.expandedSections[sectionId];
  }

  formatJson(raw: string | undefined): string {
    if (!raw) return 'No quiz data.';
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  }

  deleteCourse(): void {
    if (!this.course || !confirm(`Delete course "${this.course.title}"?`)) return;
    this.service.deleteCourse(this.course.id).subscribe({
      next: () => this.router.navigate(['/admin/community/courses']),
      error: (e: any) => console.error('Delete failed', e)
    });
  }
}
