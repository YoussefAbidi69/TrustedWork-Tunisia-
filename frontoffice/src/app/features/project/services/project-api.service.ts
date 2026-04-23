import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Project, CreateProjectDTO,
  Task, CreateTaskDTO, TaskStatus,
  SubTask, CreateSubTaskDTO,
  Deliverable, CreateDeliverableDTO, DeliverableStatus,
  ProgressReport,
  DeliveryRiskSignal,
  MLPrediction,
  ProjectNotification,
  ProjectStatus,
  TaskSuggestionResponse
} from '../models/project.models';



const GATEWAY_URL = 'http://localhost:8089/project';

@Injectable({
  providedIn: 'root'
})
export class ProjectApiService {

  constructor(private http: HttpClient) {}

  // ══════════════════════════════════════════
  // PROJECTS
  // ══════════════════════════════════════════

  createProject(dto: CreateProjectDTO): Observable<Project> {
    return this.http.post<Project>(`${GATEWAY_URL}/api/projects`, dto);
  }

  getProjectById(id: number): Observable<Project> {
    return this.http.get<Project>(`${GATEWAY_URL}/api/projects/${id}`);
  }

  /** Endpoint enrichi : retourne le projet avec CIN + nom + prénom */
  getProjectByIdEnriched(id: number): Observable<Project> {
    return this.http.get<Project>(`${GATEWAY_URL}/api/projects/${id}/enriched`);
  }

  getProjectsByUserId(userId: number): Observable<Project[]> {
    return this.http.get<Project[]>(`${GATEWAY_URL}/api/projects/user/${userId}`);
  }

