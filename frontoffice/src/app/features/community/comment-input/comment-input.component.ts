import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-comment-input',
  templateUrl: './comment-input.component.html',
  styleUrls: ['./comment-input.component.css']
})
export class CommentInputComponent {
  @Input() disabled = false;
  @Input() submitting = false;
  @Input() error = '';

  @Output() submitComment = new EventEmitter<string>();

  content = '';
  touched = false;

  onSubmit(): void {
    this.touched = true;
    const trimmed = this.content.trim();
    if (!trimmed) {
      return;
    }

    this.submitComment.emit(trimmed);
  }

  clear(): void {
    this.content = '';
    this.touched = false;
  }
}
