import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-complete-profile',
  templateUrl: './complete-profile.component.html',
  styleUrls: ['./complete-profile.component.css']
})
export class CompleteProfileComponent {

  form: FormGroup;
  submitted = false;
  loading = false;
  errorMessage = '';

  // Informations du compte Google (affichées en lecture seule)

googleUser: any;

constructor(
  private fb: FormBuilder,
  private userService: UserService,
  public authService: AuthService,
  private router: Router
) {
  this.form = this.fb.group({
    cin: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\d{8,15}$/)]],
    role: ['FREELANCER', Validators.required]
  });
}

ngOnInit(): void {
  this.googleUser = this.authService.getCurrentAuthUser();
}

  get f() {
    return this.form.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';

    if (this.form.invalid) {
      return;
    }

    this.loading = true;

    const payload = {
      cin: this.form.value.cin,
      phoneNumber: this.form.value.phoneNumber,
      role: this.form.value.role
    };

    this.userService.completeGoogleProfile(payload).subscribe({
      next: (res: AuthResponse) => {
        this.loading = false;

        // Mettre à jour la session avec les nouveaux tokens (rôle mis à jour)
        (this.authService as any)['saveSession'](res, true);

        this.router.navigate(['/app/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;

        if (err.status === 400) {
          const msg = err.error?.message || '';
          this.errorMessage = msg.includes('CIN')
            ? 'Ce numéro de CIN est déjà utilisé par un autre compte.'
            : 'Données invalides. Vérifiez les informations saisies.';
        } else if (err.status === 409) {
          this.errorMessage = 'Ce numéro de CIN est déjà enregistré.';
        } else {
          this.errorMessage = 'Erreur serveur. Réessayez plus tard.';
        }

        console.error('COMPLETE PROFILE ERROR', err);
      }
    });
  }

  getInitials(): string {
    if (!this.googleUser) return '?';
    const firstName = this.googleUser.email?.split('@')[0] ?? '?';
    return firstName.charAt(0).toUpperCase();
  }
}
