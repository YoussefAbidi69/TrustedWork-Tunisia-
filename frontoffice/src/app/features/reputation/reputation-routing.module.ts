import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { TrustScoreComponent } from './trust-score/trust-score.component';
import { ReviewsComponent } from './reviews/reviews.component';
import { BadgesComponent } from './badges/badges.component';
import { BadgesXpComponent } from './badges-xp/badges-xp.component';
import { HistoryComponent } from './history/history.component';
import { ProgressionComponent } from './progression/progression.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'trust-score',
    pathMatch: 'full'
  },
  {
    path: 'trust-score',
    component: TrustScoreComponent
  },
  {
    path: 'badges',
    component: BadgesComponent
  },
  {
    path: 'badges-xp',
    component: BadgesXpComponent
  },
  {
    path: 'reviews',
    component: ReviewsComponent
  },
  {
    path: 'history',
    component: HistoryComponent
  },
  {
    path: 'progression',
    component: ProgressionComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ReputationRoutingModule {}