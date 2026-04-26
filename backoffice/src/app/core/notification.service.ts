import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Notification } from './models/notification.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = 'http://localhost:8085/api/v1/contracts/notifications';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    // 0L is the special CIN used for Admin notifications in the backend
    return new HttpHeaders().set('X-User-Cin', '0');
  }

  getUnreadNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/unread`, { headers: this.getHeaders() });
  }

  markAsRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/read`, {}, { headers: this.getHeaders() });
  }

  markAllAsRead(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/mark-all-read`, {}, { headers: this.getHeaders() });
  }
}
