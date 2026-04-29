import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  ViewEncapsulation,
} from '@angular/core';
import { JobBoardService } from '../../services/job-board.service';
import {
  ConversationSummary,
  MessageDto,
  ScheduleMeetRequest,
} from '../../models/job-board.models';
import { AuthService } from '../../../../core/services/auth.service';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-messages',
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss'],
  encapsulation: ViewEncapsulation.None,
})
export class MessagesComponent implements OnInit, OnDestroy {
  conversations: ConversationSummary[] = [];
  filteredConversations: ConversationSummary[] = [];
  selectedConv: ConversationSummary | null = null;

  chatMessages: MessageDto[] = [];
  chatSending = false;
  chatLoading = false;
  composerContent = '';
  attachedFileName = '';
  attachedFileUrl = '';
  isActionMenuOpen = false;
  isMeetModalOpen = false;
  meetSubmitting = false;

  searchQuery = '';
  private currentUserRef = '';
  private currentUserId = 0;
  private pollSub?: Subscription;
  private attachedObjectUrl = '';
  private sentObjectUrls: string[] = [];
  private attachedFileRef: File | null = null;
  private localAttachmentByUrl = new Map<string, File>();

  @ViewChild('chatScroll') private chatScrollContainer!: ElementRef;
  @ViewChild('fileInput') private fileInput?: ElementRef<HTMLInputElement>;
  @ViewChild('composerTextarea')
  private composerTextarea?: ElementRef<HTMLTextAreaElement>;

  meetForm: ScheduleMeetRequest = {
    conversationId: '',
    title: '',
    date: '',
    time: '09:00',
    duration: 60,
    note: '',
  };

  constructor(
    private jobBoard: JobBoardService,
    private auth: AuthService,
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentAuthUser();
    if (user) {
      this.currentUserId = user.userId;
      this.currentUserRef = this.toPublicRef(user.userId);
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
    this.cleanupAttachedObjectUrl();
    this.cleanupSentObjectUrls();
  }

  loadConversations(): void {
    this.jobBoard.getConversations().subscribe({
      next: (res) => {
        this.conversations = (res || []).map((c) => this.normalizeConversation(c as any));
        this.applyFilters();
      },
      error: () => {
        this.conversations = [];
        this.filteredConversations = [];
      },
    });
  }

  loadConversationsSilent(): void {
    this.jobBoard.getConversations().subscribe({
      next: (res) => {
        this.conversations = (res || []).map((c) => this.normalizeConversation(c as any));
        this.applyFilters();
      },
    });
  }

  applyFilters(): void {
    let result = this.conversations;
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(
        (c) =>
          (c.otherPartyName?.toLowerCase() || '').includes(q) ||
          (c.jobTitle?.toLowerCase() || '').includes(q),
      );
    }
    this.filteredConversations = result;
  }

  onSearch(query: string): void {
    this.searchQuery = query;
    this.applyFilters();
  }

  selectConversation(conv: ConversationSummary): void {
    this.selectedConv = conv;
    this.isActionMenuOpen = false;
    this.chatLoading = true;
    this.chatMessages = [];
    this.loadMessages(conv);
    this.initializeMeetForm();
  }

  loadMessages(conv: ConversationSummary): void {
    const request$ =
      conv.id && conv.id.trim()
        ? this.jobBoard.getConversation(conv.id)
        : this.jobBoard.getConversation(conv.jobOfferId!, conv.peerId!);
    request$.subscribe({
      next: (msgs) => {
        this.chatMessages = msgs;
        this.chatLoading = false;
        this.scrollToBottom();
      },
      error: () => {
        this.chatLoading = false;
      },
    });
  }

  loadMessagesSilent(conv: ConversationSummary): void {
    const request$ =
      conv.id && conv.id.trim()
        ? this.jobBoard.getConversation(conv.id)
        : this.jobBoard.getConversation(conv.jobOfferId!, conv.peerId!);
    request$.subscribe({
      next: (msgs) => {
        if (this.chatMessages.length !== msgs.length) {
          this.chatMessages = msgs;
          this.scrollToBottom();
        }
      },
    });
  }

