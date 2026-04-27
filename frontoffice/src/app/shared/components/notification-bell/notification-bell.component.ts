import { Component, OnInit, OnDestroy, HostListener, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { ProjectApiService } from '../../../features/project/services/project-api.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectNotification } from '../../../features/project/models/project.models';

@Component({
  selector: 'app-notification-bell',
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.css']
})
export class NotificationBellComponent implements OnInit, OnDestroy {

  private notificationService = inject(NotificationService);
  private projectApi = inject(ProjectApiService);
  private authService = inject(AuthService);
  private router = inject(Router);

  // ── Notifications WebSocket/contrats/profil (existantes) ──
  wsCount = 0;
  hasPulse = false;
  isOpen = false;
  messages: any[] = [];

  // ── Notifications projet ──
  projectNotifications: ProjectNotification[] = [];
  projectUnreadCount = 0;

  private sub!: Subscription;
  private msgSub!: Subscription;

  get count(): number {
    return this.wsCount + this.projectUnreadCount;
  }

  ngOnInit(): void {
    this.sub = this.notificationService.count$.subscribe((n: number) => {
      this.wsCount = n;
      this.hasPulse = this.count > 0;
    });

    this.msgSub = this.notificationService.messages$.subscribe((msgs: any[]) => {
      this.messages = msgs;
    });

    this.loadProjectNotifications();
  }

  private loadProjectNotifications(): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (!userId) return;

    this.projectApi.getUnreadNotifications(userId).subscribe({
      next: (notifs) => {
        this.projectNotifications = notifs;
        this.projectUnreadCount = notifs.length;
        this.hasPulse = this.count > 0;
      },
      error: () => {}
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.msgSub?.unsubscribe();
  }

  get displayCount(): string {
    return this.count > 99 ? '99+' : String(this.count);
  }

  togglePanel(event: Event): void {
    event.stopPropagation();
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.notificationService.resetCount();
    }
  }

  onNotifClick(msg: any): void {
    this.isOpen = false;

    // Redirection dynamique si le backend (ex: contrats/litiges) fournit une URL
    if (msg.relatedUrl) {
      // Nettoyer l'URL si elle vient du backend
      const url = msg.relatedUrl.startsWith('/') ? msg.relatedUrl : '/' + msg.relatedUrl;
      this.router.navigateByUrl(url);
      return;
    }

    try {
      // Extraire le payload JSON si disponible
      const payload = msg.payload
        ? (typeof msg.payload === 'string' ? JSON.parse(msg.payload) : msg.payload)
        : null;

    switch (msg.type) {
      case 'NEW_REVIEW':
        // Naviguer vers la page avis du profil connecté
        this.router.navigate(['/app/profile/reviews']);
        break;

      case 'NEW_ENDORSEMENT':
        // Naviguer vers la page endorsements
        this.router.navigate(['/app/profile/endorsements']);
        break;

      case 'PROFILE_VIEW':
        // Naviguer vers la vue générale du profil
        this.router.navigate(['/app/profile/overview']);
        break;

      case 'NEW_REPORT':
      case 'REPORT_STATUS_UPDATED':
      case 'PROFILE_SUSPENDED':
        // Notifications admin — naviguer vers le dashboard
        this.router.navigate(['/app/dashboard']);
        break;

      default:
        this.router.navigate(['/app/dashboard']);
    }
  } catch (e) {
    // En cas d'erreur de parsing — rester sur le dashboard
    this.router.navigate(['/app/dashboard']);
  }
}

  // ── Notifications projet ──────────────────────────────────────

  onProjectNotifClick(notif: ProjectNotification): void {
    this.isOpen = false;
    this.projectApi.markAsRead(notif.id).subscribe({ error: () => {} });
    notif.read = true;
    this.projectUnreadCount = this.projectNotifications.filter(n => !n.read).length;

    if (!notif.projectId) { this.router.navigate(['/app/projects']); return; }
    switch (notif.type) {
      case 'RISK_DETECTED':         this.router.navigate(['/app/projects', notif.projectId, 'risks']); break;
      case 'DELIVERABLE_PENDING':
      case 'DELIVERABLE_REVIEWED':  this.router.navigate(['/app/projects', notif.projectId, 'deliverables']); break;
      case 'TASK_BLOCKED':
      case 'DEADLINE_24H':          this.router.navigate(['/app/projects', notif.projectId, 'kanban']); break;
      default:                      this.router.navigate(['/app/projects', notif.projectId]);
    }
  }

  getProjectIcon(type: string): string {
    return ({
      DEADLINE_24H:         'fas fa-clock',
      DELIVERABLE_PENDING:  'fas fa-file-circle-question',
      DELIVERABLE_REVIEWED: 'fas fa-file-circle-check',
      RISK_DETECTED:        'fas fa-triangle-exclamation',
      TASK_BLOCKED:         'fas fa-lock',
      WEEKLY_REPORT:        'fas fa-chart-pie'
    } as Record<string, string>)[type] ?? 'fas fa-diagram-project';
  }

  getProjectIconColor(type: string): string {
    return ({
      DEADLINE_24H:         '#ef4444',
      DELIVERABLE_PENDING:  '#f59e0b',
      DELIVERABLE_REVIEWED: '#10b981',
      RISK_DETECTED:        '#a855f7',
      TASK_BLOCKED:         '#ef4444',
      WEEKLY_REPORT:        '#3b82f6'
    } as Record<string, string>)[type] ?? '#6366f1';
  }

  @HostListener('document:click')
  onClickOutside(): void {
    this.isOpen = false;
  }

  getIcon(type: string): string {
    switch (type) {
      // ms-profile
      case 'NEW_REVIEW':      return 'fas fa-star';
      case 'NEW_ENDORSEMENT': return 'fas fa-thumbs-up';
      case 'NEW_REPORT':      return 'fas fa-flag';
      case 'PROFILE_VIEW':    return 'fas fa-eye';
      // ms-contract-servicee
      case 'SUCCESS':         return 'fas fa-check-circle';
      case 'WARNING':         return 'fas fa-exclamation-triangle';
      case 'URGENT':          return 'fas fa-circle-exclamation';
      case 'INFO':            return 'fas fa-info-circle';
      default:                return 'fas fa-bell';
    }
  }

  getIconColor(type: string): string {
    switch (type) {
      // ms-profile
      case 'NEW_REVIEW':      return '#f59e0b'; // amber
      case 'NEW_ENDORSEMENT': return '#3b82f6'; // blue
      case 'NEW_REPORT':      return '#ef4444'; // red
      case 'PROFILE_VIEW':    return '#10b981'; // green
      // ms-contract-servicee
      case 'SUCCESS':         return '#10b981'; // green
      case 'WARNING':         return '#f59e0b'; // amber
      case 'URGENT':          return '#ef4444'; // red
      case 'INFO':            return '#3b82f6'; // blue
      default:                return '#8b92a5'; // gray
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const diffMs = new Date().getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);

    if (diffMin < 1) return 'À l\'instant';
    if (diffMin < 60) return `Il y a ${diffMin} min`;

    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `Il y a ${diffH}h`;

    return `Il y a ${Math.floor(diffH / 24)}j`;
  }
}