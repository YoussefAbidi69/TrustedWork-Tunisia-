import { Component, OnInit, OnDestroy, ViewChild, ElementRef, ViewEncapsulation } from '@angular/core';
import { JobBoardService } from '../../services/job-board.service';
import { ConversationSummary, MessageDto } from '../../models/job-board.models';
import { AuthService } from '../../../../core/services/auth.service';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-messages',
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class MessagesComponent implements OnInit, OnDestroy {
  conversations: ConversationSummary[] = [];
  filteredConversations: ConversationSummary[] = [];
  selectedConv: ConversationSummary | null = null;
  
  chatMessages: MessageDto[] = [];
  chatSending = false;
  chatLoading = false;
  
  searchQuery = '';
  activeFilter: 'all' | 'unread' = 'all';

  private currentUserId = 0;
  private pollSub?: Subscription;

  @ViewChild('chatScroll') private chatScrollContainer!: ElementRef;

  constructor(
    private jobBoard: JobBoardService,
    private auth: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentAuthUser();
    if (user) {
      this.currentUserId = user.userId;
      this.loadConversations();
      
      this.pollSub = interval(5000).subscribe(() => {
        this.loadConversationsSilent();
        if (this.selectedConv) {
          this.loadMessagesSilent(this.selectedConv);
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
    }
  }

  loadConversations(): void {
    this.jobBoard.getConversations().subscribe({
      next: (res) => {
        this.conversations = res;
        this.applyFilters();
      }
    });
  }

  loadConversationsSilent(): void {
    this.jobBoard.getConversations().subscribe({
      next: (res) => {
        this.conversations = res;
        this.applyFilters();
      }
    });
  }

  applyFilters(): void {
    let result = this.conversations;
    if (this.activeFilter === 'unread') {
      result = result.filter(c => c.unreadCount > 0);
    }
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(c => 
        (c.peerName?.toLowerCase() || '').includes(q) || 
        (c.jobTitle?.toLowerCase() || '').includes(q)
      );
    }
    this.filteredConversations = result;
  }

  setFilter(filter: 'all' | 'unread'): void {
    this.activeFilter = filter;
    this.applyFilters();
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.applyFilters();
  }

  selectConversation(conv: ConversationSummary): void {
    this.selectedConv = conv;
    this.chatLoading = true;
    this.chatMessages = [];
    this.loadMessages(conv);
  }

  loadMessages(conv: ConversationSummary): void {
    this.jobBoard.getConversation(conv.jobOfferId, conv.peerId).subscribe({
      next: (msgs) => {
        this.chatMessages = msgs;
        this.chatLoading = false;
        this.scrollToBottom();
        this.jobBoard.markMessagesRead(conv.jobOfferId, conv.peerId).subscribe();
      },
      error: () => {
        this.chatLoading = false;
      }
    });
  }

  loadMessagesSilent(conv: ConversationSummary): void {
    this.jobBoard.getConversation(conv.jobOfferId, conv.peerId).subscribe({
      next: (msgs) => {
        if (this.chatMessages.length !== msgs.length) {
           this.chatMessages = msgs;
           this.scrollToBottom();
           this.jobBoard.markMessagesRead(conv.jobOfferId, conv.peerId).subscribe();
        }
      }
    });
  }

  sendChat(content: string): void {
    if (!content.trim() || this.chatSending || !this.selectedConv) return;
    this.chatSending = true;
    
    this.jobBoard.sendMessage({
      jobOfferId: this.selectedConv.jobOfferId,
      receiverId: this.selectedConv.peerId,
      content: content.trim()
    }).subscribe({
      next: (msg) => {
        this.chatMessages.push(msg);
        this.chatSending = false;
        this.scrollToBottom();
        this.loadConversationsSilent();
      },
      error: () => {
        this.chatSending = false;
        alert('Failed to send message.');
      }
    });
  }

  isMe(msg: MessageDto): boolean {
    return msg.senderId === this.currentUserId;
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatScrollContainer) {
        try {
          this.chatScrollContainer.nativeElement.scrollTop = this.chatScrollContainer.nativeElement.scrollHeight;
        } catch(err) {}
      }
    }, 100);
  }
}

