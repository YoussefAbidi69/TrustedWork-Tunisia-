import { Injectable, NgZone } from '@angular/core';
import { Observable, from } from 'rxjs';
import { ApiService } from './api.service';
import { AuthResponse } from '../models/auth.model';
import { AuthService } from './auth.service';

declare const google: any;

export interface GoogleCredentialResponse {
  credential: string;
  select_by: string;
}

@Injectable({
  providedIn: 'root'
})
export class GoogleOAuthService {

private readonly GOOGLE_CLIENT_ID = '770752272656-tqn8j0hotptmdn54pmltnd10e1sak057.apps.googleusercontent.com';
  constructor(
    private api: ApiService,
    private authService: AuthService,
    private ngZone: NgZone
  ) {}

  /**
   * Initialise Google Identity Services et rend le bouton dans le conteneur donné.
   * @param containerId  ID du div HTML qui recevra le bouton Google
   * @param onSuccess    Callback appelé avec l'AuthResponse après un login réussi
   * @param onError      Callback appelé en cas d'erreur
   */
  initGoogleButton(
    containerId: string,
    onSuccess: (response: AuthResponse) => void,
    onError: (error: any) => void
  ): void {
    if (typeof google === 'undefined') {
      console.error('Google Identity Services script not loaded');
      return;
    }

    google.accounts.id.initialize({
      client_id: this.GOOGLE_CLIENT_ID,
      callback: (response: GoogleCredentialResponse) => {
        // Le callback Google s'exécute hors de la zone Angular → NgZone.run()
        this.ngZone.run(() => {
          this.handleGoogleCredential(response.credential, onSuccess, onError);
        });
      }
    });

    google.accounts.id.renderButton(
      document.getElementById(containerId),
      {
        type: 'standard',
        shape: 'rectangular',
        theme: 'outline',
        text: 'signin_with',
        size: 'large',
        logo_alignment: 'left',
        width: '100%'
      }
    );
  }

  /**
   * Envoie le credential Google au backend et sauvegarde la session.
   */
  private handleGoogleCredential(
    credential: string,
    onSuccess: (response: AuthResponse) => void,
    onError: (error: any) => void
  ): void {
    this.api.post<AuthResponse>('/auth/google', { credential }).subscribe({
      next: (response: AuthResponse) => {
        // Réutilise la logique de session du AuthService existant
        this.authService['saveSession'](response, true);
        onSuccess(response);
      },
      error: (err) => {
        console.error('Google OAuth error:', err);
        onError(err);
      }
    });
  }

  /**
   * Révoque la session Google côté client (utile au logout).
   */
  revokeGoogleSession(email: string): void {
    if (typeof google !== 'undefined') {
      google.accounts.id.revoke(email, () => {
        console.log('Google session revoked');
      });
    }
  }
}