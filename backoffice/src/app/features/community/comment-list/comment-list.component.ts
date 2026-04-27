import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, CommentDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-comment-list',
  templateUrl: './comment-list.component.html',
  styleUrls: ['./comment-list.component.css']
})
export class CommentListComponent implements OnInit {
  items: CommentDTO[] = [];
  filteredItems: CommentDTO[] = [];
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
    this.service.getComments().subscribe({
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

  moderateItem(item: CommentDTO, action: string): void {
    this.actionLoading = item.id;
    this.service.moderateComment(item.id, action).subscribe({
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

  deleteItem(item: CommentDTO): void {
    if (!confirm(`Are you sure you want to delete this comment?`)) return;
    this.actionLoading = item.id;
    this.service.deleteComment(item.id).subscribe({
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
