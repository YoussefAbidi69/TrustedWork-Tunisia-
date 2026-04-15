import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { authGuard } from '../../core/guards/auth.guard';
import { CommunityShellComponent } from './community-shell/community-shell.component';
import { PostFeedComponent } from './post-feed/post-feed.component';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { CourseDetailComponent } from './course-detail/course-detail.component';
import { LessonViewerComponent } from './lesson-viewer/lesson-viewer.component';
import { ContributionComponent } from './contribution/contribution.component';
import { ApiTestComponent } from './api-test/api-test.component';
import { CommunityListComponent } from './community-list/community-list.component';
import { CommunityCreateComponent } from './community-create/community-create.component';
import { CommunityDetailComponent } from './community-detail/community-detail.component';
import { PostCreateComponent } from './post-create/post-create.component';
import { CourseEditorComponent } from './course-editor/course-editor.component';

const communityChildRoutes: Routes = [
  { path: '', component: PostFeedComponent },
  { path: 'api-test', component: ApiTestComponent, canActivate: [authGuard] },
  { path: 'contributions', component: ContributionComponent, canActivate: [authGuard] },
  { path: 'browse', component: CommunityListComponent },
  { path: 'create', component: CommunityCreateComponent, canActivate: [authGuard] },
  { path: 'courses/new', component: CourseEditorComponent, canActivate: [authGuard] },
  { path: 'courses/:id/learn', component: LessonViewerComponent },
  { path: 'courses/:id', component: CourseDetailComponent },
  /** Must be before `posts/:id` so "new" is not parsed as a post id */
  { path: 'posts/new', component: PostCreateComponent, canActivate: [authGuard] },
  { path: 'posts/:id', component: PostDetailComponent },
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
