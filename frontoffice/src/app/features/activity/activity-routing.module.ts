import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ContractDetailComponent } from './contract-detail/contract-detail';
import { ContractsComponent } from './contracts/contracts.component';
import { ContractFormComponent } from './contract-form/contract-form';
import { MilestoneFormComponent } from './milestones/milestone-form/milestone-form';
import { ContractSignComponent } from './contract-sign/contract-sign.component';
import { DisputeListComponent } from './disputes/dispute-list/dispute-list.component';
import { DisputeCreateComponent } from './disputes/dispute-create/dispute-create.component';
import { DisputeDetailComponent } from './disputes/dispute-detail/dispute-detail.component';

const routes: Routes = [
  { path: '', redirectTo: 'contracts', pathMatch: 'full' },
  { path: 'contracts', component: ContractsComponent },
  { path: 'contracts/new', component: ContractFormComponent },
  { path: 'contracts/:id', component: ContractDetailComponent },
  { path: 'contracts/:id/edit', component: ContractFormComponent },
  { path: 'contracts/:id/milestones/new', component: MilestoneFormComponent },
  { path: 'contracts/:id/milestones/:mId/edit', component: MilestoneFormComponent },
  { path: 'contracts/:id/sign', component: ContractSignComponent },
  { path: 'disputes', component: DisputeListComponent },
  { path: 'disputes/new', component: DisputeCreateComponent },
  { path: 'disputes/:disputeId', component: DisputeDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ActivityRoutingModule { }