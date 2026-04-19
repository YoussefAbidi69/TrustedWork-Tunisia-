import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { TeamProject, ProjectStatus } from '../../../core/models/agency.model';

@Injectable({
  providedIn: 'root'
})
export class TeamProjectService {
  constructor(private api: ApiService) {}

  getProjectsByAgency(agencyId: number): Observable<TeamProject[]> {
    return this.api.get<TeamProject[]>(`/team-projects/agency/${agencyId}`);
  }

  getActiveProjects(agencyId: number): Observable<TeamProject[]> {
    return this.api.get<TeamProject[]>(`/team-projects/agency/${agencyId}/active`);
  }

  createProject(agencyId: number, creatorMemberId: number, project: Partial<TeamProject>): Observable<TeamProject> {
    return this.api.post<TeamProject>(`/team-projects/agency/${agencyId}`, {
      ...project,
      creatorMemberId
    });
  }

  getProjectById(id: number): Observable<TeamProject> {
    return this.api.get<TeamProject>(`/team-projects/${id}`);
  }

  updateProject(id: number, project: Partial<TeamProject>): Observable<TeamProject> {
    return this.api.put<TeamProject>(`/team-projects/${id}`, project);
  }

  updateProgress(id: number): Observable<TeamProject> {
    return this.api.put<TeamProject>(`/team-projects/${id}/progress`, {});
  }

  deleteProject(id: number): Observable<void> {
    return this.api.delete<void>(`/team-projects/${id}`);
  }
}
