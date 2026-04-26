import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { EngagementDashboardComponent } from './engagement-dashboard/engagement-dashboard.component';
import { EventsComponent } from './events/events.component';
import { GamificationComponent } from './gamification/gamification.component';
import { LeaderboardComponent } from './leaderboard/leaderboard.component';
import { UserChallengesComponent } from './gamification/user-challenges/user-challenges.component';

const routes: Routes = [
  {
    path: '',
    children: [
      { path: '',           redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview',   component: EngagementDashboardComponent },
      { path: 'events',     component: EventsComponent },
      { path: 'gamification', component: GamificationComponent },
      { path: 'leaderboard',  component: LeaderboardComponent },
      { path: 'missions',     component: UserChallengesComponent }
    ]
  }
];

@NgModule({
  declarations: [
    EngagementDashboardComponent,
    EventsComponent,
    GamificationComponent,
    LeaderboardComponent,
    UserChallengesComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild(routes)
  ]
})
export class EngagementModule {}
