import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { SharedModule } from '../../shared/shared.module';
import { ProjectRoutingModule } from './project-routing.module';

import { ProjectListComponent } from './pages/project-list/project-list.component';
import { ProjectDetailComponent } from './pages/project-detail/project-detail.component';
import { KanbanBoardComponent } from './pages/kanban-board/kanban-board.component';
import { DeliverablesComponent } from './pages/deliverables/deliverables.component';
import { RiskSignalsComponent } from './pages/risk-signals/risk-signals.component';
import { NotificationsComponent } from './pages/notifications/notifications.component';
import { GanttChartComponent } from './pages/gantt-chart/gantt-chart.component';
import { BurndownComponent } from './pages/burndown/burndown.component';

@NgModule({
  declarations: [
    ProjectListComponent,
    ProjectDetailComponent,
    KanbanBoardComponent,
    DeliverablesComponent,
    RiskSignalsComponent,
    NotificationsComponent,
    GanttChartComponent,
    BurndownComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    DragDropModule,
    SharedModule,
    ProjectRoutingModule
  ]
})
export class ProjectModule {}