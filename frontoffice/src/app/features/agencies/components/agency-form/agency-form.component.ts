import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AgencyService } from '../../services/agency.service';
import { Agency, AgencyRequest } from '../../../../core/models/agency.model';
import { AuthService } from '../../../../core/services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-agency-form',
  templateUrl: './agency-form.component.html',
  styleUrls: ['./agency-form.component.css']
})
export class AgencyFormComponent implements OnInit {
  @Input() agency?: Agency;
  @Output() submitted = new EventEmitter<Agency>();
  @Output() cancelled = new EventEmitter<void>();

  agencyForm: FormGroup;
  isEditMode = false;
  saving = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  private currentUserId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private agencyService: AgencyService,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Form fields aligned with AgencyRequestDto:
    // name, description, city, country
    this.agencyForm = this.fb.group({
      name:        ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required]],
      city:        ['', [Validators.required]],
      country:     ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    // Retrieve logged-in user id from auth session
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.currentUserId = authUser.userId;
    } else {
      this.errorMessage = 'Vous devez être connecté pour créer une agence.';
    }

    if (this.agency) {
      this.isEditMode = true;
      this.agencyForm.patchValue({
        name:        this.agency.name,
        description: this.agency.description,
        city:        this.agency.city,
        country:     this.agency.country
      });
    } else {
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.isEditMode = true;
        this.loadAgency(+id);
      }
    }
  }

  loadAgency(id: number): void {
    this.agencyService.getAgencyById(id).subscribe({
      next: (agency) => {
        this.agency = agency;
        this.agencyForm.patchValue({
          name:        agency.name,
          description: agency.description,
          city:        agency.city,
          country:     agency.country
        });
      },
      error: (err) => {
        console.error('Erreur chargement agence:', err);
        this.errorMessage = 'Erreur lors du chargement des données de l\'agence.';
      }
    });
  }

  onSubmit(): void {
    this.errorMessage = null;
    this.successMessage = null;

    if (this.agencyForm.invalid) {
      this.agencyForm.markAllAsTouched();
      return;
    }

    if (!this.currentUserId) {
      this.errorMessage = 'Impossible de soumettre : utilisateur non authentifié.';
      return;
    }

    this.saving = true;
    const formValue = this.agencyForm.value;

    if (this.isEditMode && this.agency) {
      // BUILD update payload
      const updatePayload: Partial<Agency> = {
        name:        formValue.name,
        description: formValue.description,
        city:        formValue.city,
        country:     formValue.country
      };

      console.log('[AgencyForm] Updating agency payload:', updatePayload);

      this.agencyService.updateAgency(this.agency.id, updatePayload).subscribe({
        next: (updated) => {
          this.saving = false;
          this.successMessage = 'Agence mise à jour avec succès !';
          this.submitted.emit(updated);
          setTimeout(() => this.router.navigate(['/app/agencies', updated.id]), 1200);
        },
        error: (err) => {
          console.error('[AgencyForm] Update error:', err);
          this.saving = false;
          this.errorMessage = err?.error?.message || 'Erreur lors de la mise à jour de l\'agence.';
        }
      });

    } else {
      // BUILD creation payload matching AgencyRequestDto exactly
      const createPayload: AgencyRequest = {
        creatorId:   this.currentUserId,
        name:        formValue.name,
        description: formValue.description,
        city:        formValue.city,
        country:     formValue.country,
        active:      true
      };

      console.log('[AgencyForm] Creating agency payload:', createPayload);

      this.agencyService.createAgency(createPayload).subscribe({
        next: (created) => {
          this.saving = false;
          this.successMessage = 'Agence créée avec succès ! Redirection...';
          this.submitted.emit(created);
          // Redirect to the new agency dashboard
          setTimeout(() => this.router.navigate(['/app/agencies', created.id]), 1200);
        },
        error: (err) => {
          console.error('[AgencyForm] Create error:', err);
          this.saving = false;
          this.errorMessage = err?.error?.message
            || (err?.status === 401 ? 'Session expirée. Veuillez vous reconnecter.' : null)
            || 'Erreur lors de la création de l\'agence. Veuillez réessayer.';
        }
      });
    }
  }

  onCancel(): void {
    this.cancelled.emit();
    this.router.navigate(['/app/agencies']);
  }
}
