import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AgenciesRoutingModule } from './agencies-routing.module';
import { SharedModule } from '../../shared/shared.module';

import { AgencyListComponent } from './pages/agency-list/agency-list.component';
import { AgencyDetailComponent } from './pages/agency-detail/agency-detail.component';
import { AgencyFormComponent } from './components/agency-form/agency-form.component';

// Sub-components for detail tabs
import { AgencyMembersComponent } from './components/agency-members/agency-members.component';
import { TeamProjectsComponent } from './components/team-projects/team-projects.component';
import { TaskKanbanComponent } from './components/task-kanban/task-kanban.component';
import { PerformanceDashboardComponent } from './components/performance-dashboard/performance-dashboard.component';
import { CollaborationLogsComponent } from './components/collaboration-logs/collaboration-logs.component';
import { AgencyInvitationsComponent } from './components/agency-invitations/agency-invitations.component';
import { MyInvitationsComponent } from './pages/my-invitations/my-invitations.component';

@NgModule({
  declarations: [
    AgencyListComponent,
    AgencyDetailComponent,
    AgencyFormComponent,
    AgencyMembersComponent,
    TeamProjectsComponent,
    TaskKanbanComponent,
    PerformanceDashboardComponent,
    CollaborationLogsComponent,
    AgencyInvitationsComponent,
    MyInvitationsComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    AgenciesRoutingModule,
    SharedModule
  ]
})
export class AgenciesModule {
  constructor() {
    console.log('AgenciesModule loaded!');
  }
}
// End of Agencies Module
