import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { AuthResponse, AuthUser, LoginPayload } from '../models/auth.model';

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export interface VerifyTwoFactorPayload {
  email: string;
  code: string;
}

export interface PendingTwoFactorState {
  email: string;
  rememberMe: boolean;
  createdAt: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly AUTH_USER_KEY = 'auth_user';

  private readonly TWO_FACTOR_EMAIL_KEY = '2fa_email';
  private readonly TWO_FACTOR_REMEMBER_ME_KEY = 'remember_me';
  private readonly TWO_FACTOR_CREATED_AT_KEY = '2fa_created_at';

  constructor(private api: ApiService) {}

  login(payload: LoginPayload, rememberMe: boolean = true): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', payload).pipe(
      tap((response: AuthResponse) => {
        if (response.twoFactorRequired) {
          this.savePendingTwoFactorState(payload.email, rememberMe);
        } else {
          this.clearPendingTwoFactorState();
          this.saveSession(response, rememberMe);
        }
      })
    );
  }

  verifyTwoFactor(payload: VerifyTwoFactorPayload, rememberMe?: boolean): Observable<AuthResponse> {
    const resolvedRememberMe =
      rememberMe ?? this.getPendingTwoFactorState()?.rememberMe ?? true;

    return this.api.post<AuthResponse>('/auth/verify-2fa', payload).pipe(
      tap((response: AuthResponse) => {
        this.saveSession(response, resolvedRememberMe);
        this.clearPendingTwoFactorState();
      })
    );
  }

  register(payload: unknown): Observable<unknown> {
    return this.api.post('/auth/register', payload);
  }

  forgotPassword(payload: ForgotPasswordPayload): Observable<any> {
    return this.api.post('/auth/forgot-password', payload);
  }

  resetPassword(payload: ResetPasswordPayload): Observable<any> {
    return this.api.post('/auth/reset-password', payload);
  }

  logout(): void {
    this.clearSession();
    this.clearPendingTwoFactorState();
  }

  isLoggedIn(): boolean {
    return !!this.getAccessToken();
  }

  getAccessToken(): string | null {
    return (
      localStorage.getItem(this.ACCESS_TOKEN_KEY) ||
      sessionStorage.getItem(this.ACCESS_TOKEN_KEY)
    );
  }

  getRefreshToken(): string | null {
    return (
      localStorage.getItem(this.REFRESH_TOKEN_KEY) ||
      sessionStorage.getItem(this.REFRESH_TOKEN_KEY)
    );
  }

  getCurrentAuthUser(): AuthUser | null {
    const rawUser =
      localStorage.getItem(this.AUTH_USER_KEY) ||
      sessionStorage.getItem(this.AUTH_USER_KEY);

    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as AuthUser;
    } catch (error) {
      console.error('Erreur lors de la lecture du user connecté :', error);
      return null;
    }
  }

  getUserInitials(): string {
    const user = this.getCurrentAuthUser();

    if (!user || !user.email) {
      return '?';
    }

    return user.email.charAt(0).toUpperCase();
  }

  savePendingTwoFactorState(email: string, rememberMe: boolean): void {
    sessionStorage.setItem(this.TWO_FACTOR_EMAIL_KEY, email);
    sessionStorage.setItem(this.TWO_FACTOR_REMEMBER_ME_KEY, String(rememberMe));
    sessionStorage.setItem(this.TWO_FACTOR_CREATED_AT_KEY, String(Date.now()));
  }

  getPendingTwoFactorState(): PendingTwoFactorState | null {
    const email = sessionStorage.getItem(this.TWO_FACTOR_EMAIL_KEY);
    const rememberMe = sessionStorage.getItem(this.TWO_FACTOR_REMEMBER_ME_KEY);
    const createdAt = sessionStorage.getItem(this.TWO_FACTOR_CREATED_AT_KEY);

    if (!email || !rememberMe || !createdAt) {
      return null;
    }

    return {
      email,
      rememberMe: rememberMe === 'true',
      createdAt: Number(createdAt)
    };
  }

  clearPendingTwoFactorState(): void {
    sessionStorage.removeItem(this.TWO_FACTOR_EMAIL_KEY);
    sessionStorage.removeItem(this.TWO_FACTOR_REMEMBER_ME_KEY);
    sessionStorage.removeItem(this.TWO_FACTOR_CREATED_AT_KEY);
  }

  isPendingTwoFactorValid(maxMinutes: number = 10): boolean {
    const pending = this.getPendingTwoFactorState();

    if (!pending) {
      return false;
    }

    const elapsed = Date.now() - pending.createdAt;
    return elapsed <= maxMinutes * 60 * 1000;
  }

  private saveSession(response: AuthResponse, rememberMe: boolean): void {
    this.clearSession();

    const storage = rememberMe ? localStorage : sessionStorage;

    const authUser: AuthUser = {
      userId: response.userId,
      email: response.email,
      role: response.role
    };

    if (response.accessToken) {
      storage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
    }

    if (response.refreshToken) {
      storage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
    }

    storage.setItem(this.AUTH_USER_KEY, JSON.stringify(authUser));
  }

  private clearSession(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.AUTH_USER_KEY);

    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(this.AUTH_USER_KEY);
  }
}