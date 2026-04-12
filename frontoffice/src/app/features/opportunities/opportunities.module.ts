import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { OpportunitiesRoutingModule } from './opportunities-routing.module';
import { FreelanceJobsComponent } from './freelance-jobs/freelance-jobs.component';
import { RecruitmentJobsComponent } from './recruitment-jobs/recruitment-jobs.component';
import { EventsListComponent } from './events-list/events-list.component';
import { ChallengesComponent } from './challenges/challenges.component';
import { SavedItemsComponent } from './saved-items/saved-items.component';

@NgModule({
  declarations: [
    FreelanceJobsComponent,
    RecruitmentJobsComponent,
    EventsListComponent,
    ChallengesComponent,
    SavedItemsComponent
  ],
  imports: [
    CommonModule,
    OpportunitiesRoutingModule
  ]
})
export class OpportunitiesModule { }