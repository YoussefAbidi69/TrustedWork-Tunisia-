import { Component, Input } from '@angular/core';

import { Comment } from '../../../core/models/community.model';

@Component({
  selector: 'app-comment-list',
  templateUrl: './comment-list.component.html',
  styleUrls: ['./comment-list.component.css']
})
export class CommentListComponent {
  @Input() comments: Comment[] = [];
  @Input() loading = false;

  trackByCommentId(_index: number, comment: Comment): number {
    return comment.id;
  }
}
