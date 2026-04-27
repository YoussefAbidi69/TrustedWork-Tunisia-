import { Component, NgZone, AfterViewInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

declare const grecaptcha: any;

function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  if (!password || !confirmPassword) return null;
  return password === confirmPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements AfterViewInit {

  registerForm: FormGroup;
  submitted = false;
  loading = false;
  hidePassword = true;
  hideConfirmPassword = true;
  successMessage = '';
  errorMessage = '';

  captchaToken: string | null = null;
  captchaError = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone
  ) {
    this.registerForm = this.fb.group(
      {
        cin: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
        firstName: ['', [Validators.required, Validators.minLength(2)]],
        lastName: ['', [Validators.required, Validators.minLength(2)]],
        email: ['', [Validators.required, Validators.email]],
        phoneNumber: ['', [Validators.required, Validators.pattern(/^\d{8,15}$/)]],
        role: ['FREELANCER', Validators.required],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]],
        agreeTerms: [false, Validators.requiredTrue]
      },
      { validators: passwordMatchValidator }
    );

    (window as any)['onCaptchaSuccess'] = (token: string) => {
      this.ngZone.run(() => {
        this.captchaToken = token;
        this.captchaError = false;
      });
    };

    (window as any)['onCaptchaExpired'] = () => {
      this.ngZone.run(() => {
        this.captchaToken = null;
      });
    };
  }

  ngAfterViewInit(): void {
    this.renderCaptcha();
  }

  private renderCaptcha(): void {
    // Si grecaptcha est déjà prêt, on rend directement
    if (typeof grecaptcha !== 'undefined' && grecaptcha.render) {
      const container = document.getElementById('recaptcha-container');
      if (container && container.childElementCount === 0) {
        grecaptcha.render(container, {
          sitekey: '6LfHVqgsAAAAAIOKWTA9QvyQEMX0YMbvb7paevfW',
          callback: (token: string) => {
            this.ngZone.run(() => {
              this.captchaToken = token;
              this.captchaError = false;
            });
          },
          'expired-callback': () => {
            this.ngZone.run(() => {
              this.captchaToken = null;
            });
          }
        });
      }
    } else {
      // Script pas encore prêt, on réessaie dans 500ms
      setTimeout(() => this.renderCaptcha(), 500);
    }
  }

  get f() { return this.registerForm.controls; }

  get passwordValue(): string { return this.registerForm.get('password')?.value || ''; }
  get hasMinLength(): boolean { return this.passwordValue.length >= 8; }
  get hasUppercase(): boolean { return /[A-Z]/.test(this.passwordValue); }
  get hasLowercase(): boolean { return /[a-z]/.test(this.passwordValue); }
  get hasNumber(): boolean { return /\d/.test(this.passwordValue); }
  get hasSpecialChar(): boolean { return /[^A-Za-z0-9]/.test(this.passwordValue); }

  get passwordStrengthScore(): number {
    let score = 0;
    if (this.hasMinLength) score++;
    if (this.hasUppercase) score++;
    if (this.hasLowercase) score++;
    if (this.hasNumber) score++;
    if (this.hasSpecialChar) score++;
    return score;
  }

  get passwordStrengthLabel(): string {
    if (!this.passwordValue) return '';
    if (this.passwordStrengthScore <= 2) return 'Mot de passe faible';
    if (this.passwordStrengthScore <= 4) return 'Mot de passe moyen';
    return 'Mot de passe fort';
  }

  get passwordStrengthClass(): string {
    if (!this.passwordValue) return '';
    if (this.passwordStrengthScore <= 2) return 'weak';
    if (this.passwordStrengthScore <= 4) return 'medium';
    return 'strong';
  }

  get passwordStrengthWidth(): string {
    return `${(this.passwordStrengthScore / 5) * 100}%`;
  }

  onSubmit(): void {
    this.submitted = true;
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.captchaToken) {
      this.captchaError = true;
      return;
    }

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading = true;

    const payload = {
      cin: Number(this.registerForm.value.cin),
      firstName: this.registerForm.value.firstName,
      lastName: this.registerForm.value.lastName,
      email: this.registerForm.value.email,
      phoneNumber: this.registerForm.value.phoneNumber,
      role: this.registerForm.value.role,
      password: this.registerForm.value.password
    };

    this.authService.register(payload).subscribe({
      next: (_res: unknown) => {
        this.loading = false;
        this.successMessage = 'Compte créé avec succès. Vous pouvez maintenant vous connecter.';
        this.registerForm.reset({
          cin: '', firstName: '', lastName: '', email: '',
          phoneNumber: '', role: 'FREELANCER',
          password: '', confirmPassword: '', agreeTerms: false
        });
        if (typeof grecaptcha !== 'undefined') grecaptcha.reset();
        this.captchaToken = null;
        setTimeout(() => { this.router.navigate(['/auth/login']); }, 1200);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (typeof grecaptcha !== 'undefined') grecaptcha.reset();
        this.captchaToken = null;
        if (err.status === 400) {
          this.errorMessage = 'Données invalides ou utilisateur déjà existant.';
        } else if (err.status === 409) {
          this.errorMessage = 'Cet email ou CIN existe déjà.';
        } else {
          this.errorMessage = 'Erreur serveur. Réessayez plus tard.';
        }
        console.error('REGISTER ERROR', err);
      }
    });
  }
}