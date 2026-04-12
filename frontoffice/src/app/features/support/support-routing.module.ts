import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ReclamationsComponent } from './reclamations/reclamations.component';

const routes: Routes = [
  {
    path: 'reclamations',
    component: ReclamationsComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SupportRoutingModule { }