import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, CourseCommentDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-course-comment-list',
  templateUrl: './course-comment-list.component.html',
  styleUrls: ['./course-comment-list.component.css']
})
export class CourseCommentListComponent implements OnInit {
  items: CourseCommentDTO[] = [];
  filteredItems: CourseCommentDTO[] = [];
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
    this.service.getCourseComments().subscribe({
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
      return !query || item.content?.toLowerCase().includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  moderateItem(item: CourseCommentDTO, action: string): void {
    this.actionLoading = item.id;
    this.service.moderateCourseComment(item.id, action).subscribe({
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

  deleteItem(item: CourseCommentDTO): void {
    if (!confirm(`Are you sure you want to delete this comment?`)) return;
    this.actionLoading = item.id;
    this.service.deleteCourseComment(item.id).subscribe({
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
