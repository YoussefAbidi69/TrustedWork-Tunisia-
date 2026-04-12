import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { ProjectApiService } from '../../services/project-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import {
  Project, Task, SubTask, TaskStatus, TaskPriority,
  CreateTaskDTO, CreateSubTaskDTO
} from '../../models/project.models';

@Component({
  selector: 'app-kanban-board',
  templateUrl: './kanban-board.component.html',
  styleUrls: ['./kanban-board.component.css']
})
export class KanbanBoardComponent implements OnInit {


    currentUserId = 0;
  currentRole = '';
  isClient = false;
  isFreelancer = false;
  isAdmin = false;
  projectId!: number;
  project!: Project;
  loading = true;
  error = '';

  // ── Colonnes Kanban ──
  columns: { key: TaskStatus; label: string; icon: string; color: string }[] = [
    { key: 'TODO',        label: 'À faire',    icon: 'fa-circle-dot',    color: '#6d7a8d' },
    { key: 'IN_PROGRESS', label: 'En cours',    icon: 'fa-spinner',       color: '#3b82f6' },
    { key: 'IN_REVIEW',   label: 'En review',   icon: 'fa-eye',           color: '#f59e0b' },
    { key: 'DONE',        label: 'Terminée',    icon: 'fa-circle-check',  color: '#22c55e' }
  ];

  tasksByColumn: Record<TaskStatus, Task[]> = {
    TODO: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: []
  };

  // ── Sous-tâches (chargées par tâche) ──
  subTasksMap: Record<number, SubTask[]> = {};
  expandedTaskId: number | null = null;
  newSubTaskTitle: Record<number, string> = {};

  // ── Modal création tâche ──
  showTaskModal = false;
  newTask: CreateTaskDTO = { title: '', description: '', priority: 'MEDIUM', assigneeId: 0, deadline: '', estimatedHours: 8 };

  // ── Modal édition tâche ──
  showEditModal = false;
  editingTask: Task | null = null;

  // ── IDs des colonnes pour CDK ──
  columnIds: string[] = ['col-TODO', 'col-IN_PROGRESS', 'col-IN_REVIEW', 'col-DONE'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectApi: ProjectApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
  this.projectId = Number(this.route.snapshot.paramMap.get('id'));

  // Récupérer le rôle
  const user = this.authService.getCurrentAuthUser();
  this.currentUserId = user?.userId || 0;
  this.currentRole = user?.role?.toUpperCase() || '';
  this.isAdmin = this.currentRole === 'ADMIN';

  this.loadData();
  }

private loadData(): void {
  this.loading = true;

  this.projectApi.getProjectById(this.projectId).subscribe({
    next: (p) => {
      this.project = p;
      // ✅ Rôle basé sur le JWT
      this.isClient = this.currentRole === 'CLIENT';
      this.isFreelancer = this.currentRole === 'FREELANCER';
    },
    error: () => this.error = 'Projet introuvable.'
  });

  this.projectApi.getTasksByProjectId(this.projectId).subscribe({
    next: (tasks) => {
      this.distributeTasksToColumns(tasks);
      this.loading = false;
    },
    error: () => {
      this.error = 'Impossible de charger les tâches.';
      this.loading = false;
    }
  });
}

  private distributeTasksToColumns(tasks: Task[]): void {
    this.tasksByColumn = { TODO: [], IN_PROGRESS: [], IN_REVIEW: [], DONE: [] };
    tasks.forEach(task => {
      if (this.tasksByColumn[task.status]) {
        this.tasksByColumn[task.status].push(task);
      }
    });
  }

  // ══════════════════════════════════════
  // DRAG & DROP
  // ══════════════════════════════════════

