import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ProjectListComponent } from './pages/project-list/project-list.component';
import { ProjectDetailComponent } from './pages/project-detail/project-detail.component';
import { KanbanBoardComponent } from './pages/kanban-board/kanban-board.component';
import { DeliverablesComponent } from './pages/deliverables/deliverables.component';
import { RiskSignalsComponent } from './pages/risk-signals/risk-signals.component';
import { NotificationsComponent } from './pages/notifications/notifications.component';
import { GanttChartComponent } from './pages/gantt-chart/gantt-chart.component';

const routes: Routes = [
  { path: '',                    component: ProjectListComponent },
  { path: 'notifications',      component: NotificationsComponent },
  { path: ':id',                 component: ProjectDetailComponent },
  { path: ':id/kanban',          component: KanbanBoardComponent },
  { path: ':id/deliverables',    component: DeliverablesComponent },
  { path: ':id/risks',           component: RiskSignalsComponent },
  { path: ':id/gantt', component: GanttChartComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProjectRoutingModule {}