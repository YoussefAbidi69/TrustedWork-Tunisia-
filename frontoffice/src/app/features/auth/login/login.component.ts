import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { GoogleOAuthService } from '../../../core/services/google-oauth.service';
import { AuthResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  submitted = false;
  loading = false;
  hidePassword = true;
  successMessage = '';
  errorMessage = '';

  // URL du backoffice admin — à adapter selon l'environnement
  private readonly ADMIN_BACKOFFICE_URL = 'http://localhost:4201';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private googleOAuthService: GoogleOAuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      rememberMe: [true]
    });
  }

  ngOnInit(): void {
    setTimeout(() => {
      this.googleOAuthService.initGoogleButton(
        'google-signin-btn',
        (response: AuthResponse) => this.onGoogleSuccess(response),
        (error: any) => this.onGoogleError(error)
      );
    }, 0);
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.successMessage = '';
    this.errorMessage = '';

    if (this.loginForm.invalid) return;

    this.loading = true;

    const payload = {
      email: this.loginForm.value.email,
      password: this.loginForm.value.password
    };
    const rememberMe = this.loginForm.value.rememberMe;

    this.authService.login(payload, rememberMe).subscribe({
      next: (res: AuthResponse) => {
        this.loading = false;

        // Vérification 2FA en priorité
        if (res.twoFactorRequired) {
          this.successMessage = 'Code de vérification requis.';
          sessionStorage.setItem('2fa_email', payload.email);
          sessionStorage.setItem('remember_me', String(rememberMe));
          this.router.navigate(['/auth/2fa']);
          return;
        }

        // Redirection selon le rôle
        this.redirectByRole(res);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Email ou mot de passe incorrect.';
        } else if (err.status === 403) {
          this.errorMessage = 'Compte suspendu ou désactivé.';
        } else {
          this.errorMessage = 'Erreur serveur. Réessaye plus tard.';
        }
        console.error('LOGIN ERROR', err);
      }
    });
  }

  /**
   * Redirige l'utilisateur vers le bon dashboard selon son rôle.
   * - ADMIN  → backoffice (port 4201) via auto-login avec token
   * - autres → frontoffice dashboard (port 4200)
   */
  private redirectByRole(res: AuthResponse): void {
    const role = res.role?.toUpperCase();

    if (role === 'ADMIN') {
      // Transmission du token au backoffice via query param sécurisé
      const token = res.accessToken;
      const redirectUrl = `${this.ADMIN_BACKOFFICE_URL}/auth/auto-login?token=${encodeURIComponent(token)}&userId=${res.userId}&email=${encodeURIComponent(res.email)}&role=${role}`;
      window.location.href = redirectUrl;
    } else {
      // CLIENT, FREELANCER, etc. → dashboard frontoffice
      this.successMessage = 'Connexion réussie.';
      this.router.navigate(['/app/dashboard']);
    }
  }

  private onGoogleSuccess(response: AuthResponse): void {
    this.successMessage = 'Connexion Google réussie.';
    this.errorMessage = '';
    this.redirectByRole(response);
  }

  private onGoogleError(error: any): void {
    this.errorMessage = 'Échec de la connexion avec Google. Réessaye.';
    this.successMessage = '';
    console.error('Google OAuth error:', error);
  }
}