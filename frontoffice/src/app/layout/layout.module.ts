import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { PublicLayoutComponent } from './public-layout/public-layout.component';
import { AuthLayoutComponent } from './auth-layout/auth-layout.component';
import { DashboardLayoutComponent } from './dashboard-layout/dashboard-layout.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  declarations: [
    PublicLayoutComponent,
    AuthLayoutComponent,
    DashboardLayoutComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    SharedModule
  ],
  exports: [
    PublicLayoutComponent,
    AuthLayoutComponent,
    DashboardLayoutComponent
  ]
})
export class LayoutModule { }