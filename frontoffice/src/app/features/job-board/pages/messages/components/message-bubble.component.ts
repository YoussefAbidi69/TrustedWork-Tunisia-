import { Component, Input } from '@angular/core';
import { MessageDto } from '../../../models/job-board.models';

@Component({
  selector: 'app-message-bubble',
  template: `
    <div class="message-wrapper" [class.me]="isMe" [class.them]="!isMe">
      <div class="bubble-content">
        {{ msg?.content }}
      </div>
      <div class="bubble-meta">
        <span class="time">{{ msg?.sentAt | date:'shortTime' }}</span>
        <span *ngIf="isMe && msg?.read" class="read-receipt">
          <i class="fas fa-check-double"></i>
        </span>
      </div>
    </div>
  `
})
export class MessageBubbleComponent {
  @Input() msg: MessageDto | null = null;
  @Input() isMe: boolean = false;
}
