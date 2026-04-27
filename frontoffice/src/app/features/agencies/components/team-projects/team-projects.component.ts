import { Component, Input, OnInit } from '@angular/core';
import { TeamProjectService } from '../../services/team-project.service';
import { AgencyMemberService } from '../../services/agency-member.service';
import { TeamProject, ProjectStatus, ProjectPriority, AgencyMember, Task, TaskStatus } from '../../../../core/models/agency.model';
import { TaskService } from '../../services/task.service';

@Component({
  selector: 'app-team-projects',
  templateUrl: './team-projects.component.html',
  styleUrls: ['./team-projects.component.css']
})
export class TeamProjectsComponent implements OnInit {
  @Input() agencyId!: number;
  @Input() isLead: boolean = false;
  @Input() userId: number | null = null;
  
  projects: TeamProject[] = [];
  agencyMembers: AgencyMember[] = [];
  loading = true;

  // Modal State
  showModal = false;
  isEditing = false;
  submitting = false;
  currentProject: Partial<TeamProject> = {};
  
  // Enums for template
  ProjectStatus = ProjectStatus;
  ProjectPriority = ProjectPriority;

  // Modals for details / delete
  showDetailModal = false;
  selectedProject: TeamProject | null = null;
  
  projectTasks: Task[] = [];
  loadingTasks = false;

  showDeleteConfirm = false;
  projectToDelete: TeamProject | null = null;

  constructor(
    private projectService: TeamProjectService,
    private memberService: AgencyMemberService,
    private taskService: TaskService
  ) {}

  projectStats: { [projectId: number]: { total: number; completed: number } } = {};

  ngOnInit(): void {
    if (this.agencyId) {
      this.loadProjectsAndTasks();
      if (this.isLead) {
        this.loadMembers();
      }
    }
  }

