import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { JobBoardShellComponent } from './job-board-shell/job-board-shell.component';
import { MarketplaceComponent } from './pages/marketplace/marketplace.component';
import { JobDetailsComponent } from './pages/job-details/job-details.component';
import { MyApplicationsComponent } from './pages/my-applications/my-applications.component';
import { RecommendationsComponent } from './pages/recommendations/recommendations.component';
import { CareerInsightsComponent } from './pages/career-insights/career-insights.component';
import { MarketInsightsComponent } from './pages/market-insights/market-insights.component';
import { PostJobComponent } from './pages/post-job/post-job.component';
import { MyJobsComponent } from './pages/my-jobs/my-jobs.component';
import { ApplicantsComponent } from './pages/applicants/applicants.component';
import { MessagesComponent } from './pages/messages/messages.component';
import { freelancerGuard } from '../../core/guards/freelancer.guard';
import { clientGuard } from '../../core/guards/client.guard';

const routes: Routes = [
  {
    path: '',
    component: JobBoardShellComponent,
    children: [
      { path: 'marketplace', component: MarketplaceComponent, canActivate: [freelancerGuard] },
      { path: 'jobs/:id/applicants', component: ApplicantsComponent, canActivate: [clientGuard] },
      { path: 'jobs/:id', component: JobDetailsComponent },
      { path: 'messages', component: MessagesComponent },
      { path: 'my-applications', component: MyApplicationsComponent, canActivate: [freelancerGuard] },
      { path: 'recommendations', component: RecommendationsComponent, canActivate: [freelancerGuard] },
      { path: 'career-insights', component: CareerInsightsComponent, canActivate: [freelancerGuard] },
      { path: 'market-insights', component: MarketInsightsComponent },
      { path: 'post-job', component: PostJobComponent, canActivate: [clientGuard] },
      { path: 'my-jobs', component: MyJobsComponent, canActivate: [clientGuard] },
      { path: '', pathMatch: 'full', redirectTo: 'market-insights' }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class JobBoardRoutingModule {}
