import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { TokenInterceptor } from './core/interceptors/token.interceptor';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { AuthLayoutComponent } from './layout/auth-layout/auth-layout.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';

import { SidebarComponent } from './shared/components/sidebar/sidebar.component';
import { TopbarComponent } from './shared/components/topbar/topbar.component';
import { StatCardComponent } from './shared/components/stat-card/stat-card.component';

import { LoginComponent } from './features/auth/login/login.component';
// ✅ AutoLoginComponent ajouté — manquant dans la version précédente
import { AutoLoginComponent } from './features/auth/auto-login/auto-login.component';
import { OverviewComponent } from './features/dashboard/overview/overview.component';
import { UsersListComponent } from './features/users/users-list/users-list.component';
import { UserDetailComponent } from './features/users/user-detail/user-detail.component';
import { KycManagementComponent } from './features/users/kyc-management/kyc-management.component';

import { AuditLogsComponent } from './features/admin/audit-logs/audit-logs.component';
import { SuspensionsComponent } from './features/admin/suspensions/suspensions.component';
import { SchedulerManagementComponent } from './features/admin/scheduler-management/scheduler-management.component';

import { FilterCatPipe } from './shared/pipes/filter-cat.pipe';

// ── Module 02 : Freelancer Profiles ──
import { ProfilesListComponent } from './features/freelancer/profiles-list/profiles-list.component';
import { ProfileDetailComponent } from './features/freelancer/profile-detail/profile-detail.component';
import { ReportsManagementComponent } from './features/freelancer/reports-management/reports-management.component';
import { PlatformStatsComponent } from './features/freelancer/platform-stats/platform-stats.component';
import { ProfileSkillsTabComponent } from './features/freelancer/profile-detail/components/profile-skills-tab/profile-skills-tab.component';
import { ProfileExperienceTabComponent } from './features/freelancer/profile-detail/components/profile-experience-tab/profile-experience-tab.component';
import { ProfilePortfolioTabComponent } from './features/freelancer/profile-detail/components/profile-portfolio-tab/profile-portfolio-tab.component';
import { ProfileReputationTabComponent } from './features/freelancer/profile-detail/components/profile-reputation-tab/profile-reputation-tab.component';
import { ProfileInsightsTabComponent } from './features/freelancer/profile-detail/components/profile-insights-tab/profile-insights-tab.component';
import { ReviewsManagementComponent } from './features/freelancer/reviews-management/reviews-management.component';
import { ProfileOverviewTabComponent } from './features/freelancer/profile-detail/components/profile-overview-tab/profile-overview-tab.component';
import { TrendingProfilesComponent } from './features/freelancer/trending-profiles/trending-profiles.component';

@NgModule({
  declarations: [
    AppComponent,
    AuthLayoutComponent,
    AdminLayoutComponent,
    SidebarComponent,
    TopbarComponent,
    StatCardComponent,
    LoginComponent,
    AutoLoginComponent, // ✅ déclaré ici
    OverviewComponent,
    UsersListComponent,
    UserDetailComponent,
    KycManagementComponent,
    AuditLogsComponent,
    SuspensionsComponent,
    ProfileExperienceTabComponent,
    FilterCatPipe,
    // ── Module 02 ──
    ProfilesListComponent,
    ProfileDetailComponent,
    ReportsManagementComponent,
    PlatformStatsComponent,
    ProfileSkillsTabComponent,
    ProfilePortfolioTabComponent,
    ProfileReputationTabComponent,
    ProfileInsightsTabComponent,
    ReviewsManagementComponent,
    ProfileOverviewTabComponent,
    TrendingProfilesComponent,
    // ── Système ──
    SchedulerManagementComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    AppRoutingModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    CommonModule
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}