import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { Task, TaskStatus } from '../../../core/models/agency.model';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  constructor(private api: ApiService) {}

  getTasksByAgency(agencyId: number): Observable<Task[]> {
    return this.api.get<Task[]>(`/agencies/${agencyId}/tasks`);
  }

  getTasksByProject(agencyId: number, projectId: number): Observable<Task[]> {
    return this.api.get<Task[]>(`/agencies/${agencyId}/projects/${projectId}/tasks`);
  }

  createTask(agencyId: number, task: Partial<Task>, requesterId: number): Observable<Task> {
    const payload = { ...task, requesterId };
    return this.api.post<Task>(`/agencies/${agencyId}/tasks`, payload);
  }

  updateTask(agencyId: number, taskId: number, task: Partial<Task>, requesterId: number): Observable<Task> {
    const payload = { ...task, requesterId };
    return this.api.patch<Task>(`/agencies/${agencyId}/tasks/${taskId}`, payload);
  }

  updateTaskStatus(agencyId: number, taskId: number, requesterId: number, status: TaskStatus): Observable<Task> {
    return this.api.patch<Task>(`/agencies/${agencyId}/tasks/${taskId}/status?requesterId=${requesterId}`, { status });
  }

  deleteTask(agencyId: number, taskId: number, requesterId: number): Observable<void> {
    return this.api.delete<void>(`/agencies/${agencyId}/tasks/${taskId}?requesterId=${requesterId}`);
  }
}
