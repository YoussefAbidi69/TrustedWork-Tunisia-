import { Component, OnInit } from '@angular/core';
import {
  UserService,
  SuspensionRecordDTO
} from '../../../core/services/user.service';

@Component({
  selector: 'app-suspensions',
  templateUrl: './suspensions.component.html',
  styleUrls: ['./suspensions.component.css']
})
export class SuspensionsComponent implements OnInit {
  suspensions: SuspensionRecordDTO[] = [];
  filteredSuspensions: SuspensionRecordDTO[] = [];
  loading = true;
  actionLoading: number | null = null;
  searchQuery = '';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadSuspensions();
  }

  loadSuspensions(): void {
    this.loading = true;
    this.userService.getAllActiveSuspensions().subscribe({
      next: (data) => {
        this.suspensions = data || [];
        this.applyFilter();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement suspensions:', error);
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    const q = this.searchQuery.trim().toLowerCase();

    if (!q) {
      this.filteredSuspensions = [...this.suspensions];
      return;
    }

    this.filteredSuspensions = this.suspensions.filter((item) => {
      return (
        String(item.userId).toLowerCase().includes(q) ||
        (item.reason || '').toLowerCase().includes(q) ||
        (item.suspendedBy || '').toLowerCase().includes(q) ||
        (item.liftedBy || '').toLowerCase().includes(q)
      );
    });
  }

  onSearch(): void {
    this.applyFilter();
  }

  liftSuspension(item: SuspensionRecordDTO): void {
    this.actionLoading = item.userId;

    this.userService.liftSuspension(item.userId).subscribe({
      next: () => {
        this.loadSuspensions();
        this.actionLoading = null;
      },
      error: (error) => {
        console.error('Erreur lift suspension:', error);
        this.actionLoading = null;
      }
    });
  }

  getStatusBadge(active: boolean): string {
    return active ? 'badge-danger' : 'badge-success';
  }
}