import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AgencyService } from '../../services/agency.service';
import { Agency, AgencyMembershipSummary, AgencyContextDto } from '../../../../core/models/agency.model';
import { AuthService } from '../../../../core/services/auth.service';

import { TeamProjectService } from '../../services/team-project.service';
import { TaskService } from '../../services/task.service';

@Component({
  selector: 'app-agency-list',
  templateUrl: './agency-list.component.html',
  styleUrls: ['./agency-list.component.css']
})
export class AgencyListComponent implements OnInit {
  agencies: Agency[] = [];
  memberships: AgencyMembershipSummary[] = [];
  
  loading = true;
  error: string | null = null;
  userId: number | null = null;

  constructor(
    private agencyService: AgencyService,
    private authService: AuthService,
    private router: Router,
    private teamProjectService: TeamProjectService,
    private taskService: TaskService
  ) {}

  ngOnInit(): void {
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.userId = authUser.userId;
      this.loadData();
    }
  }

  loadData(): void {
    if (!this.userId) return;
    this.loading = true;

    forkJoin({
      all: this.agencyService.getAllAgencies(),
      context: this.agencyService.getMyAgencyContext(this.userId)
    }).subscribe({
      next: (results) => {
        this.agencies = results.all;
        this.memberships = results.context.hasMemberships ? results.context.memberships : [];
        this.loadAgencyStats();
      },
      error: (err) => {
        this.error = "Erreur lors du chargement des agences.";
        this.loading = false;
        console.error(err);
      }
    });
  }

  agencyStats: { [agencyId: number]: { hasUnfinishedProjects: boolean, hasUnfinishedTasksForUser: boolean } | undefined } = {};

  // For the delete modal
  showDeleteConfirm = false;
  agencyToDelete: Agency | null = null;
  deleting = false;


  loadAgencyStats(): void {
    if (this.memberships.length === 0) {
      this.loading = false;
      return;
    }

    const statRequests: any = {};

    this.memberships.forEach(m => {
      if (m.role === 'LEAD') {
        statRequests[`proj_${m.agencyId}`] = this.teamProjectService.getProjectsByAgency(m.agencyId);
      } else if (m.role === 'MEMBER') {
        statRequests[`task_${m.agencyId}`] = this.taskService.getTasksByAgency(m.agencyId);
      }
    });

    if (Object.keys(statRequests).length === 0) {
      this.loading = false;
      return;
    }

    forkJoin(statRequests).subscribe({
      next: (results: any) => {
        this.memberships.forEach(m => {
          this.agencyStats[m.agencyId] = { hasUnfinishedProjects: false, hasUnfinishedTasksForUser: false };
          
          if (m.role === 'LEAD') {
            const projects = results[`proj_${m.agencyId}`] as any[];
            if (projects) {
              this.agencyStats[m.agencyId]!.hasUnfinishedProjects = projects.some(p => p.status !== 'TERMINE' && p.status !== 'ANNULE');
            }
          } else if (m.role === 'MEMBER') {
            const tasks = results[`task_${m.agencyId}`] as any[];
            if (tasks) {
              this.agencyStats[m.agencyId]!.hasUnfinishedTasksForUser = tasks.some(t => 
                (t.assignedMember?.userId === this.userId || t.assignedMemberId === this.userId) && 
                t.status !== 'TERMINE' && t.status !== 'ANNULE'
              );
            }
          }
        });
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading stats', err);
        this.loading = false;
      }
    });
  }

  getUserRole(agencyId: number): string | null {
    const membership = this.memberships.find(m => m.agencyId === agencyId);
    return membership ? membership.role : null;
  }
  
  getMembershipDate(agencyId: number): string | null {
    const membership = this.memberships.find(m => m.agencyId === agencyId);
    return membership ? membership.joinedAt : null;
  }

  // --- Actions ---

  quitAgency(agency: Agency): void {
    if (!this.userId) return;
    if (this.agencyStats[agency.id]?.hasUnfinishedTasksForUser) return;
    
    if (confirm(`Êtes-vous sûr de vouloir quitter l'agence ${agency.name} ?`)) {
      this.agencyService.quitAgency(agency.id, this.userId).subscribe({
        next: () => {
          // Remove from memberships list visually
          this.memberships = this.memberships.filter(m => m.agencyId !== agency.id);
          // Re-evaluate roles and UI (since getUserRole checks memberships)
          alert(`Vous avez quitté l'agence ${agency.name} avec succès.`);
        },
        error: (err) => {
          console.error(err);
          const msg = err?.error?.message || 'Une erreur est survenue lors du départ de l\'agence.';
          alert(msg);
        }
      });
    }
  }

  confirmDeleteAgency(agency: Agency): void {
    if (this.agencyStats[agency.id]?.hasUnfinishedProjects) return;
    this.agencyToDelete = agency;
    this.showDeleteConfirm = true;
  }

  deleteAgency(): void {
    if (!this.agencyToDelete || !this.userId) return;
    this.deleting = true;
    this.agencyService.deleteAgency(this.agencyToDelete.id, this.userId).subscribe({
      next: () => {
        this.agencies = this.agencies.filter(a => a.id !== this.agencyToDelete!.id);
        this.showDeleteConfirm = false;
        this.agencyToDelete = null;
        this.deleting = false;
        // Also remove from memberships
        this.memberships = this.memberships.filter(m => m.agencyId !== this.agencyToDelete?.id);
      },
      error: (err) => {
        console.error(err);
        this.deleting = false;
        this.showDeleteConfirm = false;
      }
    });
  }
}
