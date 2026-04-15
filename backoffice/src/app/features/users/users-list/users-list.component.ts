import { Component, OnInit } from '@angular/core';
import { UserService, UserDTO } from '../../../core/services/user.service';

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent implements OnInit {
  users: UserDTO[] = [];
  filteredUsers: UserDTO[] = [];
  loading = true;
  searchQuery = '';
  selectedRole = 'ALL';
  selectedStatus = 'ALL';
  actionLoading: number | null = null;

  roles = ['ALL', 'FREELANCER', 'CLIENT', 'ADMIN'];
  statuses = ['ALL', 'ACTIVE', 'SUSPENDED'];

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.applyFilters();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur chargement users:', error);
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredUsers = this.users.filter((u) => {
      const query = this.searchQuery.trim().toLowerCase();

      const matchSearch =
        !query ||
        u.firstName?.toLowerCase().includes(query) ||
        u.lastName?.toLowerCase().includes(query) ||
        u.email?.toLowerCase().includes(query);

      const matchRole = this.selectedRole === 'ALL' || u.role === this.selectedRole;
      const matchStatus =
        this.selectedStatus === 'ALL' || u.accountStatus === this.selectedStatus;

      return matchSearch && matchRole && matchStatus;
    });
  }

  onSearch(): void {
    this.applyFilters();
  }

  onRoleFilter(role: string): void {
    this.selectedRole = role;
    this.applyFilters();
  }

  onStatusFilter(status: string): void {
    this.selectedStatus = status;
    this.applyFilters();
  }

  suspendUser(user: UserDTO): void {
    const reason = prompt(
      `Motif de suspension pour ${user.firstName} ${user.lastName} :`
    );

    if (!reason || !reason.trim()) {
      return;
    }

    this.actionLoading = user.id;

    this.userService.suspendUser(user.id, reason.trim()).subscribe({
      next: () => {
        this.loadUsers();
        this.actionLoading = null;
      },
      error: (error) => {
        console.error('Erreur suspension:', error);
        this.actionLoading = null;
      }
    });
  }

  activateUser(user: UserDTO): void {
    this.actionLoading = user.id;

    this.userService.liftSuspension(user.id).subscribe({
      next: () => {
        this.loadUsers();
        this.actionLoading = null;
      },
      error: (error) => {
        console.error('Erreur activation:', error);
        this.actionLoading = null;
      }
    });
  }

  getInitials(u: UserDTO): string {
    return `${u.firstName?.[0] || ''}${u.lastName?.[0] || ''}`.toUpperCase();
  }

  getAvatarClass(role: string): string {
    const map: Record<string, string> = {
      FREELANCER: 'accent',
      CLIENT: 'success',
      ADMIN: 'danger'
    };
    return map[role] || 'accent';
  }

  getRoleBadge(role: string): string {
    const map: Record<string, string> = {
      FREELANCER: 'badge-accent',
      CLIENT: 'badge-success',
      ADMIN: 'badge-danger'
    };
    return map[role] || 'badge-muted';
  }

  getStatusBadge(status: string): string {
    return status === 'ACTIVE' ? 'badge-success' : 'badge-danger';
  }

  getKycBadge(kyc: string): string {
    const map: Record<string, string> = {
      APPROVED: 'badge-success',
      PENDING: 'badge-warning',
      REJECTED: 'badge-danger'
    };
    return map[kyc] || 'badge-muted';
  }
}