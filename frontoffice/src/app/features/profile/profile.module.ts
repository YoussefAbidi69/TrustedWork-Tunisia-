import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { QRCodeModule } from 'angularx-qrcode';

import { ProfileRoutingModule } from './profile-routing.module';
import { ProfileOverviewComponent } from './profile-overview/profile-overview.component';
import { CertificationsComponent } from './certifications/certifications.component';
import { SkillsComponent } from './skills/skills.component';
import { SettingsComponent } from './settings/settings.component';
import { KycComponent } from './kyc/kyc.component';
import { TrustPassportComponent } from './trust-passport/trust-passport.component';

@NgModule({
  declarations: [
    ProfileOverviewComponent,
    CertificationsComponent,
    SkillsComponent,
    SettingsComponent,
    KycComponent,
    TrustPassportComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    QRCodeModule,
    ProfileRoutingModule
  ]
})
export class ProfileModule {}