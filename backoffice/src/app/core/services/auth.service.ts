import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private baseUrl = '/api';
  // URL du frontoffice — landing page
  private readonly FRONTOFFICE_URL = 'http://localhost:4200';

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/auth/login`, { email, password }).pipe(
      tap(res => {
        if (res.accessToken) {
          localStorage.setItem('token',   res.accessToken);
          localStorage.setItem('role',    res.role    || '');
          localStorage.setItem('email',   res.email   || email);
          localStorage.setItem('userId',  String(res.userId || ''));
        }
      })
    );
  }

  logout(): void {
    // Nettoyage de la session admin
    localStorage.clear();
    // Redirection vers la landing page du frontoffice
    window.location.href = this.FRONTOFFICE_URL;
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  getRole(): string {
    return localStorage.getItem('role') || '';
  }

  getEmail(): string {
    return localStorage.getItem('email') || '';
  }

  getUserId(): number {
    return parseInt(localStorage.getItem('userId') || '0', 10);
  }

  getCurrentAuthUser(): any {
    return {
      role: this.getRole(),
      email: this.getEmail(),
      userId: this.getUserId(),
      cin: this.getCin()
    };
  }

  getCin(): string {
    let cin = localStorage.getItem('cin');
    if (cin) return cin;

    const token = this.getToken();
    if (token) {
      try {
        const payloadStr = atob(token.split('.')[1]);
        const payload = JSON.parse(payloadStr);
        if (payload.cin) {
          localStorage.setItem('cin', String(payload.cin));
          return String(payload.cin);
        }
      } catch (e) {
        console.warn('Impossible de lire le CIN depuis le token', e);
      }
    }
    return '';
  }
}