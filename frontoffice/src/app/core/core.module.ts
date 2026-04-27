import { NgModule, Optional, SkipSelf } from '@angular/core';
import { CommonModule } from '@angular/common';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

@NgModule({
  imports: [
    CommonModule
  ],
  providers: [
    provideHttpClient(withInterceptorsFromDi())
  ]
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    if (parentModule) {
      throw new Error('CoreModule est déjà chargé. Il doit être importé une seule fois dans AppModule.');
    }
  }
}