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
    return this.api.get<TeamProject[]>(`/agencies/${agencyId}/projects`);
  }

  getActiveProjects(agencyId: number): Observable<TeamProject[]> {
    return this.api.get<TeamProject[]>(`/agencies/${agencyId}/projects/active`); // this isn't defined in the prompt, but keeping it
  }

  createProject(agencyId: number, project: Partial<TeamProject>, creatorMemberId: number): Observable<TeamProject> {
    return this.api.post<TeamProject>(`/agencies/${agencyId}/projects`, {
      ...project,
      creatorMemberId
    });
  }

  getProjectById(agencyId: number, projectId: number): Observable<TeamProject> {
    return this.api.get<TeamProject>(`/agencies/${agencyId}/projects/${projectId}`);
  }

  updateProject(agencyId: number, projectId: number, project: Partial<TeamProject>): Observable<TeamProject> {
    return this.api.patch<TeamProject>(`/agencies/${agencyId}/projects/${projectId}`, project);
  }

  deleteProject(agencyId: number, projectId: number): Observable<void> {
    return this.api.delete<void>(`/agencies/${agencyId}/projects/${projectId}`);
  }

  assignMembers(agencyId: number, projectId: number, requesterId: number, memberIds: number[]): Observable<TeamProject> {
    return this.api.post<TeamProject>(`/agencies/${agencyId}/projects/${projectId}/assign?requesterId=${requesterId}`, { memberIds });
  }

  removeMember(agencyId: number, projectId: number, requesterId: number, memberId: number): Observable<TeamProject> {
    return this.api.delete<TeamProject>(`/agencies/${agencyId}/projects/${projectId}/assign/${memberId}?requesterId=${requesterId}`);
  }
}
