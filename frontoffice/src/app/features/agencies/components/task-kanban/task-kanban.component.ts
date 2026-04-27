import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { TaskService } from '../../services/task.service';
import { AgencyService } from '../../services/agency.service';
import { TeamProjectService } from '../../services/team-project.service';
import { Task, TaskStatus, TaskPriority, TeamProject, AgencyMember } from '../../../../core/models/agency.model';
import { AgencyMemberService } from '../../services/agency-member.service';
import { finalize } from 'rxjs/operators';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../../../core/services/auth.service';

interface KanbanColumn {
  id: TaskStatus;
  title: string;
  tasks: Task[];
}

@Component({
  selector: 'app-task-kanban',
  templateUrl: './task-kanban.component.html',
  styleUrls: ['./task-kanban.component.scss']
})
export class TaskKanbanComponent implements OnInit {
  @Input() agencyId!: number;
  
  isLead: boolean = false;
  userId!: number;

  columns: KanbanColumn[] = [
    { id: TaskStatus.BACKLOG, title: 'Backlog', tasks: [] },
    { id: TaskStatus.A_FAIRE, title: 'À Faire', tasks: [] },
    { id: TaskStatus.EN_COURS, title: 'En Cours', tasks: [] },
    { id: TaskStatus.REVIEW, title: 'Review', tasks: [] },
    { id: TaskStatus.TERMINE, title: 'Terminé', tasks: [] },
    { id: TaskStatus.ANNULE, title: 'Annulé', tasks: [] }
  ];

  allTasks: Task[] = [];
  projects: TeamProject[] = [];
  members: AgencyMember[] = [];

  loading = true;
  submitting = false;

  // Filters
  filterProjectId = '';
  filterPriority = '';
  filterMemberId = '';
  searchQuery = '';

  // Modal forms
  showModal = false;
  isEditing = false;
  
  currentTask: Partial<Task> = this.getEmptyTask();

  // Delete confirm
  showDeleteConfirm = false;
  taskToDelete: Task | null = null;

  TaskStatus = TaskStatus;
  TaskPriority = TaskPriority;

  constructor(
    private taskService: TaskService,
    private agencyService: AgencyService,
    private projectService: TeamProjectService,
    private memberService: AgencyMemberService,
    private authService: AuthService,
    private toastr: ToastrService
  ) {}

  ngOnInit() {
    this.agencyService.currentAgencyRole$.subscribe((r: string | null) => {
      this.isLead = (r === 'LEAD');
    });
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.userId = authUser.userId;
    }

