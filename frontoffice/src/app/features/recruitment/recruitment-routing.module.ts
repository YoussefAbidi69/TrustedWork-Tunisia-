import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RecruitmentOverviewComponent } from './recruitment-overview/recruitment-overview.component';

const routes: Routes = [
  {
    path: 'overview',
    component: RecruitmentOverviewComponent
  },
  {
    path: '',
    redirectTo: 'overview',
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class RecruitmentRoutingModule { }