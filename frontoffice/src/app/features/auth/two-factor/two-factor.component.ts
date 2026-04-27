import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-two-factor',
  templateUrl: './two-factor.component.html',
  styleUrls: ['./two-factor.component.css']
})
export class TwoFactorComponent implements OnInit {
  code = '';
  loading = false;
  errorMessage = '';
  infoMessage = '';
  email = '';
  remainingMinutes = 10;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const pending = this.authService.getPendingTwoFactorState();

    if (!pending || !this.authService.isPendingTwoFactorValid(10)) {
      this.authService.clearPendingTwoFactorState();
      this.router.navigate(['/login']);
      return;
    }

    this.email = pending.email;

    const elapsed = Date.now() - pending.createdAt;
    const remainingMs = Math.max(0, 10 * 60 * 1000 - elapsed);
    this.remainingMinutes = Math.ceil(remainingMs / 60000);

    this.infoMessage =
      `Entrez le code généré par votre application d’authentification. ` +
      `Cette étape expire dans environ ${this.remainingMinutes} minute(s).`;
  }

  submit(): void {
    const normalizedCode = this.code.trim();

    if (!/^\d{6}$/.test(normalizedCode)) {
      this.errorMessage = 'Veuillez saisir un code OTP valide à 6 chiffres.';
      return;
    }

    const pending = this.authService.getPendingTwoFactorState();

    if (!pending) {
      this.errorMessage = 'Session 2FA expirée. Veuillez vous reconnecter.';
      return;
    }

    this.errorMessage = '';
    this.loading = true;

    this.authService.verifyTwoFactor(
      {
        email: pending.email,
        code: normalizedCode
      },
      pending.rememberMe
    )
    .pipe(finalize(() => (this.loading = false)))
    .subscribe({
      next: () => {
        this.router.navigate(['/app/dashboard']);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.resolveOtpError(error);
      }
    });
  }

  backToLogin(): void {
    this.authService.clearPendingTwoFactorState();
    this.router.navigate(['/login']);
  }

  private resolveOtpError(error: HttpErrorResponse): string {
    const backendMessage =
      error?.error?.message ||
      error?.error?.error ||
      error?.error?.details ||
      '';

    const normalized = String(backendMessage).toLowerCase();

    if (normalized.includes('expired') || normalized.includes('expir')) {
      return 'Le code OTP a expiré. Attendez le prochain code puis réessayez.';
    }

    if (
      normalized.includes('invalid') ||
      normalized.includes('incorrect') ||
      normalized.includes('invalide')
    ) {
      return 'Le code OTP saisi est invalide.';
    }

    return backendMessage || 'Vérification 2FA impossible. Veuillez réessayer.';
  }
}