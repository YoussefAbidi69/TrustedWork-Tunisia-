import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

import { AuthLayoutComponent } from './layout/auth-layout/auth-layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { LoginComponent } from './features/auth/login/login.component';
import { AutoLoginComponent } from './features/auth/auto-login/auto-login.component'; // ← AJOUT
import { OverviewComponent } from './features/dashboard/overview/overview.component';
import { UsersListComponent } from './features/users/users-list/users-list.component';
import { UserDetailComponent } from './features/users/user-detail/user-detail.component';
import { KycManagementComponent } from './features/users/kyc-management/kyc-management.component';
import { AuditLogsComponent } from './features/admin/audit-logs/audit-logs.component';
import { SuspensionsComponent } from './features/admin/suspensions/suspensions.component';

const routes: Routes = [
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      { path: 'login',      component: LoginComponent },
      { path: 'auto-login', component: AutoLoginComponent }, // ← AJOUT
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard',  component: OverviewComponent },
      { path: 'audit-logs', component: AuditLogsComponent },
      { path: 'suspensions', component: SuspensionsComponent },
      { path: 'users',      component: UsersListComponent },
      { path: 'users/kyc',  component: KycManagementComponent },
      { path: 'users/:id',  component: UserDetailComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/auth/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}