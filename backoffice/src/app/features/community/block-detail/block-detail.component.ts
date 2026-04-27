import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommunityAdminService, BlockDTO } from '../../../core/services/community-admin.service';

@Component({
  selector: 'app-block-detail',
  templateUrl: './block-detail.component.html',
  styleUrls: ['./block-detail.component.css']
})
export class BlockDetailComponent implements OnInit {
  item: BlockDTO | null = null;
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
    this.service.getBlock(id).subscribe({
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
    if (!this.item || !confirm('Are you sure you want to delete this block?')) return;
    this.service.deleteBlock(this.item.id).subscribe({
      next: () => {
        this.router.navigate(['/admin/community/blocks']);
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
