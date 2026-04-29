import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_URL = '/api';
export const JOB_BOARD_API = '/job-board-api';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  get<T>(url: string, options?: any): Observable<T> {
    return this.http.get<T>(`${API_URL}${url}`, options) as Observable<T>;
  }

  post<T>(url: string, body: unknown, options?: any): Observable<T> {
    return this.http.post<T>(`${API_URL}${url}`, body, options) as Observable<T>;
  }

  put<T>(url: string, body: unknown, options?: any): Observable<T> {
    return this.http.put<T>(`${API_URL}${url}`, body, options) as Observable<T>;
  }

  patch<T>(url: string, body: unknown, options?: any): Observable<T> {
    return this.http.patch<T>(`${API_URL}${url}`, body, options) as Observable<T>;
  }

  delete<T>(url: string, options?: any): Observable<T> {
    return this.http.delete<T>(`${API_URL}${url}`, options) as Observable<T>;
  }
}
