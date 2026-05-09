import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProjectApiService } from '../../services/project-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ProjectNotification, NotificationType } from '../../models/project.models';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css']
})
export class NotificationsComponent implements OnInit {

  notifications: ProjectNotification[] = [];
  loading = true;
  error = '';
  userId = 0;

  activeFilter: 'ALL' | 'UNREAD' | 'READ' = 'ALL';

  constructor(
    private router: Router,
    private projectApi: ProjectApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    this.userId = user?.userId || 0;
    this.loadNotifications();
  }

  private loadNotifications(): void {
    this.loading = true;
    this.projectApi.getNotifications(this.userId).subscribe({
      next: (data) => {
        this.notifications = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les notifications.';
        this.loading = false;
      }
    });
  }

  // ══════════════════════════════════════
  // FILTRES
  // ══════════════════════════════════════

  get filteredNotifications(): ProjectNotification[] {
    if (this.activeFilter === 'UNREAD') return this.notifications.filter(n => !n.read);
    if (this.activeFilter === 'READ') return this.notifications.filter(n => n.read);
    return this.notifications;
  }

  get unreadCount(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  setFilter(f: 'ALL' | 'UNREAD' | 'READ'): void {
    this.activeFilter = f;
  }

  // ══════════════════════════════════════
  // ACTIONS
  // ══════════════════════════════════════

  markAsRead(notif: ProjectNotification): void {
    if (notif.read) return;
    this.projectApi.markAsRead(notif.id).subscribe({
      next: () => {
        notif.read = true;
      }
    });
  }

  markAllAsRead(): void {
    this.projectApi.markAllAsRead(this.userId).subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
      }
    });
  }

  goToProject(notif: ProjectNotification): void {
    this.markAsRead(notif);
    if (notif.projectId) {
      if (notif.type === 'RISK_DETECTED') {
        this.router.navigate(['/app/projects', notif.projectId, 'risks']);
      } else if (notif.type === 'DELIVERABLE_PENDING' || notif.type === 'DELIVERABLE_REVIEWED') {
        this.router.navigate(['/app/projects', notif.projectId, 'deliverables']);
      } else if (notif.type === 'TASK_BLOCKED' || notif.type === 'DEADLINE_24H') {
        this.router.navigate(['/app/projects', notif.projectId, 'kanban']);
      } else {
        this.router.navigate(['/app/projects', notif.projectId]);
      }
    }
  }

  // ══════════════════════════════════════
  // HELPERS
  // ══════════════════════════════════════

  getIcon(type: NotificationType): string {
    return {
      DEADLINE_24H: 'fa-clock',
      DELIVERABLE_PENDING: 'fa-file-circle-question',
      DELIVERABLE_REVIEWED: 'fa-file-circle-check',
      RISK_DETECTED: 'fa-triangle-exclamation',
      TASK_BLOCKED: 'fa-lock',
      WEEKLY_REPORT: 'fa-chart-pie'
    }[type] || 'fa-bell';
  }

  getIconClass(type: NotificationType): string {
    return {
      DEADLINE_24H: 'nicon--red',
      DELIVERABLE_PENDING: 'nicon--amber',
      DELIVERABLE_REVIEWED: 'nicon--green',
      RISK_DETECTED: 'nicon--purple',
      TASK_BLOCKED: 'nicon--red',
      WEEKLY_REPORT: 'nicon--blue'
    }[type] || 'nicon--gray';
  }

  getTypeLabel(type: NotificationType): string {
    return {
      DEADLINE_24H: 'Deadline',
      DELIVERABLE_PENDING: 'Livrable en attente',
      DELIVERABLE_REVIEWED: 'Livrable reviewé',
      RISK_DETECTED: 'Risque IA',
      TASK_BLOCKED: 'Tâche bloquée',
      WEEKLY_REPORT: 'Rapport hebdo'
    }[type] || type;
  }

  formatDate(date: string): string {
    const d = new Date(date);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    const diffH = Math.floor(diffMin / 60);
    const diffD = Math.floor(diffH / 24);

    if (diffMin < 1) return 'À l\'instant';
    if (diffMin < 60) return 'Il y a ' + diffMin + ' min';
    if (diffH < 24) return 'Il y a ' + diffH + 'h';
    if (diffD < 7) return 'Il y a ' + diffD + 'j';
    return d.toLocaleDateString('fr-TN', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}