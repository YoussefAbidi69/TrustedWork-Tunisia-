import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ProfileOverviewComponent } from './profile-overview/profile-overview.component';
import { CertificationsComponent } from './certifications/certifications.component';
import { SkillsComponent } from './skills/skills.component';
import { SettingsComponent } from './settings/settings.component';
import { KycComponent } from './kyc/kyc.component';
import { TrustPassportComponent } from './trust-passport/trust-passport.component';
import { PortfolioComponent } from './portfolio/portfolio.component';
import { WorkExperienceComponent } from './work-experience/work-experience.component';
import { EndorsementsComponent } from './endorsements/endorsements.component';
import { ReviewsComponent } from './reviews/reviews.component';
import { EducationComponent } from './education/education.component';
import { CreateProfileComponent } from './create-profile/create-profile.component';
import { PublicProfileComponent } from './public-profile/public-profile.component';
import { CareerRecommendationsComponent } from './career-recommendations/career-recommendations.component';
const routes: Routes = [
  { path: '', redirectTo: 'overview', pathMatch: 'full' },
  { path: 'overview', component: ProfileOverviewComponent },
  { path: 'certifications', component: CertificationsComponent },
  { path: 'skills', component: SkillsComponent },
  { path: 'settings', component: SettingsComponent },
  { path: 'kyc', component: KycComponent },
  { path: 'trust-passport', component: TrustPassportComponent },
  { path: 'portfolio', component: PortfolioComponent },
  { path: 'work-experience', component: WorkExperienceComponent },
  { path: 'endorsements', component: EndorsementsComponent },
  { path: 'reviews', component: ReviewsComponent },
  { path: 'career-path', component: CareerRecommendationsComponent },
  { path: 'education', component: EducationComponent },
  { path: 'create', component: CreateProfileComponent },
  { path: 'public/:userId', component: PublicProfileComponent },
  
  { path: '**', redirectTo: 'overview' }
];
@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ProfileRoutingModule {}