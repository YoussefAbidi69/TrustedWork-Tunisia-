import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { MilestoneService } from '../../../../core/services/milestone.service';
import { Milestone } from '../../../../core/models/milestone.model';
import { AuthService } from '../../../../core/services/auth.service';
import { ContractService } from '../../../../core/services/contract.service';
import { Contract } from '../../../../core/models/contract.model';
import { AIService } from '../../../../core/services/ai.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-milestone-form',
  templateUrl: './milestone-form.html',
  styleUrl: './milestone-form.css'
})
export class MilestoneFormComponent implements OnInit {
  milestone: Milestone = {
    contractId: 0,
    titre: '',
    description: '',
    montant: 0,
    deadline: '',
    status: 'PENDING'
  };
  
  isEditMode = false;
  loading = false;
  error = '';
  totalContractAmount = 0;
  remainingContractAmount = 0;
  otherMilestonesTotal = 0;
  minDateToday = new Date().toISOString().split('T')[0];

  // ─── IA ──────────────────────────────────────────────
  aiPrompt = '';
  generating = false;
  parentContract: Contract | null = null;
  existingMilestoneTitles: string[] = [];

  constructor(
    private milestoneService: MilestoneService,
    private contractService: ContractService,
    private router: Router,
    private route: ActivatedRoute,
    public authService: AuthService,
    private aiService: AIService
  ) {}

  get isFreelancer(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'FREELANCER';
  }

  get isClient(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'CLIENT';
  }

  get isAdmin(): boolean {
    return this.authService.getCurrentAuthUser()?.role === 'ADMIN';
  }

  ngOnInit(): void {
    const contractIdFromRoute = this.route.snapshot.params['id'];
    const milestoneIdFromRoute = this.route.snapshot.params['mId'];
    
    if (contractIdFromRoute) {
      this.milestone.contractId = +contractIdFromRoute;
    }
    
    if (milestoneIdFromRoute) {
      this.isEditMode = true;
      this.loadMilestone(+milestoneIdFromRoute);
    } else if (this.milestone.contractId) {
      this.loadContractInfo(this.milestone.contractId);
    }
  }

  loadContractInfo(contractId: number, currentMilestoneId?: number): void {
    this.loading = true;
    forkJoin({
      contract: this.contractService.getById(contractId),
      milestones: this.milestoneService.getByContractId(contractId)
    }).subscribe({
      next: (data) => {
        this.parentContract = data.contract;
        this.totalContractAmount = data.contract.montantTotal;
        this.otherMilestonesTotal = data.milestones
          .filter(m => m.id !== currentMilestoneId)
          .reduce((sum, m) => sum + (m.montant || 0), 0);
        
        this.existingMilestoneTitles = data.milestones
          .filter(m => m.id !== currentMilestoneId)
          .map(m => m.titre);

        this.calculateRemainder();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des informations du contrat';
        this.loading = false;
        console.error(err);
      }
    });
  }

  calculateRemainder(): void {
    this.remainingContractAmount = this.totalContractAmount - this.otherMilestonesTotal;
  }

  generateMilestoneWithAI(): void {
    if (!this.aiPrompt || this.aiPrompt.trim().length < 5 || !this.parentContract) return;
    this.generating = true;
    this.error = '';

    const context = {
      prompt: this.aiPrompt,
      contractTitle: this.parentContract.projectTitle || '',
      contractDescription: this.parentContract.description || '',
      remainingBudget: this.remainingContractAmount,
      contractDeadline: (this.parentContract.dateFin || '').toString(),
      existingMilestones: this.existingMilestoneTitles
    };

    this.aiService.generateMilestoneDraft(context).subscribe({
      next: (draft: any) => {
        this.milestone = { ...this.milestone, ...draft };
        
        // Sécurité: vérifier que le nouveau montant ne dépasse pas le reste à allouer
        if (this.milestone.montant > this.remainingContractAmount) {
             this.milestone.montant = this.remainingContractAmount;
             this.error = "L'IA a proposé un montant supérieur au budget restant. Il a été ajusté automatiquement au maximum possible.";
        }

        this.generating = false;
        this.aiPrompt = '';
      },
      error: (err: any) => {
        console.error('AI Generation error:', err);
        this.error = "Erreur de l'IA: " + (err.error?.message || "Impossible de générer le jalon.");
        this.generating = false;
      }
    });
  }

  loadMilestone(id: number): void {
    this.loading = true;
    this.milestoneService.getById(id).subscribe({
      next: (milestone) => {
        this.milestone = milestone;
        if (this.milestone.contractId) {
          this.loadContractInfo(this.milestone.contractId, this.milestone.id);
        } else {
          this.loading = false;
        }
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement du jalon';
        this.loading = false;
        console.error(err);
      }
    });
  }

  onSubmit(): void {
    if (this.milestone.montant > this.remainingContractAmount) {
      this.error = `Le montant du jalon (${this.milestone.montant} DT) ne peut pas dépasser le reste du contrat (${this.remainingContractAmount} DT).`;
      return;
    }

    this.loading = true;
    
    // Special case: after a client rejects a milestone, they can adjust only the deadline (backend blocks PUT).
    if (this.isEditMode && this.milestone.id && this.milestone.status === 'REJECTED') {
      if (!this.isClient && !this.isAdmin) {
        this.error = "Vous n'avez pas l'autorisation de modifier la deadline de ce jalon.";
        this.loading = false;
        return;
      }
      if (!this.milestone.deadline) {
        this.error = "La deadline est obligatoire.";
        this.loading = false;
        return;
      }

      this.milestoneService.updateRejectedDeadline(this.milestone.id, String(this.milestone.deadline || '')).subscribe({
        next: () => {
          this.router.navigate(['/app/activity/contracts', this.milestone.contractId]);
        },
        error: (err) => {
          this.error = 'Erreur lors de la modification de la deadline';
          this.loading = false;
          console.error('Update Deadline Error:', err);
        }
      });
      return;
    }

    // Nettoyage du payload pour s'assurer que les types correspondent au backend
    const payload: Milestone = {
      ...this.milestone,
      contractId: Number(this.milestone.contractId),
      montant: Number(this.milestone.montant),
      deadline: this.milestone.deadline || null
    };

    if (this.isEditMode) {
      this.milestoneService.update(this.milestone.id!, payload).subscribe({
        next: () => {
          this.router.navigate(['/app/activity/contracts', this.milestone.contractId]);
        },
        error: (err) => {
          this.error = 'Erreur lors de la modification (Vérifiez les données)';
          this.loading = false;
          console.error('Update Error:', err);
        }
      });
    } else {
      this.milestoneService.create(payload).subscribe({
        next: (milestone) => {
          this.router.navigate(['/app/activity/contracts', milestone.contractId]);
        },
        error: (err) => {
          this.error = 'Erreur lors de la création';
          this.loading = false;
          console.error('Create Error:', err);
        }
      });
    }
  }
}
