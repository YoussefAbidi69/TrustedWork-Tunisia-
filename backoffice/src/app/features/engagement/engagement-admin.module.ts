import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { EventsAdminComponent } from './events-admin/events-admin.component';
import { LeaderboardAdminComponent } from './leaderboard-admin/leaderboard-admin.component';
import { BadgesAdminComponent } from './badges-admin/badges-admin.component';
import { GrowthAdminComponent } from './growth-admin/growth-admin.component';
import { ChallengesAdminComponent } from './challenges-admin/challenges-admin.component';

export const routes: Routes = [
  { path: 'events', component: EventsAdminComponent },
  { path: 'leaderboard', component: LeaderboardAdminComponent },
  { path: 'badges', component: BadgesAdminComponent },
  { path: 'growth-profiles', component: GrowthAdminComponent },
  { path: 'challenges', component: ChallengesAdminComponent },
];

@NgModule({
  declarations: [
    EventsAdminComponent,
    LeaderboardAdminComponent,
    BadgesAdminComponent,
    GrowthAdminComponent,
    ChallengesAdminComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes)
  ]
})
export class EngagementAdminModule {}
