import { Component, OnInit } from '@angular/core';
import { CommunityAdminService, BlockDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-block-list',
  templateUrl: './block-list.component.html',
  styleUrls: ['./block-list.component.css']
})
export class BlockListComponent implements OnInit {
  items: BlockDTO[] = [];
  filteredItems: BlockDTO[] = [];
  loading = true;
  searchQuery = '';
  actionLoading: number | null = null;

  constructor(private service: CommunityAdminService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.service.getBlocks().subscribe({
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
      return !query || item.title?.toLowerCase().includes(query) || item.type?.toLowerCase().includes(query) || String(item.sectionId).includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  deleteItem(item: BlockDTO): void {
    if (!confirm(`Are you sure you want to delete this block?`)) return;
    this.actionLoading = item.id;
    this.service.deleteBlock(item.id).subscribe({
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
