import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';
import { LayoutModule } from './layout/layout.module';
import { PublicModule } from './features/public/public.module';
import { AuthModule } from './features/auth/auth.module';
import { DashboardModule } from './features/dashboard/dashboard.module';
import { ProfileModule } from './features/profile/profile.module';

import { tokenInterceptor } from './core/interceptors/token.interceptor';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToastrModule } from 'ngx-toastr';


@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    CoreModule,
    AppRoutingModule,
    SharedModule,
    LayoutModule,
    PublicModule,
    AuthModule,
    DashboardModule,
    BrowserAnimationsModule,
    ToastrModule.forRoot({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
    }),
    ProfileModule
  ],
  providers: [
    provideHttpClient(withInterceptors([tokenInterceptor]))
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}