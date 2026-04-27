import { Component, Input } from '@angular/core';
import { ConversationSummary } from '../../../models/job-board.models';

@Component({
  selector: 'app-chat-header',
  template: `
    <div class="chat-header">
      <div class="header-user-info">
        <div class="avatar-container">
          <div class="avatar">{{ getInitials(conv?.peerName) }}</div>
          <div class="online-indicator"></div>
        </div>
        <div class="user-details">
          <h3>{{ conv?.peerName || 'Peer #' + conv?.peerId }}</h3>
          <span class="status-text">Online • {{ conv?.jobTitle }}</span>
        </div>
      </div>
      <div class="header-actions">
        <button class="action-btn"><i class="fas fa-phone-alt"></i></button>
        <button class="action-btn"><i class="fas fa-video"></i></button>
        <button class="action-btn"><i class="fas fa-ellipsis-v"></i></button>
      </div>
    </div>
  `
})
export class ChatHeaderComponent {
  @Input() conv: ConversationSummary | null = null;

  getInitials(name: string | undefined): string {
    if (!name) return 'P';
    return name.substring(0, 2).toUpperCase();
  }
}
