import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { JobBoardRoutingModule } from './job-board-routing.module';
import { JobBoardSharedModule } from './job-board-shared.module';
import { SharedModule } from '../../shared/shared.module';
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';

import { JobBoardShellComponent } from './job-board-shell/job-board-shell.component';
import { JobBoardToastComponent } from './job-board-toast/job-board-toast.component';

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
import { ConversationListComponent } from './pages/messages/components/conversation-list.component';
import { ChatHeaderComponent } from './pages/messages/components/chat-header.component';
import { MessageBubbleComponent } from './pages/messages/components/message-bubble.component';
import { MessageInputComponent } from './pages/messages/components/message-input.component';

@NgModule({
  declarations: [
    JobBoardShellComponent,
    JobBoardToastComponent,
    MarketplaceComponent,
    JobDetailsComponent,
    MyApplicationsComponent,
    RecommendationsComponent,
    CareerInsightsComponent,
    MarketInsightsComponent,
    PostJobComponent,
    MyJobsComponent,
    ApplicantsComponent,
    MessagesComponent,
    ConversationListComponent,
    ChatHeaderComponent,
    MessageBubbleComponent,
    MessageInputComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule,
    JobBoardSharedModule,
    JobBoardRoutingModule,
    TimeAgoPipe
  ]
})
export class JobBoardModule {}
