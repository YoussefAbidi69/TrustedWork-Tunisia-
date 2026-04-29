import { Component, OnInit } from '@angular/core';
import { CommunityAdminService, CourseVoteDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-course-vote-list',
  templateUrl: './course-vote-list.component.html',
  styleUrls: ['./course-vote-list.component.css']
})
export class CourseVoteListComponent implements OnInit {
  items: CourseVoteDTO[] = [];
  filteredItems: CourseVoteDTO[] = [];
  loading = true;
  searchQuery = '';

  constructor(private service: CommunityAdminService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.service.getCourseVotes().subscribe({
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
