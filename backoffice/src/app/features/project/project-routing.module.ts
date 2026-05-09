import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AdminDashboardComponent } from './pages/admin/admin-dashboard/admin-dashboard.component';
import { AdminProjectsComponent } from './pages/admin/admin-projects/admin-projects.component';
import { AdminTasksComponent } from './pages/admin/admin-tasks/admin-tasks.component';
import { AdminDeliverablesComponent } from './pages/admin/admin-deliverables/admin-deliverables.component';
import { AdminRisksComponent } from './pages/admin/admin-risks/admin-risks.component';
import { AdminReportsComponent } from './pages/admin/admin-reports/admin-reports.component';
import { AdminDetailComponent } from './pages/admin/admin-detail/admin-detail.component';
import { AdminMlPredictionComponent } from './pages/admin/admin-ml-prediction/admin-ml-prediction.component';
import { AdminBurndownComponent } from './pages/admin/admin-burndown/admin-burndown.component';

const routes: Routes = [
  // /admin/projects → Dashboard Module 08
  { path: '',                    component: AdminDashboardComponent },

  // /admin/projects/admin/...
  { path: 'admin/list',          component: AdminProjectsComponent },
  { path: 'admin/tasks',         component: AdminTasksComponent },
  { path: 'admin/deliverables',  component: AdminDeliverablesComponent },
  { path: 'admin/risks',         component: AdminRisksComponent },
  { path: 'admin/reports',       component: AdminReportsComponent },
  { path: 'admin/detail/:id',    component: AdminDetailComponent },
  { path: 'admin/ml-prediction', component: AdminMlPredictionComponent },
  { path: 'admin/burndown',      component: AdminBurndownComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProjectRoutingModule {}
