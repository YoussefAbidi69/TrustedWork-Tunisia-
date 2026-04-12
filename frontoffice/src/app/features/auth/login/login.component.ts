import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { GoogleOAuthService } from '../../../core/services/google-oauth.service';
import { FreelancerProfileService } from '../../../core/services/freelancer-profile.service';
import { AuthResponse } from '../../../core/models/auth.model';
import { FreelancerProfile } from '../../../core/models/freelancer.model';

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

  private readonly ADMIN_BACKOFFICE_URL = 'http://localhost:4201';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private googleOAuthService: GoogleOAuthService,
    private freelancerService: FreelancerProfileService,
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

        if (res.twoFactorRequired) {
          this.successMessage = 'Code de vérification requis.';
          sessionStorage.setItem('2fa_email', payload.email);
          sessionStorage.setItem('remember_me', String(rememberMe));
          this.router.navigate(['/auth/2fa']);
          return;
        }

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
      }
    });
  }

  /**
   * Redirige selon le rôle.
   * Pour FREELANCER : vérifie si le profil Module 02 existe.
   * S'il n'existe pas → création automatique (lazy creation).
   * Couplage faible : si Module 02 est down, on redirige quand même.
   */
  private redirectByRole(res: AuthResponse): void {
    const role = res.role?.toUpperCase();

    if (role === 'ADMIN') {
      const token = res.accessToken;
      const redirectUrl = `${this.ADMIN_BACKOFFICE_URL}/auth/auto-login?token=${encodeURIComponent(token)}&userId=${res.userId}&email=${encodeURIComponent(res.email)}&role=${role}`;
      window.location.href = redirectUrl;
      return;
    }

    if (role === 'FREELANCER' && res.userId) {
      // Attendre que le token soit bien sauvegardé en storage
      // avant d'appeler le freelancer-service
      setTimeout(() => {
        this.ensureFreelancerProfile(res.userId!);
      }, 300);
    } else {
      this.successMessage = 'Connexion réussie.';
      this.router.navigate(['/app/dashboard']);
    }
  }

  /**
   * Vérifie si le profil freelancer existe dans Module 02.
   * Pattern lazy creation : créé automatiquement au premier login.
   */
   private ensureFreelancerProfile(userId: number): void {
    this.freelancerService.getProfileByUserId(userId).subscribe({
      next: () => {
        // Profil existe → dashboard directement
        this.successMessage = 'Connexion réussie.';
        window.location.href = '/app/dashboard';
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          // Premier login → rediriger vers création de profil
          window.location.href = '/app/profile/create';
        } else {
          // Module 02 down → fail open
          window.location.href = '/app/dashboard';
        }
      }
    });
  }

  
 

  private onGoogleSuccess(response: AuthResponse): void {
    this.successMessage = 'Connexion Google réussie.';
    this.errorMessage = '';
    this.redirectByRole(response);
  }

  private onGoogleError(error: any): void {
    this.errorMessage = 'Échec de la connexion avec Google. Réessaye.';
    this.successMessage = '';
  }
}