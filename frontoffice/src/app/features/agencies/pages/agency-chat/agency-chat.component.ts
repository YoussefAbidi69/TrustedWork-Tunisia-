import { Component, OnInit, OnDestroy, ViewChild, ElementRef, HostListener, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AgencyChatService, ChatMessage, OnlineMember, ReplyPreviewDTO, AttachmentDTO } from '../../services/agency-chat.service';
import { AgencyService } from '../../services/agency.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Agency } from '../../../../core/models/agency.model';
import { finalize } from 'rxjs/operators';
import { ToastrService } from 'ngx-toastr';

import { ChatMessageBubbleComponent } from '../../components/chat-message-bubble/chat-message-bubble.component';
import { ChatInputBarComponent } from '../../components/chat-input-bar/chat-input-bar.component';
import { PinnedMessagesPanelComponent } from '../../components/pinned-messages-panel/pinned-messages-panel.component';

@Component({
  selector: 'app-agency-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, ChatMessageBubbleComponent, ChatInputBarComponent, PinnedMessagesPanelComponent],
  templateUrl: './agency-chat.component.html',
  styleUrls: ['./agency-chat.component.css']
})
export class AgencyChatComponent implements OnInit, OnDestroy {
  @ViewChild('messageContainer') messageContainer!: ElementRef;

  agencyId = signal<number | null>(null);
  myAgencies = signal<Agency[]>([]);
  isLoadingHistory = signal<boolean>(false);
  hasMoreHistory = signal<boolean>(true);
  currentPage = signal<number>(0);
  currentUserId: number | null = null;
  isObserver = signal<boolean>(false);
  isLead = signal<boolean>(false);
  
  replyTo = signal<ReplyPreviewDTO | null>(null);

  private typingTimeout: any = null;

  constructor(
    public chatService: AgencyChatService,
    private agencyService: AgencyService,
    private authService: AuthService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentAuthUser();
    if (user) {
      this.currentUserId = user.userId;
      this.loadMyAgencies();
    }
  }

  loadMyAgencies(): void {
    if (!this.currentUserId) return;
    this.agencyService.getMyAgencies(this.currentUserId).subscribe({
      next: (agencies: Agency[]) => {
        this.myAgencies.set(agencies);
        if (agencies.length > 0) {
          this.onAgencyChange(agencies[0].id);
        }
      },
      error: (err: any) => console.error(err)
    });
  }

  onAgencyChange(newAgencyId: number | string): void {
    const id = typeof newAgencyId === 'string' ? parseInt(newAgencyId, 10) : newAgencyId;
    this.agencyId.set(id);
    this.chatService.disconnect();
    this.currentPage.set(0);
    this.hasMoreHistory.set(true);
    this.replyTo.set(null);

    const token = this.authService.getAccessToken();
    if (token && this.currentUserId) {
      this.chatService.connect(id, token, this.currentUserId);
      this.loadInitialHistory();
      this.checkRole(id);
    }
  }

  checkRole(agencyId: number): void {
    this.chatService.getOnlineMembers(agencyId).subscribe({
      next: (res) => {
        const me = res.members.find(m => m.userId === this.currentUserId);
        this.isObserver.set(me?.role === 'OBSERVER');
        this.isLead.set(me?.role === 'LEAD');
      }
    });
  }

  loadInitialHistory(): void {
    this.isLoadingHistory.set(true);
    this.chatService.loadHistory(this.agencyId()!, 0, 30)
      .pipe(finalize(() => this.isLoadingHistory.set(false)))
      .subscribe({
        next: (page) => {
          const olderMessages = page.content.reverse();
          this.chatService.messages.update(msgs => [...olderMessages, ...msgs]);
          this.currentPage.set(1);
          this.hasMoreHistory.set(!page.last);
          setTimeout(() => this.scrollToBottom(), 50);
        },
        error: (err) => console.error(err)
      });
  }

  @HostListener('scroll', ['$event.target'])
  onScroll(target: HTMLElement): void {
    if (target.scrollTop === 0 && this.hasMoreHistory()) {
      this.loadOlderMessages();
    }
  }

  loadOlderMessages(): void {
    if (this.isLoadingHistory() || !this.hasMoreHistory()) return;
    this.isLoadingHistory.set(true);
    const prevScrollHeight = this.messageContainer.nativeElement.scrollHeight;
    
    this.chatService.loadHistory(this.agencyId()!, this.currentPage(), 30)
      .pipe(finalize(() => this.isLoadingHistory.set(false)))
      .subscribe({
        next: (page) => {
          const olderMessages = page.content.reverse();
          this.chatService.messages.update(msgs => [...olderMessages, ...msgs]);
          this.currentPage.update(p => p + 1);
          this.hasMoreHistory.set(!page.last);
          setTimeout(() => {
            const newScrollHeight = this.messageContainer.nativeElement.scrollHeight;
            this.messageContainer.nativeElement.scrollTop = newScrollHeight - prevScrollHeight;
          }, 0);
        },
        error: (err) => console.error(err)
      });
  }

  onSend(data: { text: string, attachments: AttachmentDTO[], taskRefId?: number }): void {
    if (this.isObserver()) return;
    if (!this.agencyId()) return;
    
    this.chatService.sendMessage(this.agencyId()!, data.text, data.attachments, this.replyTo()?.id, data.taskRefId);
    this.replyTo.set(null);
    setTimeout(() => this.scrollToBottom(), 50);
  }
  
  onTyping(isTyping: boolean): void {
    if (this.isObserver()) return;
    
    if (this.typingTimeout) clearTimeout(this.typingTimeout);
    this.chatService.sendTypingEvent(this.agencyId()!, isTyping);
    if (isTyping) {
      this.typingTimeout = setTimeout(() => {
        this.chatService.sendTypingEvent(this.agencyId()!, false);
      }, 2500);
    }
  }

  onReply(msg: ChatMessage) {
    this.replyTo.set({
      id: msg.id,
      senderFirstName: msg.senderFirstName,
      senderLastName: msg.senderLastName,
      messagePreview: msg.message.substring(0, 50),
      sentAt: msg.sentAt,
      hasAttachments: !!(msg.attachments && msg.attachments.length > 0)
    });
  }

  onReact(msgId: number, event: { emoji: string, remove: boolean }) {
    this.chatService.toggleReaction(this.agencyId()!, msgId, event.emoji, event.remove);
  }

  onDelete(msgId: number) {
    if (confirm("Êtes-vous sûr de vouloir supprimer ce message ?")) {
      this.chatService.deleteMessage(this.agencyId()!, msgId).subscribe({
        next: () => this.toastr.success("Message supprimé"),
        error: (err) => this.toastr.error("Erreur de suppression")
      });
    }
  }

  onPin(msgId: number, currentPinStatus: boolean) {
    this.chatService.pinMessage(this.agencyId()!, msgId, !currentPinStatus).subscribe({
      next: () => this.toastr.success(currentPinStatus ? "Message détaché" : "Message épinglé"),
      error: (err) => {
        this.toastr.error(err.error?.message || "Erreur lors de l'épinglage");
      }
    });
  }

  private scrollToBottom(): void {
    if (this.messageContainer && this.messageContainer.nativeElement) {
      const el = this.messageContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  isOwnMessage(msg: ChatMessage): boolean {
    return msg.senderId === this.currentUserId;
  }

  ngOnDestroy(): void {
    this.chatService.disconnect();
  }
}
