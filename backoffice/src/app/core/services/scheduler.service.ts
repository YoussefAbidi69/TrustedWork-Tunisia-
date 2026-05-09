import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Modèle correspondant à l'entité SchedulerConfig du backend
export interface SchedulerConfig {
  id: number;
  jobName: string;
  description: string;
  cronExpression: string;
  intervalMinutes: number;
  enabled: boolean;
  lastRun: string | null;   // ISO 8601 (ex: "2026-04-20T01:00:00")
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class SchedulerService {

  // Routé vers port 8082 (freelancer-profile-service) via proxy.conf.json
  private readonly baseUrl = '/api/scheduler/config';

  constructor(private http: HttpClient) {}

  /** Récupère toutes les configurations de schedulers */
  getAllConfigs(): Observable<SchedulerConfig[]> {
    return this.http.get<SchedulerConfig[]>(this.baseUrl);
  }

  /** Met à jour un job (cronExpression, intervalMinutes, enabled) */
  updateConfig(jobName: string, payload: Partial<SchedulerConfig>): Observable<SchedulerConfig> {
    return this.http.put<SchedulerConfig>(`${this.baseUrl}/${jobName}`, payload);
  }

  /** Déclenche immédiatement un job (test / démo jury) */
  runJobNow(jobName: string): Observable<{ status: string; jobName: string; message: string }> {
    return this.http.post<{ status: string; jobName: string; message: string }>(
      `${this.baseUrl}/${jobName}/run`, {}
    );
  }
}
