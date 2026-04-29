import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { CommunityListComponent } from './community-list/community-list.component';
import { PostListComponent } from './post-list/post-list.component';
import { CommentListComponent } from './comment-list/comment-list.component';
import { BlockListComponent } from './block-list/block-list.component';
import { BlockDetailComponent } from './block-detail/block-detail.component';
import { ReportListComponent } from './report-list/report-list.component';
import { CourseReportListComponent } from './course-report-list/course-report-list.component';
import { VoteListComponent } from './vote-list/vote-list.component';
import { CourseVoteListComponent } from './course-vote-list/course-vote-list.component';
import { ContributionListComponent } from './contribution-list/contribution-list.component';
import { CourseListComponent } from './course-list/course-list.component';
import { CourseCommentListComponent } from './course-comment-list/course-comment-list.component';
import { SectionListComponent } from './section-list/section-list.component';
import { SectionDetailComponent } from './section-detail/section-detail.component';

import { CommunityDetailComponent } from './community-detail/community-detail.component';
import { PostDetailComponent } from './post-detail/post-detail.component';
import { CourseDetailComponent } from './course-detail/course-detail.component';

const routes: Routes = [
  { path: 'communities', component: CommunityListComponent },
  { path: 'communities/:id/detail', component: CommunityDetailComponent },
  { path: 'posts', component: PostListComponent },
  { path: 'posts/:id/detail', component: PostDetailComponent },
  { path: 'comments', component: CommentListComponent },
  { path: 'blocks', component: BlockListComponent },
  { path: 'blocks/:id/detail', component: BlockDetailComponent },
  { path: 'reports', component: ReportListComponent },
  { path: 'course-reports', component: CourseReportListComponent },
  { path: 'votes', component: VoteListComponent },
  { path: 'course-votes', component: CourseVoteListComponent },
  { path: 'contributions', component: ContributionListComponent },
  { path: 'courses', component: CourseListComponent },
  { path: 'courses/:id/detail', component: CourseDetailComponent },
  { path: 'course-comments', component: CourseCommentListComponent },
  { path: 'sections', component: SectionListComponent },
  { path: 'sections/:id/detail', component: SectionDetailComponent },
  { path: '', redirectTo: 'communities', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CommunityRoutingModule { }
