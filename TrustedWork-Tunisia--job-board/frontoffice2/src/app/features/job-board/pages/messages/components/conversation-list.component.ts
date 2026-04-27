import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ConversationSummary } from '../../../models/job-board.models';

@Component({
  selector: 'app-conversation-list',
  template: `
    <div class="sidebar-wrapper">
      <div class="sidebar-header">
        <h2>Messages</h2>
        <div class="search-bar">
          <i class="fas fa-search search-icon"></i>
          <input type="text" placeholder="Search messages..." [value]="searchQuery" (input)="onSearch($event)" />
        </div>
        <div class="filter-tabs">
          <button [class.active]="activeFilter === 'all'" (click)="setFilter('all')">All</button>
          <button [class.active]="activeFilter === 'unread'" (click)="setFilter('unread')">Unread</button>
        </div>
      </div>
      
      <div class="empty-state" *ngIf="conversations.length === 0">
        <i class="fas fa-inbox"></i>
        <p>No conversations found.</p>
      </div>

      <div class="conversation-list">
        <div class="conversation-item" 
             *ngFor="let conv of conversations" 
             [class.active]="selectedConv?.jobOfferId === conv.jobOfferId && selectedConv?.peerId === conv.peerId"
             [class.unread]="conv.unreadCount > 0"
             (click)="onSelect(conv)">
          
          <div class="avatar-container">
            <div class="avatar">{{ getInitials(conv.peerName) }}</div>
            <div class="online-indicator"></div>
          </div>
          
          <div class="conv-content">
            <div class="conv-top">
              <strong class="peer-name">{{ conv.peerName || 'Peer #' + conv.peerId }}</strong>
              <span class="conv-time">{{ conv.lastMessageAt | date:'shortTime' }}</span>
            </div>
            <div class="conv-middle">
              <span class="job-title">{{ conv.jobTitle }}</span>
            </div>
            <div class="conv-bottom">
              <p class="conv-preview">{{ conv.lastMessage }}</p>
              <span class="unread-badge" *ngIf="conv.unreadCount > 0">{{ conv.unreadCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ConversationListComponent {
  @Input() conversations: ConversationSummary[] = [];
  @Input() selectedConv: ConversationSummary | null = null;
  @Input() activeFilter: 'all' | 'unread' = 'all';
  @Input() searchQuery = '';
  
  @Output() selectConv = new EventEmitter<ConversationSummary>();
  @Output() filterChange = new EventEmitter<'all' | 'unread'>();
  @Output() searchChange = new EventEmitter<string>();

  onSelect(conv: ConversationSummary) {
    this.selectConv.emit(conv);
  }

  setFilter(f: 'all' | 'unread') {
    this.filterChange.emit(f);
  }

  onSearch(event: any) {
    this.searchChange.emit(event.target.value);
  }

  getInitials(name: string | undefined): string {
    if (!name) return 'P';
    return name.substring(0, 2).toUpperCase();
  }
}
