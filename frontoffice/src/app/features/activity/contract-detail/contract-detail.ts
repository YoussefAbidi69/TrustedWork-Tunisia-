import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink, RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { ContractService } from '../../../core/services/contract.service';
import { MilestoneService } from '../../../core/services/milestone.service';
import { ContractSignatureService } from '../../../core/services/contract-signature.service';
import { Contract } from '../../../core/models/contract.model';
import { Milestone } from '../../../core/models/milestone.model';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { DeliveryProofResponse } from '../../../core/models/delivery-proof.model';

@Component({
  selector: 'app-contract-detail',
  standalone: true,
  imports: [
    CommonModule, 
    RouterLink, 
    RouterModule, 
    FormsModule, 
    ReactiveFormsModule
  ],
  templateUrl: './contract-detail.html',
  styleUrl: './contract-detail.css'
})
export class ContractDetailComponent implements OnInit {
  contract: Contract | null = null;
  milestones: Milestone[] = [];
  loading = false;
  error = '';

  // Rejection Modal State
  showRejectModal = false;
  rejectionReason = '';
  newDeadline = '';
  selectedMilestoneId: number | null = null;
  minDate = new Date().toISOString().split('T')[0];

  // Submission Modal State (with Proof)
  showSubmitModal = false;
  submissionForm: FormGroup;
  submittingMilestoneId: number | null = null;
  
  // Storage for proofs
  deliveryProofs: {[key: number]: DeliveryProofResponse | null} = {};
  proofLoading: {[key: number]: boolean} = {};

  constructor(
    private route: ActivatedRoute,
    private contractService: ContractService,
    private milestoneService: MilestoneService,
    private signatureService: ContractSignatureService,
    public authService: AuthService,
    private router: Router,
    private fb: FormBuilder
  ) {
    this.submissionForm = this.fb.group({
      fichiers: [''],
      lienDemo: [''],
      repoGit: [''],
      commentaire: [''],
      hashMD5: [''],
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
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.loadContract(id);
      this.loadMilestones(id);
    }
  }

  loadContract(id: number): void {
    this.loading = true;
    this.contractService.getById(id).subscribe({
      next: (contract) => {
        this.contract = contract;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement du contrat';
        this.loading = false;
        console.error('Error loading contract:', err);
      }
    });
  }

  loadMilestones(contractId: number): void {
    this.milestoneService.getByContractId(contractId).subscribe({
      next: (milestones) => {
        this.milestones = milestones;
        // Load proofs for submitted/approved/rejected milestones
        this.milestones.forEach(m => {
          if (['SUBMITTED', 'APPROVED', 'REJECTED'].includes(m.status)) {
            this.loadDeliveryProof(m.id!);
          }
        });
      },
      error: (err) => {
        console.error('Erreur chargement jalons:', err);
      }
    });
  }

  loadDeliveryProof(milestoneId: number): void {
    this.proofLoading[milestoneId] = true;
    this.milestoneService.getDeliveryProof(milestoneId).subscribe({
      next: (proof) => {
        this.deliveryProofs[milestoneId] = proof;
        this.proofLoading[milestoneId] = false;
      },
      error: () => {
        this.deliveryProofs[milestoneId] = null;
        this.proofLoading[milestoneId] = false;
      }
    });
  }

  getStatusClass(status: string): string {
    return `status-badge status-${status}`;
  }

  getTotalMilestonesAmount(): number {
    return this.milestones.reduce((sum, m) => sum + m.montant, 0);
  }

  getReleasedAmount(): number {
    return this.milestones
      .filter(m => m.status === 'APPROVED' || m.status === 'AUTO_APPROVED')
      .reduce((sum, m) => sum + m.montant, 0);
  }

  isPaymentReady(): boolean {
    return !!this.contract && this.contract.status === 'PENDING_PAYMENT';
  }

  isDraftReady(): boolean {
    if (!this.contract || this.contract.status !== 'DRAFT') return false;
    return this.getTotalMilestonesAmount() === this.contract.montantTotal;
  }

  finalizeAndSend(): void {
    if (!this.contract?.id) return;
    
    console.log('--- Finalizing Contract ---');
    console.log('ID:', this.contract.id);
    console.log('Status:', this.contract.status);
    console.log('isDraftReady:', this.isDraftReady());
    console.log('Total Milestones:', this.getTotalMilestonesAmount());
    console.log('Contract Total:', this.contract.montantTotal);

    this.loading = true;
    this.signatureService.finalizeContract(this.contract.id).subscribe({
      next: () => {
        this.signatureService.sendSignatureRequests(this.contract!.id!).subscribe({
          next: () => {
            alert('Contrat finalisé et demandes de signature envoyées par email !');
            this.loadContract(this.contract!.id!);
          },
          error: (err) => {
            this.error = err.error?.message || "Erreur lors de l'envoi des invitations.";
            this.loading = false;
          }
        });
      },
      error: (err) => {
        console.error('--- FULL ERROR OBJECT ---', err);
        const msg = err.error?.message || err.error || "Erreur lors de la finalisation du contrat.";
        this.error = msg;
        this.loading = false;
        alert("Erreur de finalisation : " + (typeof msg === 'string' ? msg : JSON.stringify(msg)));
      }
    });
  }

  goToPayment(): void {
    if (this.contract) {
      this.router.navigate(['/app/finance/checkout', this.contract.id]);
    }
  }

  deleteMilestone(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce jalon ?')) {
      this.milestoneService.delete(id).subscribe({
        next: () => this.loadMilestones(this.contract!.id!),
        error: (err) => alert('Erreur lors de la suppression')
      });
    }
  }

