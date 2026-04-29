import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ActivityRoutingModule } from './activity-routing.module';
import { ContractDetailComponent } from './contract-detail/contract-detail';
import { ContractsComponent } from './contracts/contracts.component';
import { ContractFormComponent } from './contract-form/contract-form';
import { MilestoneFormComponent } from './milestones/milestone-form/milestone-form';
import { DisputeListComponent } from './disputes/dispute-list/dispute-list.component';
import { DisputeCreateComponent } from './disputes/dispute-create/dispute-create.component';
import { DisputeDetailComponent } from './disputes/dispute-detail/dispute-detail.component';

@NgModule({
  declarations: [
    ContractsComponent,
    ContractFormComponent,
    MilestoneFormComponent,
    DisputeListComponent,
    DisputeCreateComponent,
    DisputeDetailComponent
  ],
  imports: [
    CommonModule,
    ActivityRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    ContractDetailComponent
  ]
})
export class ActivityModule { }
