import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { NavbarComponent } from './components/navbar/navbar.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { FooterComponent } from './components/footer/footer.component';
import { StatCardComponent } from './components/stat-card/stat-card.component';
import { SectionHeaderComponent } from './components/section-header/section-header.component';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';
import { NotificationBellComponent } from './components/notification-bell/notification-bell.component';
import { LoadingSpinnerComponent } from './components/loading-spinner/loading-spinner.component';
import { StatusCountPipe } from './pipes/status-count.pipe';

@NgModule({
  declarations: [
    NavbarComponent,
    SidebarComponent,
    FooterComponent,
    StatCardComponent,
    SectionHeaderComponent,
    EmptyStateComponent,
    NotificationBellComponent,
    LoadingSpinnerComponent,
    StatusCountPipe
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule
  ],
  exports: [
    NavbarComponent,
    SidebarComponent,
    FooterComponent,
    StatCardComponent,
    SectionHeaderComponent,
    EmptyStateComponent,
    NotificationBellComponent,
    LoadingSpinnerComponent,
    FormsModule,
    RouterModule,
    StatusCountPipe
  ]
})
export class SharedModule {}