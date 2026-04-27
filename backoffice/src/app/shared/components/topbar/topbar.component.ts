import { Component, Input, Output, EventEmitter } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.css']
})
export class TopbarComponent {
  @Input()  sidebarCollapsed = false;
  @Output() toggleSidebar = new EventEmitter<void>();

  currentDate = new Date();

  // On injecte AuthService au lieu de Router directement
  constructor(private authService: AuthService) {}

  onToggle() {
    this.toggleSidebar.emit();
  }

  onLogout() {
    // Délègue au AuthService qui fait localStorage.clear() + window.location.href
    this.authService.logout();
  }
}