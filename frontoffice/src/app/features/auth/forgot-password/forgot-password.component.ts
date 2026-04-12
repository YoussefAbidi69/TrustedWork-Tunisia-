import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  forgotPasswordForm: FormGroup;
  submitted = false;
  loading = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  get f() {
    return this.forgotPasswordForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.successMessage = '';
    this.errorMessage = '';

    if (this.forgotPasswordForm.invalid) {
      return;
    }

    this.loading = true;

    this.authService.forgotPassword(this.forgotPasswordForm.value).subscribe({
      next: (response) => {
        this.loading = false;
        this.successMessage =
          response?.message || 'Un lien de réinitialisation a été envoyé à votre adresse email.';
        console.log('FORGOT PASSWORD RESPONSE', response);
      },
      error: (error: HttpErrorResponse) => {
        this.loading = false;
        this.errorMessage =
          error?.error?.message || 'Une erreur est survenue lors de l’envoi du lien.';
        console.error('FORGOT PASSWORD ERROR', error);
      }
    });
  }
}