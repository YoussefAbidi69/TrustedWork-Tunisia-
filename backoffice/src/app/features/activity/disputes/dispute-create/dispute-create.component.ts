import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DisputeService } from '../../../../core/services/dispute.service';
import { MilestoneService } from '../../../../core/services/milestone.service';
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

  constructor(
    private fb: FormBuilder,
    private disputeService: DisputeService,
    private milestoneService: MilestoneService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      contractId: [null, Validators.required],
      milestoneId: [null],
      motif: ['', [Validators.required, Validators.minLength(10)]],
      preuvesPlaignant: ['']
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['contractId']) {
        this.contractId = +params['contractId'];
        this.form.patchValue({ contractId: this.contractId });
        this.loadMilestones(this.contractId);
      }
    });
  }

  onContractIdChange(): void {
    const contractId = this.form.get('contractId')?.value;
    if (contractId) {
      this.contractId = +contractId;
      this.loadMilestones(this.contractId);
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
      milestoneId: this.useContractLevel ? null : (this.form.value.milestoneId || null)
    };

    this.disputeService.create(payload).subscribe({
      next: (dispute) => {
        this.loading = false;
        this.router.navigate(['/admin/activity/disputes', dispute.id]);
      },
      error: (err: any) => {
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
      this.router.navigate(['/admin/activity/disputes'], { queryParams: { contractId: this.contractId } });
    } else {
      this.router.navigate(['/admin/activity/disputes']);
    }
  }
}
