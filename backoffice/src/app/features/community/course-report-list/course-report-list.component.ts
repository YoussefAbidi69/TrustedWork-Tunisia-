import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, CourseReportDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-course-report-list',
  templateUrl: './course-report-list.component.html',
  styleUrls: ['./course-report-list.component.css']
})
export class CourseReportListComponent implements OnInit {
  items: CourseReportDTO[] = [];
  filteredItems: CourseReportDTO[] = [];
  loading = true;
  userMap: { [id: number]: string } = {};
  searchQuery = '';
  actionLoading: number | null = null;

  constructor(private service: CommunityAdminService, private userService: UserService) {}

  ngOnInit(): void {
    
    this.userService.getUserNameMap().subscribe(map => this.userMap = map);this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.service.getCourseReports().subscribe({
      next: (data) => {
        this.items = data || [];
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur:', error);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredItems = this.items.filter((item) => {
      const query = this.searchQuery.trim().toLowerCase();
      return !query || item.reason?.toLowerCase().includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  resolveItem(item: CourseReportDTO, status: string): void {
    this.actionLoading = item.id;
    this.service.resolveCourseReport(item.id, status).subscribe({
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

  deleteItem(item: CourseReportDTO): void {
    if (!confirm(`Are you sure you want to delete this report?`)) return;
    this.actionLoading = item.id;
    this.service.deleteCourseReport(item.id).subscribe({
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
