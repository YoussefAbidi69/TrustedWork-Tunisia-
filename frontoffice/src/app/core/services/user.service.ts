import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ConnectedUserResponse, DashboardUser } from '../models/user.model';

export interface UserProfileResponse extends ConnectedUserResponse {
  twoFactorEnabled?: boolean;
}

export interface SetupTwoFactorResponse {
  qrCodeUri?: string;
  otpauthUrl?: string;
  otpauthUri?: string;
  secret?: string;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private api: ApiService) {}

  getCurrentUser(): Observable<ConnectedUserResponse> {
    return this.api.get<ConnectedUserResponse>('/users/me', undefined, this.api.getAuthUrl());
  }

  getMyProfile(): Observable<UserProfileResponse> {
    return this.api.get<UserProfileResponse>('/users/me', undefined, this.api.getAuthUrl());
  }

  getCurrentDashboardUser(): Observable<DashboardUser> {
    return this.getCurrentUser().pipe(
      map((user: ConnectedUserResponse) => this.mapToDashboardUser(user))
    );
  }

  setupTwoFactor(cin: number | string): Observable<SetupTwoFactorResponse> {
    return this.api.post<SetupTwoFactorResponse>(
      `/auth/setup-2fa/${encodeURIComponent(String(cin))}`,
      {},
      undefined,
      this.api.getAuthUrl()
    );
  }

  confirmTwoFactor(cin: number | string, code: string): Observable<any> {
    return this.api.post(
      `/auth/confirm-2fa/${encodeURIComponent(String(cin))}`,
      { code },
      undefined,
      this.api.getAuthUrl()
    );
  }

  disableTwoFactor(cin: number | string): Observable<any> {
    return this.api.post(
      `/auth/disable-2fa/${encodeURIComponent(String(cin))}`,
      {},
      undefined,
      this.api.getAuthUrl()
    );
  }

  mapToDashboardUser(user: ConnectedUserResponse | null | undefined): DashboardUser {
    if (!user) {
      return {
        id: null,
        fullName: 'Utilisateur',
        firstName: 'Utilisateur',
        lastName: '',
        email: '',
        role: ''
      };
    }

    const firstName =
      user.firstName ||
      user.firstname ||
      user.prenom ||
      '';

    const lastName =
      user.lastName ||
      user.lastname ||
      user.nom ||
      '';

    const fullName =
      user.fullName ||
      `${firstName} ${lastName}`.trim() ||
      firstName ||
      user.email ||
      'Utilisateur';

    return {
      id: user.id ?? user.userId ?? null,
      fullName,
      firstName: firstName || fullName,
      lastName,
      email: user.email || '',
      role: user.role || '',
      cin: user.cin
    };
  }

  checkProfileComplete(): Observable<{ incomplete: boolean }> {
    return this.getMyProfile().pipe(
      map(profile => {
        return { incomplete: false }; // or appropriate logic
      })
    );
  }

  completeGoogleProfile(payload: any): Observable<any> {
    return this.api.post('/auth/complete-google-profile', payload, undefined, this.api.getAuthUrl());
  }
}