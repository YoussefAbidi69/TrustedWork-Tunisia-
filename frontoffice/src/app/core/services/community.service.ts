import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Community } from '../models/community.model';

@Injectable({
  providedIn: 'root'
})
export class CommunityService {
  private readonly baseUrl = `${environment.msCommunity}/api/communities`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Community[]> {
    return this.http.get<Community[]>(this.baseUrl);
  }

  getById(id: number): Observable<Community> {
    return this.http.get<Community>(`${this.baseUrl}/${id}`);
  }

  create(payload: {
    name: string;
    description: string;
    createdBy: number;
  }): Observable<Community> {
    return this.http.post<Community>(this.baseUrl, payload);
  }
}
