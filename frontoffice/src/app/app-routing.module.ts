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

import { OverviewComponent } from './features/dashboard/overview/overview.component';
import { ProfileOverviewComponent } from './features/profile/profile-overview/profile-overview.component';
import { CertificationsComponent } from './features/profile/certifications/certifications.component';
import { SkillsComponent } from './features/profile/skills/skills.component';
import { SettingsComponent } from './features/profile/settings/settings.component';
import { KycComponent } from './features/profile/kyc/kyc.component';
import { TrustPassportComponent } from './features/profile/trust-passport/trust-passport.component';

import { FreelanceJobsComponent } from './features/opportunities/freelance-jobs/freelance-jobs.component';
import { RecruitmentJobsComponent } from './features/opportunities/recruitment-jobs/recruitment-jobs.component';
import { EventsListComponent } from './features/opportunities/events-list/events-list.component';
import { ChallengesComponent } from './features/opportunities/challenges/challenges.component';
import { SavedItemsComponent } from './features/opportunities/saved-items/saved-items.component';


import { ReviewsComponent } from './features/reputation/reviews/reviews.component';
import { NotificationsListComponent } from './features/notifications/notifications-list/notifications-list.component';
import { ChatComponent } from './features/messaging/chat/chat.component';
import { ReclamationsComponent } from './features/support/reclamations/reclamations.component';
import { TrustScoreComponent } from './features/reputation/trust-score/trust-score.component';
import { BadgesXpComponent } from './features/reputation/badges-xp/badges-xp.component';
import { BadgesComponent } from './features/reputation/badges/badges.component';
import { ProgressionComponent } from './features/reputation/progression/progression.component';
import { HistoryComponent } from './features/reputation/history/history.component';


import { EventsOverviewComponent } from './features/events/events-overview/events-overview.component';
import { RecruitmentOverviewComponent } from './features/recruitment/recruitment-overview/recruitment-overview.component';

import { authGuard } from './core/guards/auth.guard';
import { ContractSignComponent } from './features/activity/contract-sign/contract-sign.component';

const routes: Routes = [

  // 🌍 PUBLIC
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', component: LandingComponent },
      { path: 'sign/contract/:id', component: ContractSignComponent }
    ]
  },

  // 🔐 AUTH (groupé proprement)
  {
    path: 'auth',
    component: AuthLayoutComponent,
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent },
      { path: 'forgot-password', component: ForgotPasswordComponent },
      { path: 'reset-password', component: ResetPasswordComponent },
      { path: '2fa', component: TwoFactorComponent },

      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  //  DASHBOARD (protégé)
  {
    
  path: 'app',
  component: DashboardLayoutComponent,
  canActivate: [authGuard],
  canActivateChild: [authGuard],
  children: [
    { path: 'dashboard', component: OverviewComponent },

    //  MODULE PROFILE (lazy loading)
    {
      path: 'profile',
      loadChildren: () =>
        import('./features/profile/profile.module').then(
          (m) => m.ProfileModule
        )
    },

    //  le reste inchangé
    { path: 'reputation/trust-score', component: TrustScoreComponent },
    { path: 'reputation/badges', component: BadgesComponent },
    { path: 'reputation/badges-xp', component: BadgesXpComponent },
    { path: 'reputation/reviews', component: ReviewsComponent },
    { path: 'reputation/progression', component: ProgressionComponent },
    { path: 'reputation/history', component: HistoryComponent },

    { path: 'opportunities/freelance-jobs', component: FreelanceJobsComponent },
    { path: 'opportunities/recruitment-jobs', component: RecruitmentJobsComponent },
    { path: 'opportunities/events-overview', component: EventsOverviewComponent },
    { path: 'opportunities/events-list', component: EventsListComponent },
    { path: 'opportunities/challenges', component: ChallengesComponent },
    { path: 'opportunities/saved-items', component: SavedItemsComponent },

    { path: 'recruitment/overview', component: RecruitmentOverviewComponent },

    {
      path: 'activity',
      loadChildren: () =>
        import('./features/activity/activity.module').then(
          (m) => m.ActivityModule
        )
    },
    {
      path: 'finance',
      loadChildren: () =>
        import('./features/finance/finance.module').then(
          (m) => m.FinanceModule
        )
    },

    { path: 'notifications', component: NotificationsListComponent },
    { path: 'messages', component: ChatComponent },

    { path: 'support/reclamations', component: ReclamationsComponent },

    { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
  ]
}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}