import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';
import { LayoutModule } from './layout/layout.module';

import { PublicModule } from './features/public/public.module';
import { AuthModule } from './features/auth/auth.module';
import { DashboardModule } from './features/dashboard/dashboard.module';
import { ProfileModule } from './features/profile/profile.module';
import { OpportunitiesModule } from './features/opportunities/opportunities.module';
import { ReputationModule } from './features/reputation/reputation.module';
import { MessagingModule } from './features/messaging/messaging.module';
import { NotificationsModule } from './features/notifications/notifications.module';
import { SupportModule } from './features/support/support.module';
import { RecruitmentModule } from './features/recruitment/recruitment.module';

import { EventsOverviewComponent } from './features/events/events-overview/events-overview.component';
import { tokenInterceptor } from './core/interceptors/token.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    EventsOverviewComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CoreModule,
    AppRoutingModule,
    SharedModule,
    LayoutModule,
    PublicModule,
    AuthModule,
    DashboardModule,
    ProfileModule,
    OpportunitiesModule,
    ReputationModule,
    MessagingModule,
    NotificationsModule,
    SupportModule,
    RecruitmentModule
  ],
  providers: [
    provideHttpClient(
      withInterceptors([tokenInterceptor])
    )
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}