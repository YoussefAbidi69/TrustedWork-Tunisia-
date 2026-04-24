import { Component, HostBinding } from '@angular/core';

@Component({
  selector: 'app-dashboard-layout',
  templateUrl: './dashboard-layout.component.html',
  styleUrls: ['./dashboard-layout.component.css']
})
export class DashboardLayoutComponent {
  isSidebarCollapsed = true;

  @HostBinding('class.sidebar-collapsed')
  get sidebarCollapsedClass(): boolean {
    return this.isSidebarCollapsed;
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }
}