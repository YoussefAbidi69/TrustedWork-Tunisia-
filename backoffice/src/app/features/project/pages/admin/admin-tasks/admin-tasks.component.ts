import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ProjectApiService } from '../../../services/project-api.service';
import { Project, Task, TaskStatus, TaskPriority } from '../../../models/project.models';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-tasks',
  templateUrl: './admin-tasks.component.html',
  styleUrls: ['./admin-tasks.component.css']
})
export class AdminTasksComponent implements OnInit {

  projects: Project[] = [];
  allTasks: Task[] = [];
  filtered: Task[] = [];
  loading = true;

  searchQuery = '';
  filterProject = 0;     // 0 = all
  filterStatus = 'ALL';
  filterPriority = 'ALL';

  statusList: string[] = ['ALL', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
  priorityList: string[] = ['ALL', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  // Modal status change
  showStatusModal = false;
  target: Task | null = null;
  newStatus: TaskStatus = 'TODO';

  // Modal delete
  showDelete = false;
  deleteTarget: Task | null = null;

  constructor(private api: ProjectApiService, private router: Router) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading = true;
    this.api.getAllProjects().subscribe({
      next: projects => {
        this.projects = projects;
        if (!projects.length) { this.loading = false; return; }
        forkJoin(projects.map(p => this.api.getTasksByProjectId(p.id))).subscribe({
          next: arrays => {
            this.allTasks = arrays.flat();
            this.applyFilters();
            this.loading = false;
          },
          error: () => this.loading = false
        });
      },
      error: () => this.loading = false
    });
  }

  applyFilters(): void {
    const q = this.searchQuery.trim().toLowerCase();
    this.filtered = this.allTasks.filter(t => {
      const matchSearch = !q || t.title.toLowerCase().includes(q) || t.id.toString().includes(q);
      const matchProject = !this.filterProject || t.projectId === this.filterProject;
      const matchStatus = this.filterStatus === 'ALL' || t.status === this.filterStatus;
      const matchPriority = this.filterPriority === 'ALL' || t.priority === this.filterPriority;
      return matchSearch && matchProject && matchStatus && matchPriority;
    });
  }

  openStatus(t: Task): void { this.target = t; this.newStatus = t.status; this.showStatusModal = true; }
  submitStatus(): void {
    if (!this.target) return;
    this.api.updateTaskStatus(this.target.id, this.newStatus).subscribe({
      next: updated => {
        const i = this.allTasks.findIndex(x => x.id === updated.id);
        if (i !== -1) this.allTasks[i] = updated;
        this.applyFilters(); this.showStatusModal = false;
      }
    });
  }

  openDelete(t: Task): void { this.deleteTarget = t; this.showDelete = true; }
  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.api.deleteTask(this.deleteTarget.id).subscribe({
      next: () => {
        this.allTasks = this.allTasks.filter(x => x.id !== this.deleteTarget!.id);
        this.applyFilters(); this.showDelete = false;
      }
    });
  }

  goBack(): void { this.router.navigate(['/admin/projects']); }
  projectTitle(id: number): string {
    return this.projects.find(p => p.id === id)?.title || `Projet #${id}`;
  }

  statusLabel(s: string): string {
    return ({ TODO: 'To Do', IN_PROGRESS: 'In Progress', IN_REVIEW: 'In Review', DONE: 'Done' } as any)[s] || s;
  }
  statusBadge(s: string): string {
    return ({ TODO: 'badge-muted', IN_PROGRESS: 'badge-info', IN_REVIEW: 'badge-warning', DONE: 'badge-success' } as any)[s] || 'badge-muted';
  }
  priorityBadge(p: string): string {
    return ({ LOW: 'badge-muted', MEDIUM: 'badge-info', HIGH: 'badge-warning', CRITICAL: 'badge-danger' } as any)[p] || 'badge-muted';
  }
  priorityIcon(p: string): string {
    return ({ LOW: 'fa-arrow-down', MEDIUM: 'fa-minus', HIGH: 'fa-arrow-up', CRITICAL: 'fa-fire' } as any)[p] || 'fa-minus';
  }
}
