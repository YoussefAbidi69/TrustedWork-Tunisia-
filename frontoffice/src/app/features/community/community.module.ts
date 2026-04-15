import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { CommunityRoutingModule } from './community-routing.module';
import { SharedModule } from '../../shared/shared.module';
import { CommunityListComponent } from './community-list/community-list.component';
import { CommunityCreateComponent } from './community-create/community-create.component';
import { CommunityDetailComponent } from './community-detail/community-detail.component';
import { PostCreateComponent } from './post-create/post-create.component';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { CourseDetailComponent } from './course-detail/course-detail.component';
import { LessonViewerComponent } from './lesson-viewer/lesson-viewer.component';
import { ContributionComponent } from './contribution/contribution.component';
import { PostFeedComponent } from './post-feed/post-feed.component';
import { PostCardComponent } from './post-card/post-card.component';
import { CommentListComponent } from './comment-list/comment-list.component';
import { CommentInputComponent } from './comment-input/comment-input.component';
import { CommunityFilterComponent } from './community-filter/community-filter.component';
import { ApiTestComponent } from './api-test/api-test.component';
import { CommunityShellComponent } from './community-shell/community-shell.component';
import { CommunityPageHeaderComponent } from './community-page-header/community-page-header.component';
import { CommunityPanelComponent } from './community-panel/community-panel.component';
import { CourseEditorComponent } from './course-editor/course-editor.component';

@NgModule({
  declarations: [
    CommunityShellComponent,
    CommunityPageHeaderComponent,
    CommunityPanelComponent,
    CommunityListComponent,
    CommunityCreateComponent,
    CommunityDetailComponent,
    PostCreateComponent,
    PostDetailComponent,
    CourseDetailComponent,
    LessonViewerComponent,
    ContributionComponent,
    PostFeedComponent,
    PostCardComponent,
    CommentListComponent,
    CommentInputComponent,
    CommunityFilterComponent,
    ApiTestComponent,
    CourseEditorComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    CommunityRoutingModule,
    SharedModule
  ]
})
export class CommunityModule {}
