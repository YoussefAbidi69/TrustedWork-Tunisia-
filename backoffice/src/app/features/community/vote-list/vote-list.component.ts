import { Component, OnInit } from '@angular/core';
import { CommunityAdminService, VoteDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-vote-list',
  templateUrl: './vote-list.component.html',
  styleUrls: ['./vote-list.component.css']
})
export class VoteListComponent implements OnInit {
  items: VoteDTO[] = [];
  filteredItems: VoteDTO[] = [];
  loading = true;
  searchQuery = '';

  constructor(private service: CommunityAdminService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.service.getVotes().subscribe({
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
      return !query || String(item.voterId).includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }
}
