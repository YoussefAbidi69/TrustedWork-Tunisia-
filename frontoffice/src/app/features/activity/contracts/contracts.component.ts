import { Component, OnInit } from '@angular/core';
import { ContractService } from '../../../core/services/contract.service';
import { MilestoneService } from '../../../core/services/milestone.service';
import { Contract } from '../../../core/models/contract.model';
import { Milestone } from '../../../core/models/milestone.model';
import { AuthService } from '../../../core/services/auth.service';
import { FormBuilder, FormGroup } from '@angular/forms';

@Component({
  selector: 'app-contracts',
  templateUrl: './contracts.component.html',
  styleUrl: './contracts.component.css'
})
export class ContractsComponent implements OnInit {
  contracts: Contract[] = [];
  milestonesMap: { [contractId: number]: Milestone[] } = {};
  loadingMilestones: { [contractId: number]: boolean } = {};
  expandedContracts: { [contractId: number]: boolean } = {};
  loading = false;
  error = '';
  searchTerm: string = '';

  get filteredContracts(): Contract[] {
    if (!this.searchTerm.trim()) {
      return this.contracts;
    }
    const term = this.searchTerm.toLowerCase();
    return this.contracts.filter(c => 
      c.projectTitle?.toLowerCase().includes(term)
    );
  }

  // Submission Modal State
  showSubmitModal = false;
  submissionForm: FormGroup;
  submittingMilestoneId: number | null = null;
  submittingContractId: number | null = null;

  // Rejection Modal State
  showRejectModal = false;
  rejectionReason = '';
  newDeadline = '';
  selectedMilestoneId: number | null = null;
  selectedContractId: number | null = null;
  minDate = new Date().toISOString().split('T')[0];

  constructor(
    private contractService: ContractService, 
    private milestoneService: MilestoneService,
    public authService: AuthService,
    private fb: FormBuilder
  ) {
    this.submissionForm = this.fb.group({
      fichiers: [''],
      lienDemo: [''],
      repoGit: [''],
      commentaire: ['']
    });
  }

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
    this.loadContracts();
  }

  loadContracts(): void {
    this.loading = true;
    const request = this.isAdmin 
      ? this.contractService.getAll(0, 100)
      : this.contractService.getMyContracts();

    request.subscribe({
      next: (response: any) => {
        this.contracts = response.content || response;
        this.loading = false;
        this.contracts.forEach(contract => {
          if (contract.id) this.loadMilestonesForContract(contract.id);
        });
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des contrats';
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadMilestonesForContract(contractId: number): void {
    this.loadingMilestones[contractId] = true;
    this.milestoneService.getByContractId(contractId).subscribe({
      next: (milestones) => {
        this.milestonesMap[contractId] = milestones;
        this.loadingMilestones[contractId] = false;
      },
      error: (err) => {
        console.error(err);
        this.loadingMilestones[contractId] = false;
      }
    });
  }

  toggleMilestones(contractId: number): void {
    this.expandedContracts[contractId] = !this.expandedContracts[contractId];
  }

  startMilestone(contractId: number, milestoneId: number): void {
    if (confirm('Voulez-vous commencer ce jalon ?')) {
      this.milestoneService.start(milestoneId).subscribe({
        next: () => this.loadMilestonesForContract(contractId),
        error: (err) => console.error(err)
      });
    }
  }

  submitMilestone(contractId: number, milestoneId: number): void {
    this.submittingContractId = contractId;
    this.submittingMilestoneId = milestoneId;
    this.submissionForm.reset();
    this.showSubmitModal = true;
  }

  closeSubmitModal(): void {
    this.showSubmitModal = false;
    this.submittingMilestoneId = null;
    this.submittingContractId = null;
  }

  confirmSubmitWithProof(): void {
    if (this.submittingMilestoneId && this.submittingContractId) {
      const payload = this.submissionForm.value;
      this.milestoneService.submit(this.submittingMilestoneId, payload).subscribe({
        next: () => {
          this.loadMilestonesForContract(this.submittingContractId!);
          this.closeSubmitModal();
        },
        error: (err) => {
          console.error(err);
          alert('Erreur lors de la soumission : ' + (err.error?.message || 'Serveur indisponible'));
        }
      });
    }
  }

  approveMilestone(contractId: number, milestoneId: number): void {
    if (confirm('Voulez-vous approuver ce jalon ?')) {
      this.milestoneService.approve(milestoneId).subscribe({
        next: () => this.loadMilestonesForContract(contractId),
        error: (err) => console.error(err)
      });
    }
  }

  rejectMilestone(contractId: number, milestoneId: number): void {
    this.selectedContractId = contractId;
    this.selectedMilestoneId = milestoneId;
    this.rejectionReason = '';
    this.newDeadline = '';
    this.showRejectModal = true;
  }

  closeRejectModal(): void {
    this.showRejectModal = false;
    this.selectedMilestoneId = null;
    this.selectedContractId = null;
  }

  confirmReject(): void {
    if (!this.rejectionReason.trim()) {
      alert('Un motif est obligatoire pour rejeter un jalon.');
      return;
    }

    if (this.selectedMilestoneId && this.selectedContractId) {
      this.milestoneService.reject(this.selectedMilestoneId, this.rejectionReason, this.newDeadline || undefined).subscribe({
        next: () => {
          this.loadMilestonesForContract(this.selectedContractId!);
          this.closeRejectModal();
        },
        error: (err) => console.error(err)
      });
    }
  }

  deleteContract(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce contrat ?')) {
      this.contractService.delete(id).subscribe({
        next: () => {
          this.loadContracts();
        },
        error: (err) => {
          console.error(err);
          alert('Erreur lors de la suppression');
        }
      });
    }
  }

  getStatusClass(status: string): string {
    return `status-badge status-${status}`;
  }
}
