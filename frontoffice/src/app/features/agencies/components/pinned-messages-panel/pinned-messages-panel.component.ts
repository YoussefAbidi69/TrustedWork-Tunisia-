import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatMessage } from '../../services/agency-chat.service';

@Component({
  selector: 'app-pinned-messages-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-white border-b border-gray-200 shadow-sm" *ngIf="pinnedMessages.length > 0">
      <div class="px-4 py-2 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition-colors"
           (click)="expanded = !expanded">
        <div class="flex items-center gap-2 text-sm">
          <i class="fas fa-thumbtack text-orange-500"></i>
          <span class="font-medium text-gray-700">{{ pinnedMessages.length }} Pinned Messages</span>
        </div>
        <i class="fas fa-chevron-down text-gray-400 transition-transform" [class.rotate-180]="expanded"></i>
      </div>
      
      @if (expanded) {
        <div class="border-t border-gray-100 max-h-64 overflow-y-auto bg-orange-50/30">
          @for (msg of pinnedMessages; track msg.id) {
            <div class="p-3 border-b border-gray-100 hover:bg-orange-50/50 transition-colors group relative">
              <div class="flex justify-between items-start mb-1">
                <span class="text-xs font-semibold text-gray-700">{{ msg.senderFirstName }} {{ msg.senderLastName }}</span>
                <span class="text-[10px] text-gray-500">{{ msg.sentAt | date:'short' }}</span>
              </div>
              <p class="text-sm text-gray-600 line-clamp-2">{{ msg.message }}</p>
              @if (msg.attachments && msg.attachments.length > 0) {
                <span class="text-xs text-blue-500 mt-1 block"><i class="fas fa-paperclip"></i> {{ msg.attachments.length }} attachments</span>
              }
              
              <div class="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button *ngIf="canUnpin" (click)="unpin.emit(msg.id); $event.stopPropagation()" class="p-1 text-gray-400 hover:text-red-500 bg-white rounded-full shadow-sm" title="Unpin">
                  <i class="fas fa-times text-xs"></i>
                </button>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class PinnedMessagesPanelComponent {
  @Input() pinnedMessages: ChatMessage[] = [];
  @Input() canUnpin = false;
  @Output() unpin = new EventEmitter<number>();
  
  expanded = false;
}
