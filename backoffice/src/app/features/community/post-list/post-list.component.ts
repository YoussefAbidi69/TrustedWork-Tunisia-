import { Component, OnInit } from '@angular/core';
import { CommunityAdminService, PostDTO } from '../../../core/services/community-admin.service';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-post-list',
  templateUrl: './post-list.component.html',
  styleUrls: ['./post-list.component.css']
})
export class PostListComponent implements OnInit {
  items: PostDTO[] = [];
  filteredItems: PostDTO[] = [];
  loading = true;
  userMap: { [id: number]: string } = {};
  searchQuery = '';
  actionLoading: number | null = null;

  constructor(private service: CommunityAdminService, private userService: UserService) {}

  ngOnInit(): void {
    this.userService.getUserNameMap().subscribe(map => this.userMap = map);
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.service.getPosts().subscribe({
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
      return !query || item.title?.toLowerCase().includes(query) || item.content?.toLowerCase().includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  deleteItem(item: PostDTO): void {
    if (!confirm(`Are you sure you want to delete post "${item.title}"?`)) return;
    this.actionLoading = item.id;
    this.service.deletePost(item.id, item.authorId || 0).subscribe({
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
