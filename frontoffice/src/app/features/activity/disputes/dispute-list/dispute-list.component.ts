import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DisputeService } from '../../../../core/services/dispute.service';
import { ContractService } from '../../../../core/services/contract.service';
import { Dispute } from '../../../../core/models/dispute.model';
import { Contract } from '../../../../core/models/contract.model';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-dispute-list',
  templateUrl: './dispute-list.component.html',
  styleUrl: './dispute-list.component.css'
})
export class DisputeListComponent implements OnInit {
  disputes: Dispute[] = [];
  contracts: Contract[] = [];
  loading = true;
  error = '';

  constructor(
    private disputeService: DisputeService,
    private contractService: ContractService,
    public authService: AuthService,
    private router: Router
  ) {}

  get isAdmin(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'ADMIN';
  }

  ngOnInit(): void {
    this.loadAllDisputes();
  }

  /**
   * 1. Charger tous les contrats de l'utilisateur
   * 2. Pour chaque contrat, charger les litiges
   * 3. Fusionner tout dans une seule liste
   */
  loadAllDisputes(): void {
    this.loading = true;
    this.error = '';
    this.disputes = [];

    // Step 1: Get all user contracts
    const contractsObs = this.isAdmin
      ? this.contractService.getAll(0, 100)
      : this.contractService.getMyContracts();

    contractsObs.subscribe({
      next: (response: any) => {
        this.contracts = response.content || response || [];

        if (this.contracts.length === 0) {
          this.loading = false;
          return;
        }

        // Step 2: For each contract, fetch disputes
        const disputeRequests = this.contracts
          .filter(c => c.id)
          .map(c =>
            this.disputeService.getByContractId(c.id!).pipe(
              catchError(() => of([]))  // If no disputes for this contract, return empty
            )
          );

        if (disputeRequests.length === 0) {
          this.loading = false;
          return;
        }

        forkJoin(disputeRequests).subscribe({
          next: (results: any[]) => {
            // Flatten all dispute arrays into one list
            this.disputes = results
              .flat()
              .filter((d: any) => d && d.id)
              .sort((a: any, b: any) => {
                // Sort by most recent first
                const dateA = new Date(a.openedAt || 0).getTime();
                const dateB = new Date(b.openedAt || 0).getTime();
                return dateB - dateA;
              });
            this.loading = false;
          },
          error: () => {
            this.error = 'Erreur lors du chargement des litiges.';
            this.loading = false;
          }
        });
      },
      error: (err) => {
        console.error('Erreur chargement contrats:', err);
        this.error = 'Erreur lors du chargement des contrats.';
        this.loading = false;
      }
    });
  }

  openDetail(disputeId: number): void {
    this.router.navigate(['/app/activity/disputes', disputeId]);
  }

  openCreate(): void {
    this.router.navigate(['/app/activity/disputes/new']);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'open';
      case 'RESPONDED': return 'responded';
      case 'UNDER_REVIEW': return 'review';
      case 'RESOLVED_CLIENT':
      case 'RESOLVED_FREELANCER':
      case 'SPLIT': return 'resolved';
      case 'DISMISSED': return 'dismissed';
      default: return 'pending';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'OPEN': return 'Ouvert';
      case 'RESPONDED': return 'Répondu';
      case 'UNDER_REVIEW': return 'En examen';
      case 'RESOLVED_CLIENT': return 'Résolu (Client)';
      case 'RESOLVED_FREELANCER': return 'Résolu (Freelancer)';
      case 'SPLIT': return 'Partagé';
      case 'DISMISSED': return 'Rejeté';
      default: return status;
    }
  }
}
