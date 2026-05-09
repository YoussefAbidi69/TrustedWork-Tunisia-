import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

export interface NotifItem {
  id: number;
  message: string;
  time: Date;
  read: boolean;
  type: 'NEW_REPORT' | 'REPORT_STATUS_UPDATED' | 'PROFILE_SUSPENDED' | 'NEW_ENDORSEMENT' | 'OTHER';
  createdAt?: string;
}

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.css']
})
export class TopbarComponent implements OnInit, OnDestroy {

  @Input()  sidebarCollapsed = false;
  @Output() toggleSidebar = new EventEmitter<void>();

  currentDate = new Date();
  showDropdown = false;

  notifications: NotifItem[] = [];
  private sub!: Subscription;
  private countSub!: Subscription;
  private idCounter = 0;

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    // Connexion WebSocket + chargement MySQL persisté
    this.notificationService.connect();

    // Écouter le flux de messages
    this.sub = this.notificationService.messages$.subscribe((msgs: any[]) => {
      this.notifications = msgs.map((msg, i) => ({
        id:        msg.id ?? ++this.idCounter,
        message:   msg.message || this.getLabelFromType(msg.type),
        time:      msg.createdAt ? new Date(msg.createdAt) : new Date(),
        read:      msg.read ?? false,
        type:      msg.type || 'OTHER',
        createdAt: msg.createdAt
      }));
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.countSub?.unsubscribe();
  }

  get unreadCount(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  toggleDropdown(): void {
    this.showDropdown = !this.showDropdown;
    if (this.showDropdown) {
      this.markAllRead();
      this.notificationService.resetCount();
    }
  }

  markAllRead(): void {
    this.notifications.forEach(n => n.read = true);
  }

  clearAll(): void {
    this.notifications = [];
    this.notificationService.resetCount();
  }

  getNotifIcon(type: string): string {
    switch (type) {
      case 'NEW_REPORT':            return 'fas fa-flag';
      case 'REPORT_STATUS_UPDATED': return 'fas fa-arrows-rotate';
      case 'PROFILE_SUSPENDED':     return 'fas fa-ban';
      case 'NEW_ENDORSEMENT':       return 'fas fa-thumbs-up';
      default:                      return 'fas fa-bell';
    }
  }

  getNotifColor(type: string): string {
    switch (type) {
      case 'NEW_REPORT':        return '#ef4444';
      case 'PROFILE_SUSPENDED': return '#f97316';
      case 'NEW_ENDORSEMENT':   return '#3b82f6';
      default:                  return '#6366f1';
    }
  }

  formatTime(date: Date | string): string {
    const d = new Date(date);
    const diffMs  = Date.now() - d.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1)  return 'À l\'instant';
    if (diffMin < 60) return `Il y a ${diffMin} min`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24)   return `Il y a ${diffH}h`;
    return `Il y a ${Math.floor(diffH / 24)}j`;
  }

  private getLabelFromType(type: string): string {
    const labels: Record<string, string> = {
      'NEW_REPORT':            'Nouveau signalement reçu',
      'REPORT_STATUS_UPDATED': 'Statut d\'un signalement mis à jour',
      'PROFILE_SUSPENDED':     'Profil suspendu automatiquement',
      'NEW_ENDORSEMENT':       'Nouvelle validation de compétence',
    };
    return labels[type] || 'Mise à jour en temps réel';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!(event.target as HTMLElement).closest('.notif-wrapper')) {
      this.showDropdown = false;
    }
  }

  onToggle(): void { this.toggleSidebar.emit(); }

  onLogout(): void { this.authService.logout(); }
}