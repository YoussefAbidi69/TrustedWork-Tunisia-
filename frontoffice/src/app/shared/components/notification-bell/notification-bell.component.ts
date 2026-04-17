import { Component, OnInit, OnDestroy } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { ProjectApiService } from '../../../features/project/services/project-api.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-notification-bell',
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.css']
})
export class NotificationBellComponent implements OnInit, OnDestroy {

  count = 0;
  hasPulse = false;
  private sub!: Subscription;
  private userId = 0;

  constructor(
    private projectApi: ProjectApiService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    this.userId = user?.userId || 0;
    if (!this.userId) return;

    // Charger immédiatement puis toutes les 30 secondes
    this.loadCount();
    this.sub = interval(30000).subscribe(() => this.loadCount());
  }

  private loadCount(): void {
    this.projectApi.getUnreadCount(this.userId).subscribe({
      next: (n) => {
        this.count = n;
        this.hasPulse = n > 0;
      },
      error: () => {}
    });
  }

  get displayCount(): string {
    return this.count > 99 ? '99+' : `${this.count}`;
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}