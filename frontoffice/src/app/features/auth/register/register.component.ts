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

  // Tracks how many times we retried loading the captcha
  private captchaRetries = 0;
  private readonly MAX_CAPTCHA_RETRIES = 10;

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
      this.captchaRetries++;
      if (this.captchaRetries < this.MAX_CAPTCHA_RETRIES) {
        setTimeout(() => this.renderCaptcha(), 500);
      } else {
        // reCAPTCHA failed to load after max retries (ad-blocker, network, dev env).
        // Mark as bypassed so the form is not permanently blocked.
        console.warn('[Register] reCAPTCHA failed to load after', this.MAX_CAPTCHA_RETRIES, 'retries. Bypassing for this session.');
        this.captchaToken = 'BYPASSED_DEV';
      }
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
      console.warn('[Register] Blocked: CAPTCHA not resolved yet.');
      return;
    }

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      console.warn('[Register] Blocked: form invalid');
      console.log('--- FORM DEBUG ---');
      console.log('Form Status:', this.registerForm.status);
      console.log('Form cross-field errors:', this.registerForm.errors);
      
      const controls = this.registerForm.controls;
      Object.keys(controls).forEach(key => {
        const control = controls[key];
        if (control.invalid) {
          console.error(`Field [${key}] is INVALID. Errors:`, control.errors, 'Value:', control.value);
        }
      });
      console.log('------------------');
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

    console.log('[Register] Submitting payload:', { ...payload, password: '***' });

    this.authService.register(payload).subscribe({
      next: (_res: unknown) => {
        this.loading = false;
        console.log('[Register] SUCCESS');
        this.successMessage = 'Compte créé avec succès. Redirection vers la connexion...';
        this.registerForm.reset({
          cin: '', firstName: '', lastName: '', email: '',
          phoneNumber: '', role: 'FREELANCER',
          password: '', confirmPassword: '', agreeTerms: false
        });
        this.captchaToken = null;
        this.captchaRetries = 0;
        if (typeof grecaptcha !== 'undefined') grecaptcha.reset();
        setTimeout(() => { this.router.navigate(['/auth/login']); }, 1500);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.captchaToken = null;
        this.captchaRetries = 0;
        if (typeof grecaptcha !== 'undefined') grecaptcha.reset();

        console.error('[Register] ERROR', err.status, err.error);

        // Backend field-level validation errors (400 with fieldErrors map)
        if (err.status === 400 && err.error?.fieldErrors) {
          const fields = err.error.fieldErrors as Record<string, string>;
          const messages = Object.entries(fields)
            .map(([field, msg]) => `${field}: ${msg}`)
            .join(' | ');
          this.errorMessage = `Données invalides — ${messages}`;
        } else if (err.status === 400) {
          this.errorMessage = err.error?.message || 'Données invalides ou utilisateur déjà existant.';
        } else if (err.status === 409) {
          this.errorMessage = err.error?.message || 'Cet email ou CIN existe déjà.';
        } else if (err.status === 401) {
          this.errorMessage = 'Session expirée. Veuillez rafraîchir la page.';
        } else if (err.status === 0) {
          this.errorMessage = 'Impossible de joindre le serveur. Vérifiez votre connexion.';
        } else {
          this.errorMessage = err.error?.message || 'Erreur serveur. Réessayez plus tard.';
        }

        // Re-render captcha for next attempt
        setTimeout(() => this.renderCaptcha(), 300);
      }
    });
  }
}