  sendChat(): void {
    const text = this.composerContent.trim();
    if ((!text && !this.attachedFileName) || this.chatSending || !this.selectedConv) {
      return;
    }
    this.chatSending = true;

    const send$ =
      this.selectedConv.id && this.selectedConv.id.trim()
        ? this.jobBoard.sendMessage(this.selectedConv.id, {
            content: text || this.attachedFileName,
            type: this.attachedFileName ? 'file' : 'text',
            fileUrl: this.attachedFileUrl || undefined,
          })
        : this.jobBoard.sendMessage({
            jobOfferId: this.selectedConv.jobOfferId!,
            receiverId: this.selectedConv.peerId!,
            content: text || this.attachedFileName,
            type: this.attachedFileName ? 'file' : 'text',
            fileUrl: this.attachedFileUrl || undefined,
          });
    send$.subscribe({
        next: (msg) => {
          this.chatMessages.push(msg);
          if (this.attachedFileUrl.startsWith('blob:')) {
            this.sentObjectUrls.push(this.attachedFileUrl);
          }
          this.chatSending = false;
          this.composerContent = '';
          this.attachedFileName = '';
          this.attachedFileUrl = '';
          this.attachedObjectUrl = '';
          if (this.fileInput?.nativeElement) {
            this.fileInput.nativeElement.value = '';
          }
          this.scrollToBottom();
          this.loadConversationsSilent();
        },
        error: () => {
          this.chatSending = false;
        alert('Failed to send message. Please retry.');
        },
    });
  }

  isMe(msg: MessageDto): boolean {
    const sender = msg.senderId as any;
    if (typeof sender === 'number') {
      return sender === this.currentUserId;
    }
    if (typeof sender === 'string') {
      if (sender === this.currentUserRef) return true;
      const parsed = Number(sender);
      if (!Number.isNaN(parsed)) return parsed === this.currentUserId;
    }
    return false;
  }

  onComposerKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendChat();
    }
  }

  onComposerInput(): void {
    const el = this.composerTextarea?.nativeElement;
    if (!el) return;
    el.style.height = 'auto';
    const lineHeight = 24;
    const maxHeight = lineHeight * 4;
    el.style.height = `${Math.min(el.scrollHeight, maxHeight)}px`;
    el.style.overflowY = el.scrollHeight > maxHeight ? 'auto' : 'hidden';
  }

  triggerFilePicker(): void {
    this.fileInput?.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.cleanupAttachedObjectUrl();
    this.attachedObjectUrl = URL.createObjectURL(file);
    this.attachedFileRef = file;
    this.localAttachmentByUrl.set(this.attachedObjectUrl, file);
    this.attachedFileName = file.name;
    this.attachedFileUrl = this.attachedObjectUrl;
  }

  removeAttachedFile(): void {
    this.cleanupAttachedObjectUrl();
    this.attachedFileRef = null;
    this.attachedFileName = '';
    this.attachedFileUrl = '';
    if (this.fileInput?.nativeElement) {
      this.fileInput.nativeElement.value = '';
    }
  }

  downloadAttachment(msg: MessageDto, event: MouseEvent): void {
    event.preventDefault();
    const url = msg.fileUrl?.trim();
    if (!url) return;

    const localFile = this.localAttachmentByUrl.get(url);
    if (localFile) {
      const tempUrl = URL.createObjectURL(localFile);
      const a = document.createElement('a');
      a.href = tempUrl;
      a.download = msg.content || localFile.name || 'attachment';
      a.click();
      URL.revokeObjectURL(tempUrl);
      return;
    }

    const a = document.createElement('a');
    a.href = url;
    a.download = msg.content || 'attachment';
    a.target = '_self';
    a.click();
  }

  private cleanupAttachedObjectUrl(): void {
    if (!this.attachedObjectUrl) return;
    URL.revokeObjectURL(this.attachedObjectUrl);
    this.attachedObjectUrl = '';
  }

  private cleanupSentObjectUrls(): void {
    for (const objectUrl of this.sentObjectUrls) {
      URL.revokeObjectURL(objectUrl);
      this.localAttachmentByUrl.delete(objectUrl);
    }
    this.sentObjectUrls = [];
  }

  openMeetModal(): void {
    if (!this.selectedConv) return;
    this.initializeMeetForm();
    this.isMeetModalOpen = true;
    this.isActionMenuOpen = false;
  }

  openMeetModalWithTitle(title: string): void {
    if (!this.selectedConv) return;
    this.initializeMeetForm();
    this.meetForm.title = (title || '').trim() || this.meetForm.title;
    this.isMeetModalOpen = true;
    this.isActionMenuOpen = false;
  }

  generateMeetLink(title: string): void {
    if (!this.selectedConv || this.meetSubmitting) return;
    this.initializeMeetForm();
    this.meetForm.title = (title || '').trim() || this.meetForm.title;
    this.meetSubmitting = true;

    // Open a placeholder tab from the click event to avoid popup blockers.
    const pendingTab = window.open('', '_blank');

    this.jobBoard.scheduleMeet(this.meetForm).subscribe({
      next: (res) => {
        this.meetSubmitting = false;
        const meetUrl = res?.meetUrl?.trim();
        if (meetUrl) {
          if (pendingTab) {
            pendingTab.location.href = meetUrl;
          } else {
            window.open(meetUrl, '_blank', 'noopener');
          }
        } else if (pendingTab) {
          pendingTab.close();
          alert('Meet link is not ready yet. Please try again in a few seconds.');
        }
        this.loadMessages(this.selectedConv!);
        this.loadConversationsSilent();
      },
      error: (err) => {
        this.meetSubmitting = false;
        if (pendingTab) pendingTab.close();
        const backendMsg =
          err?.error?.message ||
          err?.error?.error ||
          'Failed to generate meet link.';
        alert(backendMsg);
      },
    });
  }

  closeMeetModal(): void {
    this.isMeetModalOpen = false;
    this.meetSubmitting = false;
  }

  createMeet(): void {
    if (!this.selectedConv || this.meetSubmitting) return;
    this.meetSubmitting = true;
    this.jobBoard.scheduleMeet(this.meetForm).subscribe({
      next: () => {
        this.closeMeetModal();
        this.loadMessages(this.selectedConv!);
        this.loadConversationsSilent();
      },
      error: (err) => {
        this.meetSubmitting = false;
        const backendMsg =
          err?.error?.message ||
          err?.error?.error ||
          'Failed to schedule meet.';
        alert(backendMsg);
      },
    });
  }

  getMinDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  formatConversationTime(value: string): string {
    const date = new Date(value);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffH = Math.floor(diffMs / (1000 * 60 * 60));
    if (diffH < 24) {
      return `${Math.max(diffH, 0)}h ago`;
    }
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    if (date.toDateString() === yesterday.toDateString()) {
      return 'Yesterday';
    }
    return date.toLocaleDateString();
  }

  getDateSeparator(index: number): string {
    const current = new Date(this.chatMessages[index].createdAt).toDateString();
    const previous =
      index > 0
        ? new Date(this.chatMessages[index - 1].createdAt).toDateString()
        : null;
    if (current === previous) return '';
    const today = new Date().toDateString();
    const yesterday = new Date(Date.now() - 86400000).toDateString();
    if (current === today) return 'Today';
    if (current === yesterday) return 'Yesterday';
    return new Date(this.chatMessages[index].createdAt).toLocaleDateString();
  }

  getInitials(name: string): string {
    const parts = (name || '').trim().split(/\s+/).filter(Boolean);
    if (!parts.length) return 'U';
    return parts.slice(0, 2).map((p) => p[0].toUpperCase()).join('');
  }

  private initializeMeetForm(): void {
    if (!this.selectedConv) return;
    this.meetForm = {
      conversationId: this.selectedConv.id,
      title: `Meeting re: ${this.selectedConv.jobTitle}`,
      date: this.getMinDate(),
      time: '09:00',
      duration: 60,
      note: '',
    };
  }

  private normalizeConversation(raw: any): ConversationSummary {
    const id = raw?.id ?? '';

    return {
      id,
      otherPartyId: raw?.otherPartyId ?? (raw?.peerId != null ? String(raw.peerId) : ''),
      otherPartyName: raw?.otherPartyName ?? raw?.peerName ?? 'Unknown User',
      jobTitle: raw?.jobTitle ?? '',
      lastMessage: raw?.lastMessage ?? '',
      lastMessageAt: raw?.lastMessageAt ?? new Date().toISOString(),
      unreadCount: Number(raw?.unreadCount ?? 0),
      jobOfferId: raw?.jobOfferId,
      peerId: raw?.peerId,
      peerName: raw?.peerName,
    };
  }

  canSend(): boolean {
    return !!this.selectedConv && !!(this.composerContent.trim() || this.attachedFileName) && !this.chatSending;
  }

  private toPublicRef(userId: number): string {
    const raw = userId * 2654435761;
    return `u_${raw.toString(16)}`;
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatScrollContainer) {
        try {
          this.chatScrollContainer.nativeElement.scrollTop =
            this.chatScrollContainer.nativeElement.scrollHeight;
        } catch (err) {}
      }
    }, 100);
  }
}
