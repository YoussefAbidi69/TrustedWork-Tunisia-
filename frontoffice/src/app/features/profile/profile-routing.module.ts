import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ProfileOverviewComponent } from './profile-overview/profile-overview.component';
import { CertificationsComponent } from './certifications/certifications.component';
import { SkillsComponent } from './skills/skills.component';
import { SettingsComponent } from './settings/settings.component';
import { KycComponent } from './kyc/kyc.component';
import { TrustPassportComponent } from './trust-passport/trust-passport.component';

const routes: Routes = [
  // Redirection par défaut vers overview
  { path: '',               redirectTo: 'overview', pathMatch: 'full' },

  // ← FIX : path 'overview' au lieu de 'profile-overview'
  // La sidebar pointe vers /app/profile/overview
  { path: 'overview',       component: ProfileOverviewComponent },
  { path: 'certifications', component: CertificationsComponent },
  { path: 'skills',         component: SkillsComponent },
  { path: 'settings',       component: SettingsComponent },
  { path: 'kyc',            component: KycComponent },
  { path: 'trust-passport', component: TrustPassportComponent },

  // Fallback
  { path: '**',             redirectTo: 'overview' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProfileRoutingModule {}