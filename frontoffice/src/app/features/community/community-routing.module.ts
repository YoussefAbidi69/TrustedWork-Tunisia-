import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { CommunityShellComponent } from './community-shell/community-shell.component';
import { PostFeedComponent } from './post-feed/post-feed.component';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { CourseDetailComponent } from './course-detail/course-detail.component';
import { ContributionComponent } from './contribution/contribution.component';
import { CommunityListComponent } from './community-list/community-list.component';
import { CommunityCreateComponent } from './community-create/community-create.component';
import { CommunityDetailComponent } from './community-detail/community-detail.component';
import { PostCreateComponent } from './post-create/post-create.component';
import { CourseCreateComponent } from './course-create/course-create.component';
import { MyCoursesComponent } from './my-courses/my-courses.component';

const communityChildRoutes: Routes = [
  { path: '', component: PostFeedComponent },
  { path: 'post/new', component: PostCreateComponent, canActivate: [authGuard] },
  { path: 'post/:id', component: PostDetailComponent },
  { path: 'course/new', component: CourseCreateComponent, canActivate: [authGuard] },
  { path: 'my-courses', component: MyCoursesComponent, canActivate: [authGuard] },
  { path: 'course/:id/download', component: CourseDetailComponent },
  { path: 'course/:id', component: CourseDetailComponent },
  { path: 'contributions', component: ContributionComponent, canActivate: [authGuard] },
  { path: 'browse', component: CommunityListComponent },
  { path: 'create', component: CommunityCreateComponent, canActivate: [authGuard] },
  /** Legacy aliases */
  { path: 'posts/new', redirectTo: 'post/new', pathMatch: 'full' },
  { path: 'posts/:id', redirectTo: 'post/:id', pathMatch: 'full' },
  { path: 'courses/new', redirectTo: 'course/new', pathMatch: 'full' },
  { path: 'courses/:id/download', redirectTo: 'course/:id/download', pathMatch: 'full' },
  { path: 'courses/:id', redirectTo: 'course/:id', pathMatch: 'full' },
  { path: 'communities/:id/posts/new', component: PostCreateComponent, canActivate: [authGuard] },
  { path: 'communities/:id', component: CommunityDetailComponent },
  /** Legacy: post detail at .../:id (numeric post id) */
  { path: ':id', component: PostDetailComponent }
];

const routes: Routes = [{ path: '', component: CommunityShellComponent, children: communityChildRoutes }];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CommunityRoutingModule {}