  onDrop(event: CdkDragDrop<Task[]>, targetStatus: TaskStatus): void {
    if (event.previousContainer === event.container) {
      // Réordonner dans la même colonne
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      // Déplacer vers une autre colonne
      const task = event.previousContainer.data[event.previousIndex];

      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );

      // Mise à jour optimiste : on met le statut localement
      task.status = targetStatus;

      // Appel API pour persister le changement
      this.projectApi.updateTaskStatus(task.id, targetStatus).subscribe({
        next: (updated) => {
          // Mettre à jour le completionRate du projet
          this.projectApi.getProjectById(this.projectId).subscribe(p => this.project = p);
        },
        error: () => {
          // Rollback en cas d'erreur
          this.loadData();
        }
      });
    }
  }

  // ══════════════════════════════════════
  // SOUS-TÂCHES
  // ══════════════════════════════════════

  toggleExpand(taskId: number): void {
    if (this.expandedTaskId === taskId) {
      this.expandedTaskId = null;
      return;
    }
    this.expandedTaskId = taskId;

    // Charger les sous-tâches si pas encore chargées
    if (!this.subTasksMap[taskId]) {
      this.projectApi.getSubTasksByTaskId(taskId).subscribe({
        next: (subs) => this.subTasksMap[taskId] = subs,
        error: () => this.subTasksMap[taskId] = []
      });
    }
  }

  toggleSubTask(subTask: SubTask): void {
    this.projectApi.toggleSubTask(subTask.id).subscribe({
      next: (updated) => {
        const list = this.subTasksMap[subTask.taskId];
        if (list) {
          const idx = list.findIndex(s => s.id === subTask.id);
          if (idx >= 0) list[idx] = updated;
        }
      }
    });
  }

  addSubTask(taskId: number): void {
    const title = (this.newSubTaskTitle[taskId] || '').trim();
    if (!title) return;

    this.projectApi.createSubTask(taskId, { title }).subscribe({
      next: (created) => {
        if (!this.subTasksMap[taskId]) this.subTasksMap[taskId] = [];
        this.subTasksMap[taskId].push(created);
        this.newSubTaskTitle[taskId] = '';
      }
    });
  }

  deleteSubTask(subTask: SubTask): void {
    this.projectApi.deleteSubTask(subTask.id).subscribe({
      next: () => {
        const list = this.subTasksMap[subTask.taskId];
        if (list) {
          this.subTasksMap[subTask.taskId] = list.filter(s => s.id !== subTask.id);
        }
      }
    });
  }

  getSubTaskProgress(taskId: number): { done: number; total: number } {
    const list = this.subTasksMap[taskId] || [];
    return { done: list.filter(s => s.done).length, total: list.length };
  }

  // ══════════════════════════════════════
  // CRÉATION TÂCHE
  // ══════════════════════════════════════

  openTaskModal(): void {
    this.newTask = { title: '', description: '', priority: 'MEDIUM', assigneeId: 0, deadline: '', estimatedHours: 8 };
    this.showTaskModal = true;
  }

  closeTaskModal(): void {
    this.showTaskModal = false;
  }

  submitNewTask(): void {
    if (!this.newTask.title.trim()) return;
    const user = this.authService.getCurrentAuthUser();
    this.newTask.assigneeId = user?.userId || 0;

    this.projectApi.createTask(this.projectId, this.newTask).subscribe({
      next: (task) => {
        this.tasksByColumn['TODO'].push(task);
        this.showTaskModal = false;
      }
    });
  }

  // ══════════════════════════════════════
  // ÉDITION TÂCHE
  // ══════════════════════════════════════

  openEditModal(task: Task, event: Event): void {
    event.stopPropagation();
    this.editingTask = { ...task };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingTask = null;
  }

  submitEditTask(): void {
    if (!this.editingTask) return;
    this.projectApi.updateTask(this.editingTask.id, this.editingTask).subscribe({
      next: (updated) => {
        // Mettre à jour dans la colonne
        const col = this.tasksByColumn[updated.status];
        const idx = col.findIndex(t => t.id === updated.id);
        if (idx >= 0) col[idx] = updated;
        this.closeEditModal();
      }
    });
  }

  // ══════════════════════════════════════
  // SUPPRESSION
  // ══════════════════════════════════════

  deleteTask(task: Task, event: Event): void {
    event.stopPropagation();
    if (!confirm('Supprimer la tâche "' + task.title + '" ?')) return;

    this.projectApi.deleteTask(task.id).subscribe({
      next: () => {
        this.tasksByColumn[task.status] = this.tasksByColumn[task.status].filter(t => t.id !== task.id);
        this.projectApi.getProjectById(this.projectId).subscribe(p => this.project = p);
      }
    });
  }

  // ══════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════

  get allTasks(): Task[] {
    return [
      ...this.tasksByColumn['TODO'],
      ...this.tasksByColumn['IN_PROGRESS'],
      ...this.tasksByColumn['IN_REVIEW'],
      ...this.tasksByColumn['DONE']
    ];
  }

  getPriorityIcon(p: string): string {
    return { LOW: 'fa-arrow-down', MEDIUM: 'fa-minus', HIGH: 'fa-arrow-up', CRITICAL: 'fa-fire' }[p] || 'fa-minus';
  }

  getPriorityClass(p: string): string {
    return { LOW: 'p--low', MEDIUM: 'p--med', HIGH: 'p--high', CRITICAL: 'p--crit' }[p] || '';
  }

  getPriorityLabel(p: string): string {
    return { LOW: 'Basse', MEDIUM: 'Moyenne', HIGH: 'Haute', CRITICAL: 'Critique' }[p] || p;
  }

  isOverdue(task: Task): boolean {
    if (!task.deadline || task.status === 'DONE') return false;
    return new Date(task.deadline) < new Date();
  }

  goBack(): void {
    this.router.navigate(['/app/projects', this.projectId]);
  }

  /** Le freelancer et l'admin peuvent créer/modifier/supprimer des tâches */
get canEditTasks(): boolean {
  return this.isFreelancer || this.isAdmin;
}

/** Le client ne peut que consulter, pas déplacer */
get canDragTasks(): boolean {
  return this.isFreelancer || this.isAdmin;
}
}