import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ProfileOverviewComponent } from './profile-overview/profile-overview.component';
import { CertificationsComponent } from './certifications/certifications.component';
import { SkillsComponent } from './skills/skills.component';
import { SettingsComponent } from './settings/settings.component';
import { KycComponent } from './kyc/kyc.component';
import { TrustPassportComponent } from './trust-passport/trust-passport.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'profile-overview',
    pathMatch: 'full'
  },

  {
    path: 'profile-overview',
    component: ProfileOverviewComponent
  },
  {
    path: 'certifications',
    component: CertificationsComponent
  },
  {
    path: 'skills',
    component: SkillsComponent
  },
  {
    path: 'settings',
    component: SettingsComponent
  },
  {
    path: 'kyc',
    component: KycComponent
  },
  {
    path: 'trust-passport',
    component: TrustPassportComponent
  },

  // 🔥 IMPORTANT: fallback pour éviter écran blanc
  {
    path: '**',
    redirectTo: 'profile-overview'
  }
];

@NgModule({
  imports: [
    RouterModule.forChild(routes)
  ],
  exports: [RouterModule]
})
export class ProfileRoutingModule {}