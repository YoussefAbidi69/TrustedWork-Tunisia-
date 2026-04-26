import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService } from '../../../core/services/notification.service';
import { UserService } from '../../../core/services/user.service';
import { Notification } from '../../../core/models/notification.model';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-notification-bell',
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.css']
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount = 0;
  isOpen = false;
  hasPulse = true;
  private pollSubscription?: Subscription;
  private userCin?: string;

  constructor(
    private notificationService: NotificationService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // 1. Récupérer le vrai CIN de l'utilisateur
    this.userService.getCurrentDashboardUser().subscribe({
      next: (user) => {
        if (user && user.cin) {
          this.userCin = user.cin.toString();
          
          // 2. Fetch initial
          this.fetchUnread();
          
          // 3. Polling toutes les 30 secondes
          this.pollSubscription = interval(30000).subscribe(() => {
            this.fetchUnread();
          });
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
  }

  fetchUnread(): void {
    if (!this.userCin) return;
    this.notificationService.getUnreadNotifications(this.userCin).subscribe({
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
    if (!this.userCin) return;
    this.notificationService.markAsRead(notif.id, this.userCin).subscribe(() => {
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
    if (!this.userCin) return;
    this.notificationService.markAllAsRead(this.userCin).subscribe(() => {
      this.notifications = [];
      this.unreadCount = 0;
    });
  }

  get displayCount(): string {
    return this.unreadCount > 99 ? '99+' : `${this.unreadCount}`;
  }
}