  loadProjectsAndTasks(): void {
    this.loading = true;
    this.projectService.getProjectsByAgency(this.agencyId).subscribe({
      next: (projects) => {
        this.projects = projects;
        
        // Fetch all tasks for the agency to compute progress dynamically
        this.taskService.getTasksByAgency(this.agencyId).subscribe({
          next: (tasks) => {
            this.updateProjectProgress(tasks);
            this.loading = false;
          },
          error: (err) => {
            console.error('Could not load tasks for progress', err);
            this.loading = false;
          }
        });
      },
      error: (err: any) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  updateProjectProgress(tasks: Task[]): void {
    const stats: { [key: number]: { total: number; completed: number } } = {};
    
    // Initialize stats
    this.projects.forEach(p => {
      stats[p.id] = { total: 0, completed: 0 };
    });

    tasks.forEach(t => {
      if (stats[t.projectId]) {
        stats[t.projectId].total++;
        // Consider 'TERMINE' and 'DONE' as completed
        if (t.status === TaskStatus.TERMINE || (t.status as any) === 'DONE') {
          stats[t.projectId].completed++;
        }
      }
    });

    this.projectStats = stats;

    // Update progress on project objects
    this.projects.forEach(p => {
      const s = stats[p.id];
      if (s && s.total > 0) {
        p.progress = Math.round((s.completed / s.total) * 100);
      } else {
        p.progress = 0;
      }
    });
  }

  loadMembers(): void {
    this.memberService.getMembersByAgency(this.agencyId).subscribe({
      next: (data) => {
        this.agencyMembers = data;
      },
      error: (err) => console.error(err)
    });
  }

  openCreateModal(): void {
    if (!this.isLead) return;
    this.isEditing = false;
    this.currentProject = {
      agencyId: this.agencyId,
      status: ProjectStatus.EN_COURS,
      priority: ProjectPriority.MOYENNE,
      progress: 0,
      active: true,
      assignedMembers: []
    };
    this.showModal = true;
  }

  openEditModal(project: TeamProject): void {
    if (!this.isLead) return;
    this.isEditing = true;
    
    // Format dates for <input type="date">
    const formatForDateInput = (isoDate?: string) => isoDate ? isoDate.split('T')[0] : undefined;
    
    // Make a copy so we don't mutate the UI until saved
    this.currentProject = { 
      ...project,
      startDate: formatForDateInput(project.startDate),
      endDate: formatForDateInput(project.endDate),
      // Map assignedMembers to member IDs for checkboxes
      assignedMembers: project.assignedMembers ? project.assignedMembers.map(m => m.memberId) as any : []
    };
    this.showModal = true;
    this.showDetailModal = false; // close detail modal if it was open
  }

  closeModal(): void {
    this.showModal = false;
    this.currentProject = {};
  }

  toggleMemberSelection(memberId: number): void {
    if (!this.currentProject.assignedMembers) {
      this.currentProject.assignedMembers = [];
    }
    const arr = this.currentProject.assignedMembers as any[];
    const idx = arr.indexOf(memberId);
    if (idx > -1) {
      arr.splice(idx, 1);
    } else {
      arr.push(memberId);
    }
  }

  isMemberSelected(memberId: number): boolean {
    if (!this.currentProject.assignedMembers) return false;
    return (this.currentProject.assignedMembers as any[]).includes(memberId);
  }

  saveProject(): void {
    if (!this.isLead) return;
    this.submitting = true;

    // Convert local date string back to LocalDateTime format for backend
    const payload = { ...this.currentProject };
    if (payload.startDate && payload.startDate.length === 10) {
      payload.startDate = payload.startDate + 'T00:00:00';
    }
    if (payload.endDate && payload.endDate.length === 10) {
      payload.endDate = payload.endDate + 'T00:00:00';
    }

    if (this.isEditing && payload.id) {
      const { assignedMembers, ...patchData } = payload as any;
      this.projectService.updateProject(this.agencyId, payload.id, patchData).subscribe({
        next: (updated) => {
          const idx = this.projects.findIndex(p => p.id === updated.id);
          if (idx !== -1) {
            this.projects[idx] = updated;
          }
          this.submitting = false;
          this.closeModal();
        },
        error: (err) => {
          console.error(err);
          this.submitting = false;
        }
      });
    } else {
      // Create new
      this.projectService.createProject(this.agencyId, payload as TeamProject, this.userId!).subscribe({
        next: (created) => {
          this.projects.push(created);
          this.submitting = false;
          this.closeModal();
        },
        error: (err) => {
          console.error(err);
          this.submitting = false;
        }
      });
    }
  }

  confirmDelete(project: TeamProject): void {
    if (!this.isLead) return;
    this.projectToDelete = project;
    this.showDeleteConfirm = true;
  }

  deleteProject(): void {
    if (!this.isLead || !this.projectToDelete) return;
    
    this.projectService.deleteProject(this.agencyId, this.projectToDelete.id).subscribe({
      next: () => {
        this.projects = this.projects.filter(p => p.id !== this.projectToDelete!.id);
        this.showDeleteConfirm = false;
        this.projectToDelete = null;
      },
      error: (err) => {
        console.error(err);
        this.showDeleteConfirm = false;
      }
    });
  }

  openDetail(project: TeamProject): void {
    this.selectedProject = project;
    this.showDetailModal = true;
    this.loadProjectTasks(project.id);
  }

  loadProjectTasks(projectId: number): void {
    this.loadingTasks = true;
    this.taskService.getTasksByProject(this.agencyId, projectId).subscribe({
      next: (tasks) => {
        this.projectTasks = tasks;
        this.loadingTasks = false;
      },
      error: () => this.loadingTasks = false
    });
  }

  closeDetail(): void {
    this.showDetailModal = false;
    this.selectedProject = null;
  }

  onMemberAssigned(event: any, projectId: number): void {
    if (!this.isLead || !this.userId) return;
    const memberId = event.target.value;
    if (!memberId) return;

    this.projectService.assignMembers(this.agencyId, projectId, this.userId, [Number(memberId)]).subscribe({
      next: (updated) => {
        const idx = this.projects.findIndex(p => p.id === updated.id);
        if (idx !== -1) this.projects[idx] = updated;
        if (this.selectedProject && this.selectedProject.id === updated.id) {
          this.selectedProject = updated;
        }
        event.target.value = '';
      },
      error: (err) => console.error(err)
    });
  }

  removeAssignedMember(projectId: number, memberId: number): void {
    if (!this.isLead || !this.userId) return;
    if (!confirm('Voulez-vous retirer ce membre du projet ?')) return;

    this.projectService.removeMember(this.agencyId, projectId, this.userId, memberId).subscribe({
      next: (updated) => {
        const idx = this.projects.findIndex(p => p.id === updated.id);
        if (idx !== -1) this.projects[idx] = updated;
        if (this.selectedProject && this.selectedProject.id === updated.id) {
          this.selectedProject = updated;
        }
      },
      error: (err) => console.error(err)
    });
  }

  // --- Display Helpers ---
  
  getProgressBarColorClass(progress: number): string {
    if (progress === 100) return 'prog-success'; // Vert
    if (progress >= 30) return 'prog-active'; // Bleu/Indigo
    return 'prog-neutral'; // Gris
  }

  getStatusLabel(status: string): string {
    switch(status) {
      case ProjectStatus.EN_COURS: return 'En cours';
      case ProjectStatus.EN_ATTENTE: return 'En attente';
      case ProjectStatus.TERMINE: return 'Terminé';
      case ProjectStatus.ANNULE: return 'Annulé';
      default: return status;
    }
  }

  getStatusClass(status: string): string {
    switch(status) {
      case ProjectStatus.TERMINE: return 'badge--success';
      case ProjectStatus.EN_ATTENTE: return 'badge--warning';
      case ProjectStatus.ANNULE: return 'badge--secondary';
      case ProjectStatus.EN_COURS: return 'badge--primary';
      default: return 'badge--primary';
    }
  }

  getPriorityLabel(priority: string): string {
    switch(priority) {
      case ProjectPriority.HAUTE: return 'Haute';
      case ProjectPriority.MOYENNE: return 'Moyenne';
      case ProjectPriority.FAIBLE: return 'Faible';
      default: return priority || 'Moyenne';
    }
  }

  getPriorityClass(priority: string): string {
    switch(priority) {
      case ProjectPriority.HAUTE: return 'badge--danger';
      case ProjectPriority.MOYENNE: return 'badge--warning';
      case ProjectPriority.FAIBLE: return 'badge--success';
      default: return 'badge--secondary';
    }
  }

  isMemberAssigned(amId: number): boolean {
    if (!this.selectedProject || !this.selectedProject.assignedMembers) return false;
    return this.selectedProject.assignedMembers.some(sm => sm.memberId === amId);
  }

  /** Members that can be assigned to a project (LEAD excluded — they are the creator) */
  get assignableMembers(): AgencyMember[] {
    return this.agencyMembers.filter(m => m.userId !== this.userId && m.role !== 'LEAD');
  }
}
