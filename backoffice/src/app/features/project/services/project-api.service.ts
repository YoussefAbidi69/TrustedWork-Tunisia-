import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Project, CreateProjectDTO,
  Task, CreateTaskDTO, TaskStatus,
  SubTask, CreateSubTaskDTO,
  Deliverable, CreateDeliverableDTO, DeliverableStatus,
  ProgressReport,
  DeliveryRiskSignal,
  ProjectNotification,
  ProjectStatus,
  MLPrediction
} from '../models/project.models';

@Injectable({ providedIn: 'root' })
export class ProjectApiService {

  // ms-project-service → port 8089, context-path /project
  private readonly BASE = 'http://localhost:8089/project';

  constructor(private http: HttpClient) {}

  private getHeaders(): HttpHeaders {
    const token =
      localStorage.getItem('access_token') ||
      localStorage.getItem('token') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  // ══════════════════════════════════════════════════════════
  // PROJECTS — 8 endpoints
  // ══════════════════════════════════════════════════════════

  createProject(dto: CreateProjectDTO): Observable<Project> {
    return this.http.post<Project>(
      `${this.BASE}/api/projects`, dto, { headers: this.getHeaders() }
    );
  }

  getProjectById(id: number): Observable<Project> {
    return this.http.get<Project>(
      `${this.BASE}/api/projects/${id}`, { headers: this.getHeaders() }
    );
  }

  getProjectByIdEnriched(id: number): Observable<Project> {
    return this.http.get<Project>(
      `${this.BASE}/api/projects/${id}/enriched`, { headers: this.getHeaders() }
    );
  }

  getProjectByContractId(contractId: number): Observable<Project> {
    return this.http.get<Project>(
      `${this.BASE}/api/projects/contract/${contractId}`, { headers: this.getHeaders() }
    );
  }

  getProjectsByUserId(userId: number): Observable<Project[]> {
    return this.http.get<Project[]>(
      `${this.BASE}/api/projects/user/${userId}`, { headers: this.getHeaders() }
    );
  }

  getAllProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(
      `${this.BASE}/api/projects`, { headers: this.getHeaders() }
    );
  }

  updateProject(id: number, dto: Partial<Project>): Observable<Project> {
    return this.http.put<Project>(
      `${this.BASE}/api/projects/${id}`, dto, { headers: this.getHeaders() }
    );
  }

