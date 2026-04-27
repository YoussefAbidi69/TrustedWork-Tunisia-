import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-notification-bell',
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.css']
})
export class NotificationBellComponent {
  @Input() count = 3;
  @Input() hasPulse = true;

  get displayCount(): string {
    return this.count > 99 ? '99+' : `${this.count}`;
  }
}