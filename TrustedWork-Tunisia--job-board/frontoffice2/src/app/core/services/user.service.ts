import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from './api.service';
import { ConnectedUserResponse, DashboardUser } from '../models/user.model';

export interface UserProfileResponse extends ConnectedUserResponse {
  phone?: string;
  photo?: string;
  headline?: string;
  location?: string;
  bio?: string;
  kycStatus?: string;
  twoFactorEnabled?: boolean;
  trustLevel?: number;
  livenessPassed?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SetupTwoFactorResponse {
  qrCodeUri?: string;
  otpauthUrl?: string;
  otpauthUri?: string;
  secret?: string;
  message?: string;
}

export interface CompleteProfilePayload {
  cin: string;
  phoneNumber: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private api: ApiService) {}

  getCurrentUser(): Observable<ConnectedUserResponse> {
    return this.api.get<ConnectedUserResponse>('/users/me');
  }

  getMyProfile(): Observable<UserProfileResponse> {
    return this.api.get<UserProfileResponse>('/users/me');
  }

  getCurrentDashboardUser(): Observable<DashboardUser> {
    return this.getCurrentUser().pipe(
      map((user: ConnectedUserResponse) => this.mapToDashboardUser(user))
    );
  }

  checkProfileComplete(): Observable<{ incomplete: boolean }> {
    return this.api.get<{ incomplete: boolean }>('/users/me/profile-complete');
  }

  completeGoogleProfile(payload: CompleteProfilePayload): Observable<any> {
    return this.api.post('/users/me/complete-profile', payload);
  }

  setupTwoFactor(cin: number | string): Observable<SetupTwoFactorResponse> {
    return this.api.post<SetupTwoFactorResponse>(
      `/auth/setup-2fa/${encodeURIComponent(String(cin))}`,
      {}
    );
  }

  confirmTwoFactor(cin: number | string, code: string): Observable<any> {
    return this.api.post(
      `/auth/confirm-2fa/${encodeURIComponent(String(cin))}`,
      { code }
    );
  }

  disableTwoFactor(cin: number | string): Observable<any> {
    return this.api.post(
      `/auth/disable-2fa/${encodeURIComponent(String(cin))}`,
      {}
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
      user.firstName || user.firstname || user.prenom || '';

    const lastName =
      user.lastName || user.lastname || user.nom || '';

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
}