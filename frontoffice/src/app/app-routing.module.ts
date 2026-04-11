import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { PublicLayoutComponent } from './layout/public-layout/public-layout.component';
import { AuthLayoutComponent } from './layout/auth-layout/auth-layout.component';
import { DashboardLayoutComponent } from './layout/dashboard-layout/dashboard-layout.component';

import { LandingComponent } from './features/public/landing/landing.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { TwoFactorComponent } from './features/auth/two-factor/two-factor.component';
import { CompleteProfileComponent } from './features/auth/complete-profile/complete-profile.component';
import { OverviewComponent } from './features/dashboard/overview/overview.component';

import { authGuard } from './core/guards/auth.guard';
import { completeProfileGuard } from './core/guards/complete-profile.guard';

const routes: Routes = [

  // PUBLIC
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', component: LandingComponent }
    ]
  },

  // AUTH
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      { path: 'login',            component: LoginComponent },
      { path: 'register',         component: RegisterComponent },
      { path: 'forgot-password',  component: ForgotPasswordComponent },
      { path: 'reset-password',   component: ResetPasswordComponent },
      { path: '2fa',              component: TwoFactorComponent },
      { path: 'complete-profile', component: CompleteProfileComponent },
      { path: '',                 redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // DASHBOARD — Module 01 uniquement
  {
    path: 'app',
    component: DashboardLayoutComponent,
    canActivate: [authGuard, completeProfileGuard],
    canActivateChild: [authGuard, completeProfileGuard],
    children: [
      { path: 'dashboard', component: OverviewComponent },
      {
        path: 'profile',
        loadChildren: () =>
          import('./features/profile/profile.module').then(m => m.ProfileModule)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  { path: '**', redirectTo: '/auth/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}