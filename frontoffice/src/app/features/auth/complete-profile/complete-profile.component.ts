import { Component, OnInit } from '@angular/core';
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
export class CompleteProfileComponent implements OnInit {
  form: FormGroup;
  submitted = false;
  loading = false;
  errorMessage = '';
  googleUser: any;

  constructor(
    private readonly fb: FormBuilder,
    private readonly userService: UserService,
    public readonly authService: AuthService,
    private readonly router: Router
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

  get selectedRole(): string {
    return this.form.get('role')?.value;
  }

  get displayName(): string {
    if (!this.googleUser) return 'Utilisateur';
    return (
      this.googleUser.fullName ||
      this.googleUser.name ||
      this.googleUser.email?.split('@')[0] ||
      'Utilisateur'
    );
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';

    if (this.form.invalid) {
      this.form.markAllAsTouched();
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

    const fullName = this.googleUser.fullName || this.googleUser.name;
    if (fullName) {
      const parts = fullName.trim().split(' ').filter((p: string) => !!p);
      if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase();
      }
      return parts[0][0].toUpperCase();
    }

    const firstName = this.googleUser.email?.split('@')[0] ?? '?';
    return firstName.charAt(0).toUpperCase();
  }
}