import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DisputeService } from '../../../../core/services/dispute.service';
import { MilestoneService } from '../../../../core/services/milestone.service';
import { ContractService } from '../../../../core/services/contract.service';
import { Milestone } from '../../../../core/models/milestone.model';

@Component({
  selector: 'app-dispute-create',
  templateUrl: './dispute-create.component.html',
  styleUrl: './dispute-create.component.css'
})
export class DisputeCreateComponent implements OnInit {
  form: FormGroup;
  milestones: Milestone[] = [];
  contractId: number | null = null;
  loading = false;
  loadingMilestones = false;
  error = '';
  useContractLevel = false;
  isContractLocked = false;

  constructor(
    private fb: FormBuilder,
    private disputeService: DisputeService,
    private milestoneService: MilestoneService,
    private contractService: ContractService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      contractReference: ['', Validators.required],
      milestoneId: [null],
      motif: ['', [Validators.required, Validators.minLength(10)]],
      preuvesPlaignant: ['']
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['contractId']) {
        this.contractId = +params['contractId'];
        this.isContractLocked = true;
        this.contractService.getById(this.contractId).subscribe({
          next: (c) => {
            this.form.patchValue({ contractReference: c.reference });
            this.loadMilestones(this.contractId!);
          }
        });
      }
    });
  }

  onContractReferenceChange(): void {
    const ref = this.form.get('contractReference')?.value;
    if (ref) {
      this.contractService.getByReference(ref).subscribe({
        next: (contract) => {
          if (contract) {
            this.contractId = contract.id!;
            this.loadMilestones(this.contractId);
            this.error = '';
          } else {
            this.contractId = null;
            this.milestones = [];
            this.error = 'Contrat introuvable avec cette référence.';
          }
        },
        error: () => {
          this.contractId = null;
          this.milestones = [];
          this.error = 'Erreur lors de la recherche du contrat.';
        }
      });
    }
  }

  loadMilestones(contractId: number): void {
    this.loadingMilestones = true;
    this.milestoneService.getByContractId(contractId).subscribe({
      next: (milestones) => {
        this.milestones = milestones;
        this.loadingMilestones = false;
      },
      error: () => {
        this.milestones = [];
        this.loadingMilestones = false;
      }
    });
  }

  toggleContractLevel(): void {
    this.useContractLevel = !this.useContractLevel;
    if (this.useContractLevel) {
      this.form.patchValue({ milestoneId: null });
    }
  }

  submit(): void {
    if (this.form.invalid) return;

    this.loading = true;
    this.error = '';

    const payload = {
      ...this.form.value,
      contractId: this.contractId,
      milestoneId: this.useContractLevel ? null : (this.form.value.milestoneId || null)
    };
    delete (payload as any).contractReference;

    this.disputeService.create(payload).subscribe({
      next: (dispute) => {
        this.loading = false;
        this.router.navigate(['/app/activity/disputes', dispute.id]);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 409) {
          this.error = 'Un litige existe déjà pour ce contrat/jalon. Vous ne pouvez pas créer un doublon.';
        } else if (err.status === 400) {
          this.error = err.error?.message || 'Données invalides. Vérifiez le formulaire.';
        } else if (err.status === 403) {
          this.error = 'Vous n\'êtes pas autorisé à créer un litige sur ce contrat.';
        } else {
          this.error = err.error?.message || 'Erreur lors de la création du litige.';
        }
        console.error(err);
      }
    });
  }

  cancel(): void {
    if (this.contractId) {
      this.router.navigate(['/app/activity/disputes'], { queryParams: { contractId: this.contractId } });
    } else {
      this.router.navigate(['/app/activity/disputes']);
    }
  }
}
