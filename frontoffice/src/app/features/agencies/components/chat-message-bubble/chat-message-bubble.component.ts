import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatMessage, AttachmentDTO, ReactionSummaryDTO } from '../../services/agency-chat.service';
import { TaskCardEmbedComponent } from '../task-card-embed/task-card-embed.component';
import { AttachmentPreviewStripComponent } from '../attachment-preview-strip/attachment-preview-strip.component';
import { ReactionPickerComponent } from '../reaction-picker/reaction-picker.component';

@Component({
  selector: 'app-chat-message-bubble',
  standalone: true,
  imports: [CommonModule, TaskCardEmbedComponent, AttachmentPreviewStripComponent, ReactionPickerComponent],
  template: `
    <div class="group flex gap-3 mb-4 relative max-w-3xl" [class.flex-row-reverse]="isMine" [class.self-end]="isMine">
      
      <!-- Avatar -->
      <div class="flex-shrink-0" *ngIf="!isMine">
        @if (message.senderPhoto) {
          <img [src]="'http://localhost:8082' + message.senderPhoto" class="w-8 h-8 rounded-full object-cover border border-gray-200">
        } @else {
          <div class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold border border-blue-200 shadow-sm">
            {{ message.senderFirstName?.charAt(0) || 'U' }}
          </div>
        }
      </div>

      <!-- Message Content Container -->
      <div class="flex flex-col relative" [class.items-end]="isMine">
        
        <!-- Header -->
        <div class="flex items-baseline gap-2 mb-1 px-1" [class.flex-row-reverse]="isMine">
          <span class="text-xs font-semibold text-gray-700" *ngIf="!isMine">
            {{ message.senderFirstName }} {{ message.senderLastName }}
          </span>
          <span class="text-[10px] px-1.5 py-0.5 rounded bg-gray-100 text-gray-600 font-medium" *ngIf="!isMine">
            {{ message.senderRole }}
          </span>
          <span class="text-[10px] text-gray-400 font-medium">
            {{ formatTime(message.sentAt) }}
          </span>
          @if (message.isPinned) {
            <span class="text-[10px] text-orange-500 font-medium ml-1"><i class="fas fa-thumbtack"></i> Pinned</span>
          }
        </div>

        <!-- Bubble -->
        <div class="relative max-w-full">
          
          <!-- Actions Menu (Hover) -->
          <div class="absolute -top-3 hidden group-hover:flex items-center gap-1 bg-white border border-gray-200 rounded-md shadow-sm p-1 z-10"
               [class.-right-3]="!isMine" [class.-left-3]="isMine">
            <button class="text-gray-400 hover:text-gray-600 p-1 rounded hover:bg-gray-100 transition-colors relative group/react"
                    (click)="showReactionPicker = !showReactionPicker">
              <i class="fas fa-smile text-xs"></i>
              @if (showReactionPicker) {
                <div class="absolute bottom-full mb-2 left-1/2 -translate-x-1/2">
                  <app-reaction-picker (selectEmoji)="onReact($event)"></app-reaction-picker>
                </div>
              }
            </button>
            <button class="text-gray-400 hover:text-blue-500 p-1 rounded hover:bg-gray-100 transition-colors"
                    title="Reply" (click)="reply.emit()">
              <i class="fas fa-reply text-xs"></i>
            </button>
            @if (canPin) {
              <button class="text-gray-400 hover:text-orange-500 p-1 rounded hover:bg-gray-100 transition-colors"
                      title="Pin" (click)="pin.emit()">
                <i class="fas fa-thumbtack text-xs"></i>
              </button>
            }
            @if (canDelete) {
              <button class="text-gray-400 hover:text-red-500 p-1 rounded hover:bg-gray-100 transition-colors"
                      title="Delete" (click)="delete.emit()">
                <i class="fas fa-trash text-xs"></i>
              </button>
            }
          </div>

          <!-- The Bubble itself -->
          <div class="rounded-2xl px-4 py-2.5 shadow-sm min-w-[60px]"
               [ngClass]="{
                 'bg-blue-600 text-white rounded-tr-sm': isMine && !message.deleted,
                 'bg-white text-gray-800 border border-gray-100 rounded-tl-sm': !isMine && !message.deleted,
                 'bg-gray-100 text-gray-400 italic': message.deleted
               }">
            
            <!-- Reply Context -->
            @if (message.replyTo) {
              <div class="mb-2 pl-2 border-l-2 opacity-80 text-xs" [class.border-blue-300]="isMine" [class.border-gray-300]="!isMine">
                <span class="font-semibold block">{{ message.replyTo.senderFirstName }}</span>
                <span class="truncate block max-w-[200px]">{{ message.replyTo.messagePreview || 'Attachment' }}</span>
              </div>
            }

            <!-- Task Context -->
            @if (message.taskRef) {
              <div class="mb-2">
                <app-task-card-embed [task]="message.taskRef"></app-task-card-embed>
              </div>
            }

            <!-- Text Content -->
            <p class="whitespace-pre-wrap text-sm leading-relaxed" [innerHTML]="formatMessage(message.message)"></p>

            <!-- Attachments -->
            @if (message.attachments && message.attachments.length > 0 && !message.deleted) {
              <div class="mt-2 pt-2 border-t" [class.border-blue-500]="isMine" [class.border-gray-100]="!isMine">
                <app-attachment-preview-strip [attachments]="message.attachments" [editable]="false"></app-attachment-preview-strip>
              </div>
            }
          </div>

          <!-- Reactions Strip -->
          @if (message.reactions && message.reactions.length > 0) {
            <div class="flex flex-wrap gap-1 mt-1 px-1" [class.justify-end]="isMine">
              @for (reaction of message.reactions; track reaction.emoji) {
                <button class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-xs border transition-colors"
                        [ngClass]="reaction.reactedByMe ? 'bg-blue-50 border-blue-200 text-blue-700' : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'"
                        (click)="onReact(reaction.emoji)">
                  <span>{{ getEmojiIcon(reaction.emoji) }}</span>
                  <span class="font-medium">{{ reaction.count }}</span>
                </button>
              }
            </div>
          }
        </div>
      </div>
    </div>
  `
})
export class ChatMessageBubbleComponent {
  @Input() message!: ChatMessage;
  @Input() isMine = false;
  @Input() canDelete = false;
  @Input() canPin = false;

  @Output() reply = new EventEmitter<void>();
  @Output() react = new EventEmitter<{ emoji: string, remove: boolean }>();
  @Output() delete = new EventEmitter<void>();
  @Output() pin = new EventEmitter<void>();

  showReactionPicker = false;

  formatTime(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  formatMessage(text: string): string {
    if (!text) return '';
    // simple linkify
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    return text.replace(urlRegex, '<a href="$1" target="_blank" class="underline hover:text-blue-300">$1</a>');
  }

  onReact(emoji: string) {
    this.showReactionPicker = false;
    const existing = this.message.reactions?.find(r => r.emoji === emoji);
    const remove = existing?.reactedByMe || false;
    this.react.emit({ emoji, remove });
  }

  getEmojiIcon(key: string): string {
    const map: Record<string, string> = {
      'THUMBS_UP': '👍',
      'CHECK': '✅',
      'EYES': '👀',
      'FIRE': '🔥',
      'QUESTION': '❓',
      'PARTY': '🎉'
    };
    return map[key] || '👍';
  }
}
