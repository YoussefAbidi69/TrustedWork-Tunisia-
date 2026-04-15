import { Routes } from '@angular/router';
import { DashboardLayoutComponent } from './layout/dashboard-layout/dashboard-layout.component';

export const appRoutes: Routes = [
  {
    path: 'posts',
    component: DashboardLayoutComponent,
    children: [
      {
        path: '',
        loadChildren: () =>
          import('./features/community/community.module').then(m => m.CommunityModule)
      }
    ]
  },
  {
    path: 'communities/:communityId',
    component: DashboardLayoutComponent,
    children: [
      {
        path: '',
        loadChildren: () =>
          import('./features/community/community.module').then(m => m.CommunityModule)
      }
    ]
  },
  {
    path: 'community',
    component: DashboardLayoutComponent,
    children: [
      {
        path: '',
        loadChildren: () =>
          import('./features/community/community.module').then(m => m.CommunityModule)
      }
    ]
  }
];
