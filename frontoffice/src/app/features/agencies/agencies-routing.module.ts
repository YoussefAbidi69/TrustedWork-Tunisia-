import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AgencyListComponent } from './pages/agency-list/agency-list.component';
import { AgencyDetailComponent } from './pages/agency-detail/agency-detail.component';
import { AgencyFormComponent } from './components/agency-form/agency-form.component';
import { MyInvitationsComponent } from './pages/my-invitations/my-invitations.component';
import { OwnerDashboardComponent } from './pages/owner-dashboard/owner-dashboard.component';
import { MyAgenciesComponent } from './pages/my-agencies/my-agencies.component';
import { RecommendedFreelancersComponent } from './pages/recommended-freelancers/recommended-freelancers.component';
import { AgencyChatComponent } from './pages/agency-chat/agency-chat.component';

const routes: Routes = [
  { path: '', component: AgencyListComponent, pathMatch: 'full' },
  { path: 'new', component: AgencyFormComponent },
  { path: 'create', component: AgencyFormComponent },
  { path: 'invitations', component: MyInvitationsComponent },
  { path: 'mon-agence', component: OwnerDashboardComponent },
  { path: 'mes-agences', component: MyAgenciesComponent },
  { path: 'chat', component: AgencyChatComponent },
  { path: ':id', component: AgencyDetailComponent },
  { path: ':id/edit', component: AgencyFormComponent },
  { path: ':id/members', component: RecommendedFreelancersComponent }
];


@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AgenciesRoutingModule { }
