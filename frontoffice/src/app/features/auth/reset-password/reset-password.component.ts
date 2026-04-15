import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

function resetPasswordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;

  if (!password || !confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent {
  resetPasswordForm: FormGroup;
  submitted = false;
  loading = false;
  hidePassword = true;
  hideConfirmPassword = true;
  successMessage = '';
  errorMessage = '';
  token = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private authService: AuthService
  ) {
    this.resetPasswordForm = this.fb.group(
      {
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]]
      },
      { validators: resetPasswordMatchValidator }
    );

    this.token = this.route.snapshot.queryParamMap.get('token') || '';
  }

  get f() {
    return this.resetPasswordForm.controls;
  }

  get passwordValue(): string {
    return this.resetPasswordForm.get('password')?.value || '';
  }

  get passwordStrength(): 'weak' | 'medium' | 'strong' | '' {
    const password = this.passwordValue;

    if (!password) {
      return '';
    }

    let score = 0;

    if (password.length >= 8) {
      score++;
    }

    if (/[A-Z]/.test(password)) {
      score++;
    }

    if (/[a-z]/.test(password)) {
      score++;
    }

    if (/\d/.test(password)) {
      score++;
    }

    if (/[^A-Za-z0-9]/.test(password)) {
      score++;
    }

    if (score <= 2) {
      return 'weak';
    }

    if (score <= 4) {
      return 'medium';
    }

    return 'strong';
  }

  get passwordStrengthLabel(): string {
    switch (this.passwordStrength) {
      case 'weak':
        return 'Faible';
      case 'medium':
        return 'Moyen';
      case 'strong':
        return 'Fort';
      default:
        return '';
    }
  }

  onSubmit(): void {
    this.submitted = true;
    this.successMessage = '';
    this.errorMessage = '';

    if (this.resetPasswordForm.invalid) {
      return;
    }

    if (!this.token) {
      this.errorMessage = 'Token de réinitialisation introuvable.';
      return;
    }

    this.loading = true;

    const payload = {
      token: this.token,
      newPassword: this.resetPasswordForm.value.password
    };

    console.log('RESET PASSWORD PAYLOAD', payload);
    console.log('RESET PASSWORD TOKEN FROM URL', this.token);

    this.authService.resetPassword(payload).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage =
          response?.message || 'Votre mot de passe a été réinitialisé avec succès.';
        console.log('RESET PASSWORD RESPONSE', response);
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage =
          error?.error?.message || 'Une erreur est survenue lors de la réinitialisation.';
        console.error('RESET PASSWORD ERROR', error);
      }
    });
  }
}