    this.loadData();
  }

  getEmptyTask(): Partial<Task> {
    return {
      title: '',
      description: '',
      status: TaskStatus.BACKLOG,
      priority: TaskPriority.MOYENNE,
      projectId: undefined,
      assignedMemberId: undefined
    };
  }

  loadData() {
    this.loading = true;
    
    // Load Projects
    this.projectService.getProjectsByAgency(this.agencyId).subscribe(projs => {
      this.projects = projs.filter(p => p.active);
    });

    // Load Members
    this.memberService.getMembersByAgency(this.agencyId).subscribe(mems => {
      // exclude LEADs from assignee if needed, but the prompt says they can be assigned?
      // "Le membre assigné doit appartenir à l'agence"
      this.members = mems.filter(m => m.status === 'ACTIVE');
    });

    // Load Tasks
    this.taskService.getTasksByAgency(this.agencyId).subscribe(t => {
      this.allTasks = t;
      this.applyFilters();
      this.loading = false;
    });
  }

  applyFilters() {
    let filtered = [...this.allTasks];

    if (this.filterProjectId) {
      filtered = filtered.filter(t => t.projectId === Number(this.filterProjectId));
    }
    if (this.filterPriority) {
      filtered = filtered.filter(t => t.priority === this.filterPriority);
    }
    if (this.filterMemberId) {
      const memId = Number(this.filterMemberId);
      filtered = filtered.filter(t => t.assignedMember?.memberId === memId);
    }
    if (this.searchQuery) {
      filtered = filtered.filter(t => t.title.toLowerCase().includes(this.searchQuery.toLowerCase()));
    }

    // Distribute into columns
    this.columns.forEach(col => {
      col.tasks = filtered.filter(t => t.status === col.id)
                          .sort((a,b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime()); 
    });
  }

  resetFilters() {
    this.filterProjectId = '';
    this.filterPriority = '';
    this.filterMemberId = '';
    this.searchQuery = '';
    this.applyFilters();
  }

  onFilterChange() {
    this.applyFilters();
  }

  getProjectName(projectId?: number): string {
    if (!projectId) return 'Projet inconnu';
    const proj = this.projects.find(p => p.id === projectId);
    return proj ? proj.name : 'Projet inconnu';
  }

  canMoveTask(task: Task): boolean {
    if (this.isLead) return true;
    return task.assignedMember?.userId === this.userId;
  }

  // --- Drag & Drop ---
  drop(event: CdkDragDrop<Task[]>) {
    const task = event.item.data as Task;
    if (!this.canMoveTask(task)) {
      this.toastr.warning("Vous ne pouvez déplacer que les tâches qui vous sont assignées.");
      return;
    }

    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const task = event.previousContainer.data[event.previousIndex];
      const newStatus = event.container.id as TaskStatus;

      // Optimistic update
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );

      // Call API
      this.taskService.updateTaskStatus(this.agencyId, task.id, this.userId, newStatus).subscribe({
        next: (updatedTask) => {
          // Update allTasks array reference
          const idx = this.allTasks.findIndex(t => t.id === updatedTask.id);
          if (idx !== -1) {
            this.allTasks[idx] = updatedTask;
          }
          // Note: Here we could reload the agency members workload since tasks EN_COURS changed.
        },
        error: () => {
          this.toastr.error('Erreur lors du déplacement de la tâche');
          this.loadData(); // revert
        }
      });
    }
  }

  // --- Modals ---
  openCreateModal(defaultStatus?: TaskStatus) {
    if (!this.isLead) return;
    this.isEditing = false;
    this.currentTask = this.getEmptyTask();
    if (defaultStatus) {
      this.currentTask.status = defaultStatus;
    }
    this.showModal = true;
  }

  openEditModal(task: Task) {
    if (!this.isLead) return;
    this.isEditing = true;
    this.currentTask = {
      ...task,
      assignedMemberId: task.assignedMember ? task.assignedMember.memberId : undefined
    };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.currentTask = this.getEmptyTask();
  }

  saveTask() {
    if (!this.isLead) return;
    if (!this.currentTask.title || !this.currentTask.projectId || !this.currentTask.priority || !this.currentTask.status) return;

    this.submitting = true;

    if (!this.currentTask.assignedMemberId) {
        this.currentTask.assignedMemberId = undefined;
    }

    if (this.isEditing && this.currentTask.id) {
      this.taskService.updateTask(this.agencyId, this.currentTask.id, this.currentTask, this.userId)
        .pipe(finalize(() => this.submitting = false))
        .subscribe({
          next: () => {
            this.toastr.success('Tâche modifiée');
            this.closeModal();
            this.loadData();
          },
          error: () => this.toastr.error('Erreur lors de la modification')
        });
    } else {
      this.taskService.createTask(this.agencyId, this.currentTask, this.userId)
        .pipe(finalize(() => this.submitting = false))
        .subscribe({
          next: () => {
            this.toastr.success('Tâche créée');
            this.closeModal();
            this.loadData();
          },
          error: () => this.toastr.error('Erreur lors de la création')
        });
    }
  }

  // --- Delete ---
  confirmDelete(task: Task) {
    if (!this.isLead) return;
    this.taskToDelete = task;
    this.showDeleteConfirm = true;
  }

  deleteTask() {
    if (!this.isLead || !this.taskToDelete) return;
    
    this.taskService.deleteTask(this.agencyId, this.taskToDelete.id, this.userId).subscribe({
      next: () => {
        this.toastr.success('Tâche supprimée');
        this.showDeleteConfirm = false;
        this.taskToDelete = null;
        this.loadData();
      },
      error: () => this.toastr.error('Erreur lors de la suppression')
    });
  }

  getPriorityClass(p: string): string {
    switch(p) {
      case 'URGENTE': return 'badge-danger';
      case 'HAUTE': return 'badge-warning';
      case 'MOYENNE': return 'badge-info';
      case 'FAIBLE': return 'badge-secondary';
      default: return 'badge-light';
    }
  }
}
