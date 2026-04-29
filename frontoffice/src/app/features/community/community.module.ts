import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { CommunityRoutingModule } from './community-routing.module';
import { CommunityListComponent } from './community-list/community-list.component';
import { PostListComponent } from './post-list/post-list.component';
import { CommentListComponent } from './comment-list/comment-list.component';
import { BlockListComponent } from './block-list/block-list.component';
import { ReportListComponent } from './report-list/report-list.component';
import { CourseReportListComponent } from './course-report-list/course-report-list.component';
import { VoteListComponent } from './vote-list/vote-list.component';
import { CourseVoteListComponent } from './course-vote-list/course-vote-list.component';
import { ContributionListComponent } from './contribution-list/contribution-list.component';
import { CourseListComponent } from './course-list/course-list.component';
import { CourseCommentListComponent } from './course-comment-list/course-comment-list.component';
import { SectionListComponent } from './section-list/section-list.component';
import { CommunityDetailComponent } from './community-detail/community-detail.component';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { CourseDetailComponent } from './course-detail/course-detail.component';
import { BlockDetailComponent } from './block-detail/block-detail.component';
import { SectionDetailComponent } from './section-detail/section-detail.component';

@NgModule({
  declarations: [
    CommunityListComponent,
    PostListComponent,
    CommentListComponent,
    BlockListComponent,
    ReportListComponent,
    CourseReportListComponent,
    VoteListComponent,
    CourseVoteListComponent,
    ContributionListComponent,
    CourseListComponent,
    CourseCommentListComponent,
    SectionListComponent,
    CommunityDetailComponent,
    PostDetailComponent,
    CourseDetailComponent,
    BlockDetailComponent,
    SectionDetailComponent
  ],
  imports: [
    CommonModule,
    CommunityRoutingModule,
    FormsModule
  ]
})
export class CommunityModule { }
