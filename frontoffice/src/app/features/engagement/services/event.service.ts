import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventDTO, EventRegistration } from '../models/engagement.models';

const BASE = '/api';

@Injectable({ providedIn: 'root' })
export class EventService {
  constructor(private http: HttpClient) {}

  getAllEvents(): Observable<EventDTO[]> {
    return this.http.get<EventDTO[]>(`${BASE}/events`);
  }

  getEventsByGovernorate(gov: string): Observable<EventDTO[]> {
    return this.http.get<EventDTO[]>(`${BASE}/events/governorate/${gov}`);
  }

  createEvent(dto: EventDTO): Observable<EventDTO> {
    return this.http.post<EventDTO>(`${BASE}/events`, dto);
  }

  registerToEvent(eventId: number): Observable<EventRegistration> {
    return this.http.post<EventRegistration>(`${BASE}/events/${eventId}/register`, {});
  }

  markAttended(regId: number): Observable<void> {
    return this.http.patch<void>(`${BASE}/events/registrations/${regId}/attend`, {});
  }

  getMyRegistrations(): Observable<number[]> {
    return this.http.get<number[]>(`${BASE}/events/my-registrations`);
  }
}
