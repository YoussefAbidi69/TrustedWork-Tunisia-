import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectRoutingModule } from './project-routing.module';

// ── Admin Components ──
import { AdminDashboardComponent } from './pages/admin/admin-dashboard/admin-dashboard.component';
import { AdminProjectsComponent } from './pages/admin/admin-projects/admin-projects.component';
import { AdminTasksComponent } from './pages/admin/admin-tasks/admin-tasks.component';
import { AdminDeliverablesComponent } from './pages/admin/admin-deliverables/admin-deliverables.component';
import { AdminRisksComponent } from './pages/admin/admin-risks/admin-risks.component';
import { AdminReportsComponent } from './pages/admin/admin-reports/admin-reports.component';
import { AdminDetailComponent } from './pages/admin/admin-detail/admin-detail.component';
import { AdminMlPredictionComponent } from './pages/admin/admin-ml-prediction/admin-ml-prediction.component';

@NgModule({
  declarations: [
    AdminDashboardComponent,
    AdminProjectsComponent,
    AdminTasksComponent,
    AdminDeliverablesComponent,
    AdminRisksComponent,
    AdminReportsComponent,
    AdminDetailComponent,
    AdminMlPredictionComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ProjectRoutingModule
  ]
})
export class ProjectModule {}
