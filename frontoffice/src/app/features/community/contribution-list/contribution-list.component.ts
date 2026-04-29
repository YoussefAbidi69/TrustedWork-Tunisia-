import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { CommunityAdminService, ContributionDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-contribution-list',
  templateUrl: './contribution-list.component.html',
  styleUrls: ['./contribution-list.component.css']
})
export class ContributionListComponent implements OnInit {
  items: ContributionDTO[] = [];
  filteredItems: ContributionDTO[] = [];
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
    this.service.getContributions().subscribe({
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
      return !query || item.action?.toLowerCase().includes(query) || String(item.userId).includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  deleteItem(item: ContributionDTO): void {
    alert('Delete not supported for contributions.');
  }
}
