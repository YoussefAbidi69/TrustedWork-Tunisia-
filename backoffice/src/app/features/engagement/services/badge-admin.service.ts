import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BadgeDTO } from '../models/engagement.models';

const BASE = 'http://localhost:8086/api/badges';

@Injectable({ providedIn: 'root' })
export class BadgeAdminService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<BadgeDTO[]> {
    return this.http.get<BadgeDTO[]>(BASE);
  }

  getById(id: number): Observable<BadgeDTO> {
    return this.http.get<BadgeDTO>(`${BASE}/${id}`);
  }

  create(dto: BadgeDTO): Observable<BadgeDTO> {
    return this.http.post<BadgeDTO>(BASE, dto);
  }

  update(id: number, dto: BadgeDTO): Observable<BadgeDTO> {
    return this.http.put<BadgeDTO>(`${BASE}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}
