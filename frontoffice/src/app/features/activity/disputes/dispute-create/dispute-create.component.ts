import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DisputeService } from '../../../../core/services/dispute.service';
import { MilestoneService } from '../../../../core/services/milestone.service';
import { ContractService } from '../../../../core/services/contract.service';
import { Milestone } from '../../../../core/models/milestone.model';
import { forkJoin, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

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
  selectedFiles: File[] = [];
  fileError = '';

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

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fileError = '';
    if (input.files) {
      const filesArr = Array.from(input.files);
      if (this.selectedFiles.length + filesArr.length > 2) {
        this.fileError = 'Vous ne pouvez uploader que 2 fichiers maximum.';
        return;
      }
      for (const file of filesArr) {
        if (file.size > 5 * 1024 * 1024) { // Limit 5MB
          this.fileError = `Le fichier ${file.name} dépasse 5 Mo.`;
          return;
        }
      }
      this.selectedFiles = [...this.selectedFiles, ...filesArr].slice(0, 2);
    }
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
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

    this.disputeService.create(payload).pipe(
      switchMap((dispute: any) => {
        if (this.selectedFiles.length === 0) {
          return of({ dispute, uploads: [] });
        }
        // Upload all selected files concurrently
        const uploadObs = this.selectedFiles.map(file => this.disputeService.uploadEvidence(dispute.id!, file));
        return forkJoin(uploadObs).pipe(
          switchMap(uploads => of({ dispute, uploads }))
        );
      })
    ).subscribe({
      next: (result) => {
        this.loading = false;
        this.router.navigate(['/app/activity/disputes', result.dispute.id]);
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
          this.error = err.error?.message || 'Erreur lors de la création du litige ou upload des fichiers.';
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
