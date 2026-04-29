import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DragDropModule } from '@angular/cdk/drag-drop';

import { UserCommunityRoutingModule } from './community-routing.module';
import { SharedModule } from '../../shared/shared.module';
import { CommunityListComponent } from './community-list/community-list.component';
import { CommunityCreateComponent } from './community-create/community-create.component';
import { CommunityDetailComponent } from './community-detail/community-detail.component';
import { PostCreateComponent } from './post-create/post-create.component';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { ContributionComponent } from './contribution/contribution.component';
import { PostFeedComponent } from './post-feed/post-feed.component';
import { PostCardComponent } from './post-card/post-card.component';
import { CommentListComponent } from './comment-list/comment-list.component';
import { CommentInputComponent } from './comment-input/comment-input.component';
import { CommunityFilterComponent } from './community-filter/community-filter.component';
import { CommunityShellComponent } from './community-shell/community-shell.component';
import { CommunityPageHeaderComponent } from './community-page-header/community-page-header.component';
import { CommunityPanelComponent } from './community-panel/community-panel.component';
import { CourseCardComponent } from './course-card/course-card.component';
import { CourseDetailComponent } from './course-detail/course-detail.component';
import { CourseCreateComponent } from './course-create/course-create.component';
import { MyCoursesComponent } from './my-courses/my-courses.component';

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
    ContributionComponent,
    PostFeedComponent,
    PostCardComponent,
    CommentListComponent,
    CommentInputComponent,
    CommunityFilterComponent,
    CourseCardComponent,
    CourseDetailComponent,
    CourseCreateComponent,
    MyCoursesComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DragDropModule,
    UserCommunityRoutingModule,
    SharedModule
  ]
})
export class UserCommunityModule {}
