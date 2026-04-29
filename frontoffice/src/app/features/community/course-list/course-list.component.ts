import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, CourseDTO } from '../../../core/services/community-admin.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

interface CourseStats { comments: number; reports: number; sections: number; }

@Component({
  selector: 'app-course-list',
  templateUrl: './course-list.component.html',
  styleUrls: ['./course-list.component.css']
})
export class CourseListComponent implements OnInit {
  items: CourseDTO[] = [];
  filteredItems: CourseDTO[] = [];
  loading = true;
  statsLoading = false;
  userMap: { [id: number]: string } = {};
  stats: { [id: number]: CourseStats } = {};
  searchQuery = '';
  actionLoading: number | null = null;

  constructor(private service: CommunityAdminService, private userService: UserService) {}

  ngOnInit(): void {
    this.userService.getUserNameMap().subscribe(map => this.userMap = map);
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.service.getCourses().subscribe({
      next: (data) => {
        this.items = data || [];
        this.applyFilters();
        this.loading = false;
        this.loadStats();
      },
      error: (error) => {
        console.error('Erreur:', error);
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    if (!this.items.length) return;
    this.statsLoading = true;
    const calls = this.items.map(course =>
      forkJoin({
        comments: this.service.getCommentsByCourse(course.id).pipe(catchError(() => of([]))),
        reports:  this.service.getReportsByCourse(course.id).pipe(catchError(() => of([]))),
        sections: this.service.getSectionsByCourse(course.id).pipe(catchError(() => of([])))
      })
    );
    forkJoin(calls).subscribe({
      next: (results) => {
        this.items.forEach((course, i) => {
          this.stats[course.id] = {
            comments: results[i].comments.length,
            reports:  results[i].reports.length,
            sections: results[i].sections.length
          };
        });
        this.statsLoading = false;
      },
      error: () => this.statsLoading = false
    });
  }

  applyFilters(): void {
    this.filteredItems = this.items.filter((item) => {
      const query = this.searchQuery.trim().toLowerCase();
      return !query || item.title?.toLowerCase().includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  deleteItem(item: CourseDTO): void {
    if (!confirm(`Are you sure you want to delete course "${item.title}"?`)) return;
    this.actionLoading = item.id;
    this.service.deleteCourse(item.id).subscribe({
      next: () => {
        this.loadData();
        this.actionLoading = null;
      },
      error: (err) => {
        console.error('Error:', err);
        this.actionLoading = null;
      }
    });
  }
}
