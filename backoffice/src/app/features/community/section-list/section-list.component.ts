import { Component, OnInit } from '@angular/core';
import { CommunityAdminService, SectionDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-section-list',
  templateUrl: './section-list.component.html',
  styleUrls: ['./section-list.component.css']
})
export class SectionListComponent implements OnInit {
  items: SectionDTO[] = [];
  filteredItems: SectionDTO[] = [];
  loading = true;
  searchQuery = '';
  actionLoading: number | null = null;

  constructor(private service: CommunityAdminService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.service.getSections().subscribe({
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
      return !query || item.title?.toLowerCase().includes(query) || String(item.courseId).includes(query);
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  deleteItem(item: SectionDTO): void {
    if (!confirm(`Are you sure you want to delete section "${item.title}"?`)) return;
    this.actionLoading = item.id;
    this.service.deleteSection(item.id).subscribe({
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
