import { Component, OnInit, OnDestroy, HostListener, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-notification-bell',
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.css']
})
export class NotificationBellComponent implements OnInit, OnDestroy {

  private notificationService = inject(NotificationService);
  private router = inject(Router);

  count = 0;
  hasPulse = false;
  isOpen = false;
  messages: any[] = [];

  private sub!: Subscription;
  private msgSub!: Subscription;

  ngOnInit(): void {
    this.sub = this.notificationService.count$.subscribe((n: number) => {
      this.count = n;
      this.hasPulse = n > 0;
    });

    this.msgSub = this.notificationService.messages$.subscribe((msgs: any[]) => {
      this.messages = msgs;
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

  @HostListener('document:click')
  onClickOutside(): void {
    this.isOpen = false;
  }

  getIcon(type: string): string {
    switch (type) {
      case 'NEW_REVIEW':      return 'fas fa-star';
      case 'NEW_ENDORSEMENT': return 'fas fa-thumbs-up';
      case 'NEW_REPORT':      return 'fas fa-flag';
      case 'PROFILE_VIEW':    return 'fas fa-eye';
      default:                return 'fas fa-bell';
    }
  }

  getIconColor(type: string): string {
    switch (type) {
      case 'NEW_REVIEW':      return '#f59e0b';
      case 'NEW_ENDORSEMENT': return '#3b82f6';
      case 'NEW_REPORT':      return '#ef4444';
      case 'PROFILE_VIEW':    return '#10b981';
      default:                return '#8b92a5';
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