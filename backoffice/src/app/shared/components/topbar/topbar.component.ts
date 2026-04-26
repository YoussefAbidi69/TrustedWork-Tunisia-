import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/notification.service';
import { Notification } from '../../../core/models/notification.model';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.css']
})
export class TopbarComponent implements OnInit, OnDestroy {
  @Input()  sidebarCollapsed = false;
  @Output() toggleSidebar = new EventEmitter<void>();

  currentDate = new Date();
  
  notifications: Notification[] = [];
  unreadCount = 0;
  isOpen = false;
  hasPulse = true;
  private pollSubscription?: Subscription;

  // On injecte AuthService au lieu de Router directement
  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.fetchUnread();
    this.pollSubscription = interval(30000).subscribe(() => {
      this.fetchUnread();
    });
  }

  ngOnDestroy(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
  }

  fetchUnread(): void {
    this.notificationService.getUnreadNotifications().subscribe({
      next: (data) => {
        this.notifications = data;
        this.unreadCount = data.length;
        this.hasPulse = this.unreadCount > 0;
      },
      error: () => {}
    });
  }

  toggleDropdown(event: Event): void {
    event.stopPropagation();
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.fetchUnread();
    }
  }

  @HostListener('document:click')
  closeDropdown(): void {
    this.isOpen = false;
  }

  onNotificationClick(notif: Notification, event: Event): void {
    event.stopPropagation();
    this.notificationService.markAsRead(notif.id).subscribe(() => {
      this.notifications = this.notifications.filter(n => n.id !== notif.id);
      this.unreadCount = this.notifications.length;
      if (notif.relatedUrl) {
        this.router.navigateByUrl(notif.relatedUrl);
      }
      this.isOpen = false;
    });
  }

  markAllAsRead(event: Event): void {
    event.stopPropagation();
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications = [];
      this.unreadCount = 0;
    });
  }

  get displayCount(): string {
    return this.unreadCount > 99 ? '99+' : `${this.unreadCount}`;
  }

  onToggle() {
    this.toggleSidebar.emit();
  }

  onLogout() {
    // Délègue au AuthService qui fait localStorage.clear() + window.location.href
    this.authService.logout();
  }
}