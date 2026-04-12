import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { RecruitmentRoutingModule } from './recruitment-routing.module';
import { RecruitmentOverviewComponent } from './recruitment-overview/recruitment-overview.component';

@NgModule({
  declarations: [
    RecruitmentOverviewComponent
  ],
  imports: [
    CommonModule,
    RecruitmentRoutingModule
  ]
})
export class RecruitmentModule { }