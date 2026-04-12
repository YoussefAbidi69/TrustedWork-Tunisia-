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
import { PortfolioComponent } from './portfolio/portfolio.component';
import { WorkExperienceComponent } from './work-experience/work-experience.component';
import { EndorsementsComponent } from './endorsements/endorsements.component';
import { ReviewsComponent } from './reviews/reviews.component';
import { CareerRecommendationsComponent } from './career-recommendations/career-recommendations.component';
import { EducationComponent } from './education/education.component';
import { CreateProfileComponent } from './create-profile/create-profile.component';
import { PublicProfileComponent } from './public-profile/public-profile.component';

@NgModule({
  declarations: [
    ProfileOverviewComponent,
    CertificationsComponent,
    SkillsComponent,
    SettingsComponent,
    KycComponent,
    TrustPassportComponent,
    PortfolioComponent,
    WorkExperienceComponent,
    EndorsementsComponent,
    ReviewsComponent,
    CareerRecommendationsComponent,
    EducationComponent,
    CreateProfileComponent,
    PublicProfileComponent
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