  /**
   * Endpoint principal après login.
   * Retourne les projets de l'utilisateur connecté (lu depuis le JWT côté backend).
   * Token ajouté automatiquement par tokenInterceptor.
   */
  getMyProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${GATEWAY_URL}/api/projects/my`);
  }

  getAllProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${GATEWAY_URL}/api/projects`);
  }

  updateProject(id: number, dto: Partial<Project>): Observable<Project> {
    return this.http.put<Project>(`${GATEWAY_URL}/api/projects/${id}`, dto);
  }

  updateProjectStatus(id: number, status: ProjectStatus): Observable<Project> {
    return this.http.patch<Project>(
      `${GATEWAY_URL}/api/projects/${id}/status`,
      null,
      { params: new HttpParams().set('status', status) }
    );
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${GATEWAY_URL}/api/projects/${id}`);
  }

  // ══════════════════════════════════════════
  // TASKS
  // ══════════════════════════════════════════

  createTask(projectId: number, dto: CreateTaskDTO): Observable<Task> {
    return this.http.post<Task>(`${GATEWAY_URL}/api/projects/${projectId}/tasks`, dto);
  }

  getTasksByProjectId(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${GATEWAY_URL}/api/projects/${projectId}/tasks`);
  }

  getTaskById(id: number): Observable<Task> {
    return this.http.get<Task>(`${GATEWAY_URL}/api/tasks/${id}`);
  }

  updateTask(id: number, dto: Partial<Task>): Observable<Task> {
    return this.http.put<Task>(`${GATEWAY_URL}/api/tasks/${id}`, dto);
  }

  updateTaskStatus(id: number, status: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(
      `${GATEWAY_URL}/api/tasks/${id}/status`,
      null,
      { params: new HttpParams().set('status', status) }
    );
  }

  assignTask(id: number, assigneeId: number): Observable<Task> {
    return this.http.patch<Task>(
      `${GATEWAY_URL}/api/tasks/${id}/assign`,
      null,
      { params: new HttpParams().set('assigneeId', assigneeId.toString()) }
    );
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${GATEWAY_URL}/api/tasks/${id}`);
  }

  // ══════════════════════════════════════════
  // SUBTASKS
  // ══════════════════════════════════════════

  createSubTask(taskId: number, dto: CreateSubTaskDTO): Observable<SubTask> {
    return this.http.post<SubTask>(`${GATEWAY_URL}/api/tasks/${taskId}/subtasks`, dto);
  }

  getSubTasksByTaskId(taskId: number): Observable<SubTask[]> {
    return this.http.get<SubTask[]>(`${GATEWAY_URL}/api/tasks/${taskId}/subtasks`);
  }

  toggleSubTask(id: number): Observable<SubTask> {
    return this.http.patch<SubTask>(`${GATEWAY_URL}/api/subtasks/${id}/toggle`, null);
  }

  deleteSubTask(id: number): Observable<void> {
    return this.http.delete<void>(`${GATEWAY_URL}/api/subtasks/${id}`);
  }

  // ══════════════════════════════════════════
  // DELIVERABLES
  // ══════════════════════════════════════════

  submitDeliverable(projectId: number, dto: CreateDeliverableDTO): Observable<Deliverable> {
    return this.http.post<Deliverable>(`${GATEWAY_URL}/api/projects/${projectId}/deliverables`, dto);
  }

  getDeliverablesByProjectId(projectId: number): Observable<Deliverable[]> {
    return this.http.get<Deliverable[]>(`${GATEWAY_URL}/api/projects/${projectId}/deliverables`);
  }

  getDeliverableById(id: number): Observable<Deliverable> {
    return this.http.get<Deliverable>(`${GATEWAY_URL}/api/deliverables/${id}`);
  }

  reviewDeliverable(id: number, status: DeliverableStatus, reviewComment: string): Observable<Deliverable> {
    return this.http.patch<Deliverable>(
      `${GATEWAY_URL}/api/deliverables/${id}/review`,
      null,
      { params: new HttpParams().set('status', status).set('reviewComment', reviewComment) }
    );
  }

  deleteDeliverable(id: number): Observable<void> {
    return this.http.delete<void>(`${GATEWAY_URL}/api/deliverables/${id}`);
  }

  // ══════════════════════════════════════════
  // PROGRESS REPORTS
  // ══════════════════════════════════════════

  generateReport(projectId: number): Observable<ProgressReport> {
    return this.http.get<ProgressReport>(`${GATEWAY_URL}/api/projects/${projectId}/report`);
  }

  getReportHistory(projectId: number): Observable<ProgressReport[]> {
    return this.http.get<ProgressReport[]>(`${GATEWAY_URL}/api/projects/${projectId}/reports`);
  }

  // ══════════════════════════════════════════
  // RISK SIGNALS
  // ══════════════════════════════════════════

  getActiveRisks(projectId: number): Observable<DeliveryRiskSignal[]> {
    return this.http.get<DeliveryRiskSignal[]>(`${GATEWAY_URL}/api/projects/${projectId}/risks`);
  }

  getLatestCriticalRisk(projectId: number): Observable<DeliveryRiskSignal> {
    return this.http.get<DeliveryRiskSignal>(`${GATEWAY_URL}/api/projects/${projectId}/risks/latest`);
  }

  analyzeRisks(projectId: number): Observable<void> {
    return this.http.post<void>(`${GATEWAY_URL}/api/projects/${projectId}/risks/analyze`, null);
  }

  resolveRisk(id: number): Observable<DeliveryRiskSignal> {
    return this.http.patch<DeliveryRiskSignal>(`${GATEWAY_URL}/api/risks/${id}/resolve`, null);
  }

  // ══════════════════════════════════════════
  // NOTIFICATIONS
  // ══════════════════════════════════════════

  getNotifications(userId: number): Observable<ProjectNotification[]> {
    return this.http.get<ProjectNotification[]>(`${GATEWAY_URL}/api/notifications/user/${userId}`);
  }

  getUnreadNotifications(userId: number): Observable<ProjectNotification[]> {
    return this.http.get<ProjectNotification[]>(`${GATEWAY_URL}/api/notifications/user/${userId}/unread`);
  }

  getUnreadCount(userId: number): Observable<number> {
    return this.http.get<number>(`${GATEWAY_URL}/api/notifications/user/${userId}/unread/count`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${GATEWAY_URL}/api/notifications/${id}/read`, null);
  }

  markAllAsRead(userId: number): Observable<void> {
    return this.http.patch<void>(`${GATEWAY_URL}/api/notifications/user/${userId}/read-all`, null);
  }

  // ══════════════════════════════════════════
  // EXPORT
  // ══════════════════════════════════════════

  exportPdf(projectId: number): Observable<Blob> {
    return this.http.get(`${GATEWAY_URL}/api/projects/${projectId}/export/pdf`, {
      responseType: 'blob'
    });
  }

  exportCsv(projectId: number): Observable<Blob> {
    return this.http.get(`${GATEWAY_URL}/api/projects/${projectId}/export/csv`, {
      responseType: 'blob'
    });
  }

  // Prédiction ML Random Forest
  predictDeliveryRisk(projectId: number): Observable<MLPrediction> {
    return this.http.get<MLPrediction>(
      `${GATEWAY_URL}/api/projects/${projectId}/risks/ml-predict`
    );
  }

  // Suggestion IA de tâches (RandomForest)
  getSuggestedTasks(projectId: number): Observable<TaskSuggestionResponse> {
    return this.http.get<TaskSuggestionResponse>(
      `${GATEWAY_URL}/api/projects/${projectId}/suggest-tasks`
    );
  }
}