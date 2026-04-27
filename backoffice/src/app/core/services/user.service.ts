import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserDTO {
  id: number;
  cin: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  photo: string;
  role: string;
  accountStatus: string;
  kycStatus: string;
  twoFactorEnabled: boolean;
  createdAt: string;
  trustLevel?: number;
  livenessPassed?: boolean;
  lastLoginAt?: string;
  updatedAt?: string;
  failedAttempts?: number;
  lockedUntil?: string | null;
}

export interface DashboardStats {
  totalUsers: number;
  totalFreelancers: number;
  totalClients: number;
  activeUsers: number;
  suspendedUsers: number;
  kycPending: number;
  kycApproved: number;
  kycRejected: number;
}

export interface KycRequestDTO {
  id: number;
  userId: number;
  userEmail?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  cin?: number;
  phone?: string;
  role?: string;
  accountStatus?: string;
  twoFactorEnabled?: boolean;
  createdAt?: string;
  cinDocumentPath?: string;
  selfiePath?: string;
  diplomaDocumentPath?: string;
  livenessScore?: number;
  livenessPassed?: boolean;
  status?: string;
}

export interface SuspensionRecordDTO {
  id: number;
  userId: number;
  reason: string;
  suspendedBy: string;
  suspendedAt: string;
  liftedAt?: string | null;
  liftedBy?: string | null;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private baseUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token =
      localStorage.getItem('access_token') ||
      localStorage.getItem('token') ||
      '';

    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }

  getAllUsers(): Observable<UserDTO[]> {
    return this.http.get<UserDTO[]>(`${this.baseUrl}/admin/users`, {
      headers: this.getHeaders()
    });
  }

  getUsersByRole(role: string): Observable<UserDTO[]> {
    return this.http.get<UserDTO[]>(`${this.baseUrl}/admin/users/role/${role}`, {
      headers: this.getHeaders()
    });
  }

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/admin/stats`, {
      headers: this.getHeaders()
    });
  }

  suspendUser(id: number, reason: string): Observable<SuspensionRecordDTO> {
    return this.http.post<SuspensionRecordDTO>(
      `${this.baseUrl}/admin/suspensions/suspend/${id}`,
      { reason },
      { headers: this.getHeaders() }
    );
  }

  liftSuspension(id: number): Observable<SuspensionRecordDTO> {
    return this.http.post<SuspensionRecordDTO>(
      `${this.baseUrl}/admin/suspensions/lift/${id}`,
      {},
      { headers: this.getHeaders() }
    );
  }

  getSuspensionHistory(id: number): Observable<SuspensionRecordDTO[]> {
    return this.http.get<SuspensionRecordDTO[]>(
      `${this.baseUrl}/admin/suspensions/history/${id}`,
      { headers: this.getHeaders() }
    );
  }

  getAllActiveSuspensions(): Observable<SuspensionRecordDTO[]> {
    return this.http.get<SuspensionRecordDTO[]>(
      `${this.baseUrl}/admin/suspensions/active`,
      { headers: this.getHeaders() }
    );
  }

  getPendingKyc(): Observable<KycRequestDTO[]> {
    return this.http.get<KycRequestDTO[]>(`${this.baseUrl}/kyc/requests/pending`, {
      headers: this.getHeaders()
    });
  }

  reviewKyc(id: number, decision: string, rejectReason: string): Observable<KycRequestDTO> {
    return this.http.put<KycRequestDTO>(
      `${this.baseUrl}/kyc/requests/review/${id}`,
      { decision, rejectReason },
      { headers: this.getHeaders() }
    );
  }

  getAuditLogs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/admin/audit-logs`, {
      headers: this.getHeaders()
    });
  }

  getAuditLogsByUser(email: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/admin/audit-logs/user/${email}`, {
      headers: this.getHeaders()
    });
  }
}