import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Injectable({
  providedIn: 'root'
})
export class TaskAssignmentService {
  constructor(private api: ApiService) {}

  assignTask(taskId: number, memberId: number): Observable<any> {
    return this.api.post<any>(`/task-assignments/assign`, { taskId, memberId });
  }

  autoAssignTask(taskId: number, agencyId: number): Observable<any> {
    return this.api.post<any>(`/task-assignments/auto-assign/${taskId}/agency/${agencyId}`, {});
  }

  autoAssignBySkills(taskId: number, agencyId: number): Observable<any> {
    return this.api.post<any>(`/task-assignments/auto-assign-skills/${taskId}/agency/${agencyId}`, {});
  }

  autoAssignSmart(taskId: number, agencyId: number): Observable<any> {
    return this.api.post<any>(`/task-assignments/auto-assign-smart/${taskId}/agency/${agencyId}`, {});
  }

  updateCompletionScore(assignmentId: number, completionScore: number): Observable<any> {
    return this.api.put<any>(`/task-assignments/${assignmentId}/completion-score`, { completionScore });
  }

  getAssignmentsByAgency(agencyId: number): Observable<any[]> {
    return this.api.get<any[]>(`/task-assignments/agency/${agencyId}`);
  }
}