  updateProjectStatus(id: number, status: ProjectStatus): Observable<Project> {
    return this.http.patch<Project>(
      `${this.BASE}/api/projects/${id}/status`, null,
      { headers: this.getHeaders(), params: new HttpParams().set('status', status) }
    );
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE}/api/projects/${id}`, { headers: this.getHeaders() }
    );
  }

  // ══════════════════════════════════════════════════════════
  // TASKS — 7 endpoints
  // ══════════════════════════════════════════════════════════

  createTask(projectId: number, dto: CreateTaskDTO): Observable<Task> {
    return this.http.post<Task>(
      `${this.BASE}/api/projects/${projectId}/tasks`, dto, { headers: this.getHeaders() }
    );
  }

  getTasksByProjectId(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(
      `${this.BASE}/api/projects/${projectId}/tasks`, { headers: this.getHeaders() }
    );
  }

  getTaskById(id: number): Observable<Task> {
    return this.http.get<Task>(
      `${this.BASE}/api/tasks/${id}`, { headers: this.getHeaders() }
    );
  }

  updateTask(id: number, dto: Partial<Task>): Observable<Task> {
    return this.http.put<Task>(
      `${this.BASE}/api/tasks/${id}`, dto, { headers: this.getHeaders() }
    );
  }

  updateTaskStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(
      `${this.BASE}/api/tasks/${id}/status`, null,
      { headers: this.getHeaders(), params: new HttpParams().set('status', status) }
    );
  }

  assignTask(id: number, assigneeId: number): Observable<Task> {
    return this.http.patch<Task>(
      `${this.BASE}/api/tasks/${id}/assign`, null,
      { headers: this.getHeaders(), params: new HttpParams().set('assigneeId', assigneeId.toString()) }
    );
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE}/api/tasks/${id}`, { headers: this.getHeaders() }
    );
  }

  // ══════════════════════════════════════════════════════════
  // SUBTASKS — 4 endpoints
  // ══════════════════════════════════════════════════════════

  createSubTask(taskId: number, dto: CreateSubTaskDTO): Observable<SubTask> {
    return this.http.post<SubTask>(
      `${this.BASE}/api/tasks/${taskId}/subtasks`, dto, { headers: this.getHeaders() }
    );
  }

  getSubTasksByTaskId(taskId: number): Observable<SubTask[]> {
    return this.http.get<SubTask[]>(
      `${this.BASE}/api/tasks/${taskId}/subtasks`, { headers: this.getHeaders() }
    );
  }

  toggleSubTask(id: number): Observable<SubTask> {
    return this.http.patch<SubTask>(
      `${this.BASE}/api/subtasks/${id}/toggle`, null, { headers: this.getHeaders() }
    );
  }

  deleteSubTask(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE}/api/subtasks/${id}`, { headers: this.getHeaders() }
    );
  }

  // ══════════════════════════════════════════════════════════
  // DELIVERABLES — 5 endpoints
  // ══════════════════════════════════════════════════════════

  submitDeliverable(projectId: number, dto: CreateDeliverableDTO): Observable<Deliverable> {
    return this.http.post<Deliverable>(
      `${this.BASE}/api/projects/${projectId}/deliverables`, dto, { headers: this.getHeaders() }
    );
  }

  getDeliverablesByProjectId(projectId: number): Observable<Deliverable[]> {
    return this.http.get<Deliverable[]>(
      `${this.BASE}/api/projects/${projectId}/deliverables`, { headers: this.getHeaders() }
    );
  }

  getDeliverableById(id: number): Observable<Deliverable> {
    return this.http.get<Deliverable>(
      `${this.BASE}/api/deliverables/${id}`, { headers: this.getHeaders() }
    );
  }

  reviewDeliverable(id: number, status: DeliverableStatus, reviewComment: string): Observable<Deliverable> {
    return this.http.patch<Deliverable>(
      `${this.BASE}/api/deliverables/${id}/review`, null,
      { headers: this.getHeaders(), params: new HttpParams().set('status', status).set('reviewComment', reviewComment) }
    );
  }

  deleteDeliverable(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.BASE}/api/deliverables/${id}`, { headers: this.getHeaders() }
    );
  }

  // ══════════════════════════════════════════════════════════
  // PROGRESS REPORTS — 2 endpoints
  // ══════════════════════════════════════════════════════════

  generateReport(projectId: number): Observable<ProgressReport> {
    return this.http.get<ProgressReport>(
      `${this.BASE}/api/projects/${projectId}/report`, { headers: this.getHeaders() }
    );
  }

  getReportHistory(projectId: number): Observable<ProgressReport[]> {
    return this.http.get<ProgressReport[]>(
      `${this.BASE}/api/projects/${projectId}/reports`, { headers: this.getHeaders() }
    );
  }

  // ══════════════════════════════════════════════════════════
  // RISK SIGNALS — 4 endpoints
  // ══════════════════════════════════════════════════════════

  getActiveRisks(projectId: number): Observable<DeliveryRiskSignal[]> {
    return this.http.get<DeliveryRiskSignal[]>(
      `${this.BASE}/api/projects/${projectId}/risks`, { headers: this.getHeaders() }
    );
  }

  getLatestCriticalRisk(projectId: number): Observable<DeliveryRiskSignal> {
    return this.http.get<DeliveryRiskSignal>(
      `${this.BASE}/api/projects/${projectId}/risks/latest`, { headers: this.getHeaders() }
    );
  }

  analyzeRisks(projectId: number): Observable<DeliveryRiskSignal[]> {
    return this.http.post<DeliveryRiskSignal[]>(
      `${this.BASE}/api/projects/${projectId}/risks/analyze`, null, { headers: this.getHeaders() }
    );
  }

  resolveRisk(id: number): Observable<DeliveryRiskSignal> {
    return this.http.patch<DeliveryRiskSignal>(
      `${this.BASE}/api/risks/${id}/resolve`, null, { headers: this.getHeaders() }
    );
  }

  predictDeliveryRisk(projectId: number): Observable<MLPrediction> {
    return this.http.get<MLPrediction>(
      `${this.BASE}/api/projects/${projectId}/risks/ml-predict`, { headers: this.getHeaders() }
    );
  }

  // ══════════════════════════════════════════════════════════
  // NOTIFICATIONS — 6 endpoints
  // ══════════════════════════════════════════════════════════

  getNotifications(userId: number): Observable<ProjectNotification[]> {
    return this.http.get<ProjectNotification[]>(
      `${this.BASE}/api/notifications/user/${userId}`, { headers: this.getHeaders() }
    );
  }

  getUnreadNotifications(userId: number): Observable<ProjectNotification[]> {
    return this.http.get<ProjectNotification[]>(
      `${this.BASE}/api/notifications/user/${userId}/unread`, { headers: this.getHeaders() }
    );
  }

  getUnreadCount(userId: number): Observable<number> {
    return this.http.get<number>(
      `${this.BASE}/api/notifications/user/${userId}/unread/count`, { headers: this.getHeaders() }
    );
  }

  markAsRead(id: number): Observable<ProjectNotification> {
    return this.http.patch<ProjectNotification>(
      `${this.BASE}/api/notifications/${id}/read`, null, { headers: this.getHeaders() }
    );
  }

  markAllAsRead(userId: number): Observable<void> {
    return this.http.patch<void>(
      `${this.BASE}/api/notifications/user/${userId}/read-all`, null, { headers: this.getHeaders() }
    );
  }

  testNotifications(projectId: number): Observable<string> {
    return this.http.post(
      `${this.BASE}/api/notifications/test/${projectId}`, null,
      { headers: this.getHeaders(), responseType: 'text' }
    );
  }

  // ══════════════════════════════════════════════════════════
  // EXPORT — 2 endpoints
  // ══════════════════════════════════════════════════════════

  exportPdf(projectId: number): Observable<Blob> {
    return this.http.get(
      `${this.BASE}/api/projects/${projectId}/export/pdf`,
      { headers: this.getHeaders(), responseType: 'blob' }
    );
  }

  exportCsv(projectId: number): Observable<Blob> {
    return this.http.get(
      `${this.BASE}/api/projects/${projectId}/export/csv`,
      { headers: this.getHeaders(), responseType: 'blob' }
    );
  }
}
