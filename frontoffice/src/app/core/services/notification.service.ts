import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification } from '../models/notification.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  // On passe par l'API Gateway pour éviter les erreurs CORS
  private apiUrl = 'http://localhost:8085/api/v1/contracts/notifications';

  constructor(private http: HttpClient, private authService: AuthService) {}

  private getHeaders(cin: string): HttpHeaders {
    let headers = new HttpHeaders();
    if (cin) {
      headers = headers.set('X-User-Cin', cin);
    }
    return headers;
  }

  getMyNotifications(cin: string): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/my-notifications`, { headers: this.getHeaders(cin) });
  }

  getUnreadNotifications(cin: string): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/unread`, { headers: this.getHeaders(cin) });
  }

  getUnreadCount(cin: string): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/unread/count`, { headers: this.getHeaders(cin) });
  }

  markAsRead(id: number, cin: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/read`, {}, { headers: this.getHeaders(cin) });
  }

  markAllAsRead(cin: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/mark-all-read`, {}, { headers: this.getHeaders(cin) });
  }
}