  startMilestone(id: number): void {
    this.milestoneService.start(id).subscribe({
      next: () => this.loadMilestones(this.contract!.id!),
      error: (err) => console.error(err)
    });
  }

  submitMilestone(id: number): void {
    this.submittingMilestoneId = id;
    this.submissionForm.reset();
    this.showSubmitModal = true;
  }

  closeSubmitModal(): void {
    this.showSubmitModal = false;
    this.submittingMilestoneId = null;
  }

  confirmSubmitWithProof(): void {
    if (this.submittingMilestoneId) {
      const payload = this.submissionForm.value;
      this.milestoneService.submit(this.submittingMilestoneId, payload).subscribe({
        next: () => {
          this.loadMilestones(this.contract!.id!);
          this.closeSubmitModal();
        },
        error: (err) => {
          this.error = err?.error?.message ?? 'Submit failed';
          console.error(err);
        }
      });
    }
  }

  approveMilestone(id: number): void {
    this.milestoneService.approve(id).subscribe({
      next: () => this.loadMilestones(this.contract!.id!),
      error: (err) => {
        if (err.status === 400 && err.error?.message?.includes('proof')) {
          alert("L'approbation nécessite une preuve de livraison (Delivery Proof). Veuillez demander au freelancer de la soumettre.");
        } else {
          console.error(err);
          alert("Erreur lors de l'approbation: " + (err.error?.message || 'Inconnue'));
        }
      }
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
          this.loadMilestones(this.contract!.id!);
          this.closeRejectModal();
        },
        error: (err) => console.error(err)
      });
    }
  }

  resubmitMilestone(id: number): void {
    if (confirm('Voulez-vous soumettre à nouveau ce jalon ?')) {
      this.milestoneService.resubmit(id).subscribe({
        next: () => this.loadMilestones(this.contract!.id!),
        error: (err) => console.error(err)
      });
    }
  }

  formatUrl(url: any): string {
    if (!url) return '';
    const trimmed = String(url).trim();
    if (!trimmed.startsWith('http')) return 'https://' + trimmed;
    return trimmed;
  }
  downloadContractPDF(): void {
    if (!this.contract || !this.contract.id) return;
    const filename = `Contrat_${this.contract.reference || this.contract.id}.pdf`;
    
    this.signatureService.downloadPdf(this.contract.id).subscribe({
      next: (blob: Blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: (err: any) => {
        alert("Erreur lors de la génération du document PDF.");
        console.error('PDF error:', err);
      }
    });
  }
}