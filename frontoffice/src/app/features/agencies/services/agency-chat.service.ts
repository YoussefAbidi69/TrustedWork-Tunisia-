import { Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
const API_URL = 'http://localhost:8082/api';

export interface AttachmentDTO {
  id?: number;
  url: string;
  filename: string;
  fileType: string;
  fileSize: number;
}

export interface TaskCardDTO {
  id: number;
  title: string;
  status: string;
  priority: string;
  assigneeName?: string;
  assigneePhoto?: string;
  dueDate?: string;
  projectTitle?: string;
  progressPercent: number;
}

export interface ReplyPreviewDTO {
  id: number;
  senderFirstName: string;
  senderLastName: string;
  messagePreview: string;
  sentAt: string;
  hasAttachments: boolean;
}

export interface ReactionSummaryDTO {
  emoji: string;
  count: number;
  userIds: number[];
  reactedByMe: boolean;
}

export interface ChatMessage {
  id: number;
  agencyId: number;
  senderId: number;
  senderFirstName: string;
  senderLastName: string;
  senderPhoto: string;
  senderRole: string;
  message: string;
  sentAt: string;
  deleted: boolean;
  isPinned: boolean;
  attachments?: AttachmentDTO[];
  taskRef?: TaskCardDTO;
  replyTo?: ReplyPreviewDTO;
  reactions?: ReactionSummaryDTO[];
}

export interface PresenceEvent {
  agencyId: number;
  userId: number;
  firstName: string;
  lastName: string;
  photo: string;
  event: string;
  timestamp: string;
  currentOnlineMembers: OnlineMember[];
}

export interface TypingEvent {
  agencyId: number;
  userId: number;
  firstName: string;
  isTyping: boolean;
}

export interface ReactionEventDTO {
  messageId: number;
  emoji: string;
  userId: number;
  removed: boolean;
  updatedReactions: ReactionSummaryDTO[];
}

export interface OnlineMember {
  userId: number;
  firstName: string;
  lastName: string;
  photo: string;
  role: string;
  isOnline: boolean;
}

export interface OnlineMembersResponse {
  totalMembers: number;
  onlineCount: number;
  members: OnlineMember[];
}

export interface TaskStatusUpdateDTO {
  taskId: number;
  newStatus: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class AgencyChatService {
  private stompClient: Client | null = null;

  messages = signal<ChatMessage[]>([]);
  onlineMembers = signal<OnlineMember[]>([]);
  typingUsers = signal<TypingEvent[]>([]);
  connectionState = signal<'CONNECTING' | 'CONNECTED' | 'DISCONNECTED'>('DISCONNECTED');
  pinnedMessages = signal<ChatMessage[]>([]);

  constructor(private http: HttpClient) {}

  connect(agencyId: number, token: string, currentUserId: number): void {
    if (this.stompClient) {
      this.disconnect();
    }

    this.connectionState.set('CONNECTING');

    this.stompClient = new Client({
      webSocketFactory: () => {
        const Socket = (SockJS as any).default || SockJS;
        return new Socket(`${API_URL}/ws`);
      },
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      debug: (str) => console.log(str),

      onConnect: () => {
        this.connectionState.set('CONNECTED');
        this.stompClient!.subscribe(
          `/topic/agency/${agencyId}/messages`,
          (msg: IMessage) => {
            const chatMsg: ChatMessage = JSON.parse(msg.body);
            // Compute reactedByMe for initial load
            if (chatMsg.reactions) {
              chatMsg.reactions.forEach(r => r.reactedByMe = r.userIds.includes(currentUserId));
            }
            if (chatMsg.deleted) {
              this.messages.update(msgs =>
                msgs.map(m => m.id === chatMsg.id ? { ...m, message: '[Message supprimé]', deleted: true, attachments: [] } : m)
              );
            } else {
              this.messages.update(msgs => {
                const idx = msgs.findIndex(m => m.id === chatMsg.id);
                if (idx !== -1) {
                  const copy = [...msgs];
                  copy[idx] = chatMsg;
                  return copy;
                }
                return [...msgs, chatMsg];
              });
            }
            
            // Refresh pinned if necessary
            if (chatMsg.isPinned) {
              this.loadPinnedMessages(agencyId).subscribe(pinned => this.pinnedMessages.set(pinned));
            } else {
               this.pinnedMessages.update(pinned => pinned.filter(p => p.id !== chatMsg.id));
            }
          }
        );
        this.stompClient!.subscribe(
          `/topic/agency/${agencyId}/presence`,
          (msg: IMessage) => {
            const event: PresenceEvent = JSON.parse(msg.body);
            this.onlineMembers.set(event.currentOnlineMembers);
          }
        );
        this.stompClient!.subscribe(
          `/topic/agency/${agencyId}/typing`,
          (msg: IMessage) => {
            const event: TypingEvent = JSON.parse(msg.body);
            this.updateTypingUsers(event);
          }
        );
        this.stompClient!.subscribe(
          `/topic/agency/${agencyId}/reactions`,
          (msg: IMessage) => {
            const event: ReactionEventDTO = JSON.parse(msg.body);
            // process updated reactions
            event.updatedReactions.forEach(r => r.reactedByMe = r.userIds.includes(currentUserId));
            this.messages.update(msgs => msgs.map(m => {
              if (m.id === event.messageId) {
                return { ...m, reactions: event.updatedReactions };
              }
              return m;
            }));
          }
        );
        this.stompClient!.subscribe(
          `/topic/agency/${agencyId}/tasks`,
          (msg: IMessage) => {
            const event: TaskStatusUpdateDTO = JSON.parse(msg.body);
            this.messages.update(msgs => msgs.map(m => {
              if (m.taskRef?.id === event.taskId) {
                return { ...m, taskRef: { ...m.taskRef, status: event.newStatus } };
              }
              return m;
            }));
          }
        );
        
        this.loadPinnedMessages(agencyId).subscribe(pinned => this.pinnedMessages.set(pinned));
      },

      onDisconnect: () => this.connectionState.set('DISCONNECTED'),
      onStompError: (frame) => console.error('STOMP error', frame),
    });
    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.deactivate();
    }
    this.connectionState.set('DISCONNECTED');
    this.messages.set([]);
    this.onlineMembers.set([]);
    this.typingUsers.set([]);
    this.pinnedMessages.set([]);
  }

  sendMessage(agencyId: number, message: string, attachments?: AttachmentDTO[], replyToId?: number, taskRefId?: number): void {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.publish({
        destination: `/app/agency/${agencyId}/send`,
        body: JSON.stringify({ message, attachments, replyToId, taskRefId })
      });
    }
  }

  sendTypingEvent(agencyId: number, isTyping: boolean): void {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.publish({
        destination: `/app/agency/${agencyId}/typing`,
        body: JSON.stringify({ isTyping })
      });
    }
  }
  
  toggleReaction(agencyId: number, messageId: number, emoji: string, remove: boolean): void {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.publish({
        destination: `/app/agency/${agencyId}/react`,
        body: JSON.stringify({ messageId, emoji, remove })
      });
    }
  }

  private updateTypingUsers(event: TypingEvent): void {
    this.typingUsers.update(users => {
      const filtered = users.filter(u => u.userId !== event.userId);
      if (event.isTyping) {
        return [...filtered, event];
      }
      return filtered;
    });
  }

  loadHistory(agencyId: number, page: number, size: number = 30): Observable<any> {
    return this.http.get<any>(`${API_URL}/agencies/${agencyId}/chat/messages?page=${page}&size=${size}`);
  }

  getOnlineMembers(agencyId: number): Observable<OnlineMembersResponse> {
    return this.http.get<OnlineMembersResponse>(`${API_URL}/agencies/${agencyId}/chat/members/online`);
  }

  deleteMessage(agencyId: number, messageId: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/agencies/${agencyId}/chat/messages/${messageId}`);
  }
  
  uploadFiles(agencyId: number, files: File[]): Observable<AttachmentDTO[]> {
    const formData = new FormData();
    files.forEach(f => formData.append('files', f));
    return this.http.post<AttachmentDTO[]>(`${API_URL}/agencies/${agencyId}/chat/upload`, formData);
  }
  
  searchTasks(agencyId: number, query: string): Observable<TaskCardDTO[]> {
    return this.http.get<TaskCardDTO[]>(`${API_URL}/agencies/${agencyId}/chat/tasks/search?q=${encodeURIComponent(query)}`);
  }
  
  loadPinnedMessages(agencyId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${API_URL}/agencies/${agencyId}/chat/messages/pinned`);
  }
  
  pinMessage(agencyId: number, messageId: number, pin: boolean): Observable<void> {
    return this.http.patch<void>(`${API_URL}/agencies/${agencyId}/chat/messages/${messageId}/pin`, { pin });
  }
}
