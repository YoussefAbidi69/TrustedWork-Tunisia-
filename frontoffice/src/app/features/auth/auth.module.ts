import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { QRCodeModule } from 'angularx-qrcode';
import { RouterModule } from '@angular/router';             // ← nécessaire pour routerLink

import { AuthRoutingModule } from './auth-routing.module';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';
import { TwoFactorComponent } from './two-factor/two-factor.component';
import { CompleteProfileComponent } from './complete-profile/complete-profile.component'; // ← NOUVEAU

@NgModule({
  declarations: [
    LoginComponent,
    RegisterComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    TwoFactorComponent,
    CompleteProfileComponent  // ← NOUVEAU
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    QRCodeModule,
    AuthRoutingModule
  ]
})
export class AuthModule {}
