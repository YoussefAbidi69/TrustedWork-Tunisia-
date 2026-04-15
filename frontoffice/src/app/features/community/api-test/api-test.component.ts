import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';

import { AuthService } from '../../../core/services/auth.service';
import { CommunityService } from '../../../core/services/community.service';
import { PostService } from '../../../core/services/post.service';
import { ContributionService } from '../../../core/services/contribution.service';
import { CommunityAiService } from '../services/community-ai.service';
import { VoteType } from '../../../core/models/community.model';

@Component({
  selector: 'app-api-test',
  templateUrl: './api-test.component.html',
  styleUrls: ['./api-test.component.css']
})
export class ApiTestComponent {
  loading = false;
  lastAction = 'No request yet';
  responseText = '';
  errorText = '';

  /** Post id used for GET /api/comments/post/{id} */
  testPostId = 1;

  constructor(
    private readonly communityService: CommunityService,
    private readonly postService: PostService,
    private readonly contributionService: ContributionService,
    private readonly authService: AuthService,
    private readonly communityAiService: CommunityAiService
  ) {}

  testGetCommunities(): void {
    this.start('GET /api/communities');
    this.communityService.getAll().subscribe({
      next: (data) => this.finishSuccess(data),
      error: (err) => this.finishError(err)
    });
  }

  testGetPosts(): void {
    this.start('GET /api/posts');
    this.postService.getAll().subscribe({
      next: (data) => this.finishSuccess(data),
      error: (err) => this.finishError(err)
    });
  }

  testGetComments(): void {
    this.start(`GET /api/comments/post/${this.testPostId}`);
    this.postService.getComments(this.testPostId).subscribe({
      next: (data) => this.finishSuccess(data),
      error: (err) => this.finishError(err)
    });
  }

  testVote(): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) {
      this.finishClientError('Sign in to test POST /api/votes (userId required).');
      return;
    }
    this.start(`POST /api/votes?postId=${this.testPostId}&type=UP&userId=${userId}`);
    this.postService.vote(this.testPostId, VoteType.UP, userId).subscribe({
      next: (data) => this.finishSuccess(data),
      error: (err) => this.finishError(err)
    });
  }

  testContribution(): void {
    const userId = this.authService.getCurrentAuthUser()?.userId;
    if (userId == null) {
      this.finishClientError('Sign in to test GET /api/contributions/users/{userId}.');
      return;
    }
    this.start(`GET /api/contributions/users/${userId}`);
    this.contributionService.getByUserId(userId).subscribe({
      next: (data) => this.finishSuccess(data),
      error: (err) => this.finishError(err)
    });
  }

  testAiCourseOutline(): void {
    this.start('POST /api/ai/course-outline');
    this.communityAiService.generateCourseOutline('Spring Boot', 'Beginner').subscribe({
      next: (data) => this.finishSuccess(data),
      error: (err) => this.finishError(err)
    });
  }

  testAiQuiz(): void {
    this.start('POST /api/ai/quiz');
    this.communityAiService.generateQuiz('Lesson: variables and loops.').subscribe({
      next: (data) => this.finishSuccess(data),
      error: (err) => this.finishError(err)
    });
  }

  testAiSummarize(): void {
    this.start('POST /api/ai/summarize');
    this.communityAiService.summarizeLesson('Long lesson text about REST APIs.').subscribe({
      next: (data) => this.finishSuccess({ summary: data }),
      error: (err) => this.finishError(err)
    });
  }

  testAiTutor(): void {
    this.start('POST /api/ai/tutor-answer');
    this.communityAiService.tutorAnswer('Course: HTTP basics.', 'What is idempotency?').subscribe({
      next: (data) => this.finishSuccess({ answer: data }),
      error: (err) => this.finishError(err)
    });
  }

  private start(action: string): void {
    this.loading = true;
    this.lastAction = action;
    this.responseText = '';
    this.errorText = '';
  }

  private finishSuccess(data: unknown): void {
    this.loading = false;
    this.responseText = JSON.stringify(data, null, 2);
  }

  private finishError(error: HttpErrorResponse): void {
    this.loading = false;
    this.errorText = `Status: ${error.status}\n${JSON.stringify(error.error, null, 2)}`;
  }

  private finishClientError(message: string): void {
    this.loading = false;
    this.errorText = message;
    this.responseText = '';
  }
}
