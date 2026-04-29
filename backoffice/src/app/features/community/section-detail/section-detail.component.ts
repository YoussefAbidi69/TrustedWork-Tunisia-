import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommunityAdminService, SectionDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-section-detail',
  templateUrl: './section-detail.component.html',
  styleUrls: ['./section-detail.component.css']
})
export class SectionDetailComponent implements OnInit {
  item: SectionDTO | null = null;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: CommunityAdminService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadData(id);
    }
  }

  loadData(id: number): void {
    this.loading = true;
    this.service.getSection(id).subscribe({
      next: (data) => {
        this.item = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error:', err);
        this.loading = false;
      }
    });
  }

  deleteItem(): void {
    if (!this.item || !confirm('Are you sure you want to delete this section?')) return;
    this.service.deleteSection(this.item.id).subscribe({
      next: () => {
        this.router.navigate(['/admin/community/sections']);
      },
      error: (err) => {
        console.error('Error:', err);
      }
    });
  }

  goBack(): void {
    window.history.back();
  }
}
