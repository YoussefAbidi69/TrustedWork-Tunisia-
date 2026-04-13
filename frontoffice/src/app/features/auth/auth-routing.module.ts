import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';
import { TwoFactorComponent } from './two-factor/two-factor.component';
import { CompleteProfileComponent } from './complete-profile/complete-profile.component'; // ← NOUVEAU

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  { path: 'verify-2fa', component: TwoFactorComponent },
  { path: 'complete-profile', component: CompleteProfileComponent }, // ← NOUVEAU
  { path: '2fa', component: TwoFactorComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AuthRoutingModule {}
