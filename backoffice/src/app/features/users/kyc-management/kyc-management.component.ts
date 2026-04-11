import { Component, OnInit } from '@angular/core';
import {
  UserService,
  KycRequestDTO
} from '../../../core/services/user.service';

@Component({
  selector: 'app-kyc-management',
  templateUrl: './kyc-management.component.html',
  styleUrls: ['./kyc-management.component.css']
})
export class KycManagementComponent implements OnInit {
  pendingUsers: KycRequestDTO[] = [];
  loading = true;
  actionLoading: number | null = null;
  selectedUser: KycRequestDTO | null = null;
  notes = '';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadPending();
  }

  loadPending(): void {
    this.loading = true;
    this.userService.getPendingKyc().subscribe({
      next: (data) => {
        this.pendingUsers = data;
        this.loading = false;
        console.log('[KYC ADMIN] pending requests:', data);
      },
      error: (error) => {
        console.error('Erreur chargement KYC pending:', error);
        this.loading = false;
      }
    });
  }

  openReview(user: KycRequestDTO): void {
    this.selectedUser = user;
    this.notes = '';
    console.log('[KYC ADMIN] selected request:', user);
  }

  closeReview(): void {
    this.selectedUser = null;
    this.notes = '';
  }

  approve(): void {
    if (!this.selectedUser?.id) {
      console.error('ID de demande KYC introuvable');
      return;
    }

    this.actionLoading = this.selectedUser.id;

    this.userService
      .reviewKyc(this.selectedUser.id, 'APPROVED', this.notes)
      .subscribe({
        next: (response) => {
          console.log('[KYC ADMIN] approved:', response);
          this.closeReview();
          this.loadPending();
          this.actionLoading = null;
        },
        error: (error) => {
          console.error('Erreur approbation KYC:', error);
          this.actionLoading = null;
        }
      });
  }

  reject(): void {
    if (!this.selectedUser?.id) {
      console.error('ID de demande KYC introuvable');
      return;
    }

    this.actionLoading = this.selectedUser.id;

    this.userService
      .reviewKyc(this.selectedUser.id, 'REJECTED', this.notes)
      .subscribe({
        next: (response) => {
          console.log('[KYC ADMIN] rejected:', response);
          this.closeReview();
          this.loadPending();
          this.actionLoading = null;
        },
        error: (error) => {
          console.error('Erreur rejet KYC:', error);
          this.actionLoading = null;
        }
      });
  }

  getInitials(u: KycRequestDTO): string {
    return `${u.firstName?.[0] || ''}${u.lastName?.[0] || ''}`.toUpperCase();
  }

  getFileUrl(path: string | undefined | null): string {
    if (!path) return '';
    return `http://localhost:8081/api${path}`;
  }
}