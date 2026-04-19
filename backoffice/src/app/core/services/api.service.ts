import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const API_URL = environment.apiUrl;
const AUTH_URL = environment.authUrl;

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  constructor(private http: HttpClient) {}

  get<T>(url: string, params?: any, baseUrl: string = API_URL): Observable<T> {
    const fullUrl = url.startsWith('/') ? url : `/${url}`;
    return this.http.get<T>(`${baseUrl}${fullUrl}`, { params });
  }

  post<T>(url: string, body: unknown, params?: any, baseUrl: string = API_URL): Observable<T> {
    const fullUrl = url.startsWith('/') ? url : `/${url}`;
    return this.http.post<T>(`${baseUrl}${fullUrl}`, body, { params });
  }

  put<T>(url: string, body: unknown, params?: any, baseUrl: string = API_URL): Observable<T> {
    const fullUrl = url.startsWith('/') ? url : `/${url}`;
    return this.http.put<T>(`${baseUrl}${fullUrl}`, body, { params });
  }

  patch<T>(url: string, body?: unknown, params?: any, baseUrl: string = API_URL): Observable<T> {
    const fullUrl = url.startsWith('/') ? url : `/${url}`;
    return this.http.patch<T>(`${baseUrl}${fullUrl}`, body, { params });
  }

  delete<T>(url: string, params?: any, baseUrl: string = API_URL): Observable<T> {
    const fullUrl = url.startsWith('/') ? url : `/${url}`;
    return this.http.delete<T>(`${baseUrl}${fullUrl}`, { params });
  }

  // Helper for Auth service
  getAuthUrl(): string {
    return AUTH_URL;
  }
}