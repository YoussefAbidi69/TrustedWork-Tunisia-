import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AdminJobsComponent } from './pages/admin-jobs/admin-jobs.component';
import { AdminFraudComponent } from './pages/admin-fraud/admin-fraud.component';
import { AdminAnalyticsComponent } from './pages/admin-analytics/admin-analytics.component';
import { AdminApplicationsComponent } from './pages/admin-applications/admin-applications.component';
import { CountUpDirective } from '../../shared/directives/count-up.directive';
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';

@NgModule({
  declarations: [
    AdminJobsComponent,
    AdminFraudComponent,
    AdminAnalyticsComponent,
    AdminApplicationsComponent
  ],
  imports: [CommonModule, FormsModule, RouterModule, CountUpDirective, TimeAgoPipe],
  exports: [
    AdminJobsComponent,
    AdminFraudComponent,
    AdminAnalyticsComponent,
    AdminApplicationsComponent
  ]
})
export class JobBoardAdminModule {}
