import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SupportRoutingModule } from './support-routing.module';
import { ReclamationsComponent } from './reclamations/reclamations.component';

@NgModule({
  declarations: [
    ReclamationsComponent
  ],
  imports: [
    CommonModule,
    SupportRoutingModule
  ]
})
export class SupportModule { }