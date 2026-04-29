import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, CommunityDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-community-list',
  templateUrl: './community-list.component.html',
  styleUrls: ['./community-list.component.css']
})
export class CommunityListComponent implements OnInit {
  items: CommunityDTO[] = [];
  filteredItems: CommunityDTO[] = [];
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
    this.service.getCommunities().subscribe({
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
      return !query || item.name?.toLowerCase().includes(query) || item.description?.toLowerCase().includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  deleteItem(item: CommunityDTO): void {
    if (!confirm(`Are you sure you want to delete "${item.name}"?`)) return;
    this.actionLoading = item.id;
    this.service.deleteCommunity(item.id).subscribe({
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
