import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventDTO } from '../models/engagement.models';

const BASE = 'http://localhost:8086/api';

@Injectable({ providedIn: 'root' })
export class EventAdminService {
  constructor(private http: HttpClient) {}

  getAllEvents(): Observable<EventDTO[]> {
    return this.http.get<EventDTO[]>(`${BASE}/events`);
  }

  createEvent(dto: EventDTO): Observable<EventDTO> {
    return this.http.post<EventDTO>(`${BASE}/events`, dto);
  }

  updateEvent(id: number, dto: EventDTO): Observable<EventDTO> {
    return this.http.put<EventDTO>(`${BASE}/events/${id}`, dto);
  }

  deleteEvent(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/events/${id}`);
  }

  removeRegistration(eventId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/events/${eventId}/registrations/${userId}`);
  }
}
