import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AgencyMemberService } from '../../services/agency-member.service';
import { AgencyService } from '../../services/agency.service';
import { AgencyInvitationService } from '../../services/agency-invitation.service';
import { AgencyMember, MemberRole, Task, TaskStatus } from '../../../../core/models/agency.model';
import { AuthService } from '../../../../core/services/auth.service';
import { PublicUserDTO } from '../../../../core/models/user.model';
import { TaskService } from '../../services/task.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-agency-members',
  templateUrl: './agency-members.component.html',
  styleUrls: ['./agency-members.component.css']
})
export class AgencyMembersComponent implements OnInit, OnDestroy {
  @Input() agencyId!: number;
  members: AgencyMember[] = [];
  loading = true;
  
  // Roles & Auth
  currentUserId: number | null = null;
  isLead = false;

  // Invite Modal State
  showInviteModal = false;
  searchQuery = '';
  searchResults: PublicUserDTO[] = [];
  selectedUser: PublicUserDTO | null = null;
  inviteMessage = '';
  inviteLoading = false;
  inviteSuccess = '';
  inviteError = '';
  
  // Search state
  private searchSubject = new Subject<string>();
  private searchSubscription!: Subscription;

  constructor(
    private memberService: AgencyMemberService,
    private agencyService: AgencyService,
    private invitationService: AgencyInvitationService,
    private authService: AuthService,
    private taskService: TaskService
  ) {}

  ngOnInit(): void {
    const authUser = this.authService.getCurrentAuthUser();
    if (authUser) {
      this.currentUserId = authUser.userId || (authUser as any).id;
    }
    if (this.agencyId) {
      this.loadMembers();
    }

    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(email => {
      this.performSearch(email);
    });
  }

  ngOnDestroy(): void {
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
  }

  loadMembers(): void {
    this.loading = true;
    forkJoin({
      members: this.memberService.getMembersByAgency(this.agencyId),
      tasks: this.taskService.getTasksByAgency(this.agencyId)
    }).subscribe({
      next: ({ members, tasks }) => {
        this.members = members;
        this.calculateWorkload(tasks);
        // Check if current user is lead
        this.isLead = this.members.some(m => m.userId === this.currentUserId && m.role === 'LEAD');
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
      }
    });
  }

  calculateWorkload(tasks: Task[]): void {
    // Formulation: (assigned IN_PROGRESS tasks / total IN_PROGRESS tasks of agency) * 100
    const inProgressTasks = tasks.filter(t => t.status === TaskStatus.EN_COURS);
    const totalInProgress = inProgressTasks.length;

    if (totalInProgress === 0) {
      this.members.forEach(m => m.workloadScore = 0);
      return;
    }

    this.members.forEach(m => {
      const assignedCount = inProgressTasks.filter(t => t.assignedMember?.memberId === m.id).length;
      m.workloadScore = Math.round((assignedCount / totalInProgress) * 100);
    });
  }

  getRoleBadgeClass(role: string): string {
    return role === MemberRole.LEAD ? 'badge--danger' : 'badge--azure';
  }

  getStatusBadgeClass(status: string): string {
    return status === 'ACTIVE' ? 'badge--success' : 'badge--warning';
  }

  removeMember(memberId: number): void {
    if (confirm('Voulez-vous vraiment retirer ce membre de l\'agence ?')) {
      this.memberService.removeMember(memberId).subscribe({
        next: () => this.loadMembers(),
        error: (err) => alert(err.error?.message || 'Erreur lors de la suppression')
      });
    }
  }

  // --- ROLE UPDATE FLOW ---
  updateRole(userId: number, newRole: string): void {
    if (!this.currentUserId || !this.isLead) return;
    this.agencyService.updateMemberRole(this.agencyId, userId, newRole, this.currentUserId).subscribe({
      next: () => {
        this.loadMembers(); // Refresh UI instantly
      },
      error: (err) => {
        alert("Erreur: " + (err.error?.message || "Impossible de changer le rôle"));
      }
    });
  }

  // --- INVITE FLOW ---
  openInviteModal(): void {
    this.showInviteModal = true;
    this.searchQuery = '';
    this.searchResults = [];
    this.selectedUser = null;
    this.inviteMessage = '';
    this.inviteSuccess = '';
    this.inviteError = '';
  }

  closeInviteModal(): void {
    this.showInviteModal = false;
  }

  // Triggered on keyup
  searchUsers(): void {
    this.searchSubject.next(this.searchQuery);
  }

  performSearch(email: string): void {
    if (!email || email.length < 3 || !this.currentUserId) {
      this.searchResults = [];
      return;
    }
    
    this.agencyService.searchUsersByEmail(this.agencyId, email).subscribe({
      next: (res) => this.searchResults = res,
      error: (err) => {
        console.error(err);
        this.searchResults = [];
      }
    });
  }

  selectUser(user: PublicUserDTO): void {
    this.selectedUser = user;
    this.searchResults = [];
    this.searchQuery = user.email || '';
  }

  sendInvitation(): void {
    if (!this.selectedUser || !this.currentUserId) return;
    
    this.inviteLoading = true;
    this.inviteError = '';
    
    this.invitationService.createInvitation(this.agencyId, this.currentUserId, this.selectedUser.id, this.inviteMessage).subscribe({
      next: () => {
        this.inviteLoading = false;
        this.inviteSuccess = `Invitation envoyée à ${this.selectedUser?.fullName}`;
        setTimeout(() => {
          this.closeInviteModal();
          this.inviteSuccess = '';
        }, 2000);
      },
      error: (err) => {
        this.inviteLoading = false;
        this.inviteError = err.error?.message || "Erreur lors de l'envoi de l'invitation.";
      }
    });
  }
}
