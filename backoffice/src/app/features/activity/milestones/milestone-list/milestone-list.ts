import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MilestoneService } from '../../../../core/services/milestone.service';
import { Milestone } from '../../../../core/models/milestone.model';
import { AuthService } from '../../../../core/services/auth.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-milestone-list',
  templateUrl: './milestone-list.html',
  styleUrl: './milestone-list.css'
})
export class MilestoneListComponent implements OnInit {
  milestones: Milestone[] = [];
  contractId: number | null = null;
  loading = false;
  error = '';
  
  // Rejection Modal State
  showRejectModal = false;
  rejectionReason = '';
  newDeadline = '';
  selectedMilestoneId: number | null = null;
  minDate = new Date().toISOString().split('T')[0];

  constructor(
    private route: ActivatedRoute,
    private milestoneService: MilestoneService,
    public authService: AuthService
  ) {}

  get isClient(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'CLIENT';
  }

  get isFreelancer(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'FREELANCER';
  }

  get isAdmin(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'ADMIN';
  }

  ngOnInit(): void {
    this.contractId = this.route.snapshot.params['contractId'];
    this.loadMilestones();
  }

  loadMilestones(): void {
    this.loading = true;
    
    if (this.contractId) {
      this.milestoneService.getByContractId(this.contractId).subscribe({
        next: (milestones) => {
          this.milestones = milestones;
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Erreur lors du chargement des jalons';
          this.loading = false;
          console.error(err);
        }
      });
    } else {
      if (this.isAdmin) {
        this.milestoneService.getAll(0, 100).subscribe({
          next: (response) => {
            this.milestones = response.content || response;
            this.loading = false;
          },
          error: (err) => {
            this.error = 'Erreur lors du chargement des jalons';
            this.loading = false;
            console.error(err);
          }
        });
      } else {
        this.milestones = [];
        this.loading = false;
        this.error = 'Veuillez consulter vos jalons depuis la page de détails de l\'un de vos contrats.';
      }
    }
  }

  deleteMilestone(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce jalon ?')) {
      this.milestoneService.delete(id).subscribe({
        next: () => {
          this.loadMilestones();
        },
        error: (err) => {
          console.error(err);
          alert('Erreur lors de la suppression');
        }
      });
    }
  }

  startMilestone(id: number): void {
    this.milestoneService.start(id).subscribe({
      next: () => this.loadMilestones(),
      error: (err) => console.error(err)
    });
  }

  submitMilestone(id: number): void {
    if (confirm('Voulez-vous soumettre ce jalon pour validation ?')) {
      this.milestoneService.submit(id).subscribe({
        next: () => this.loadMilestones(),
        error: (err) => console.error(err)
      });
    }
  }

  approveMilestone(id: number): void {
    this.milestoneService.approve(id).subscribe({
      next: () => this.loadMilestones(),
      error: (err) => console.error(err)
    });
  }

  rejectMilestone(id: number): void {
    this.selectedMilestoneId = id;
    this.rejectionReason = '';
    this.newDeadline = '';
    this.showRejectModal = true;
  }

  closeRejectModal(): void {
    this.showRejectModal = false;
    this.selectedMilestoneId = null;
  }

  confirmReject(): void {
    if (!this.rejectionReason.trim()) {
      alert('Un motif est obligatoire pour rejeter un jalon.');
      return;
    }

    if (this.selectedMilestoneId) {
      this.milestoneService.reject(this.selectedMilestoneId, this.rejectionReason, this.newDeadline || undefined).subscribe({
        next: () => {
          this.loadMilestones();
          this.closeRejectModal();
        },
        error: (err) => console.error(err)
      });
    }
  }

  resubmitMilestone(id: number): void {
    if (confirm('Voulez-vous soumettre à nouveau ce jalon ?')) {
      this.milestoneService.resubmit(id).subscribe({
        next: () => this.loadMilestones(),
        error: (err) => console.error(err)
      });
    }
  }

  getStatusClass(status: string): string {
    return `status-badge status-${status}`;
  }
}