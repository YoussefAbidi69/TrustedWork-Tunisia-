import { Component, EventEmitter, Input, Output, ViewChild, ElementRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AttachmentDTO, ReplyPreviewDTO, TaskCardDTO, AgencyChatService } from '../../services/agency-chat.service';
import { AttachmentPreviewStripComponent } from '../attachment-preview-strip/attachment-preview-strip.component';
import { Subject, debounceTime } from 'rxjs';

@Component({
  selector: 'app-chat-input-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, AttachmentPreviewStripComponent],
  template: `
    <div class="chat-input-bar p-3 bg-white border-t border-gray-200">
      
      <!-- Reply Context -->
      @if (replyTo) {
        <div class="flex items-center justify-between bg-blue-50 border-l-4 border-blue-500 p-2 mb-2 rounded-r-md text-xs">
          <div class="flex flex-col">
            <span class="font-semibold text-blue-700">Reply to {{ replyTo.senderFirstName }}</span>
            <span class="text-gray-600 truncate max-w-md">{{ replyTo.messagePreview || 'Attachment' }}</span>
          </div>
          <button (click)="cancelReply.emit()" class="text-gray-400 hover:text-gray-600"><i class="fas fa-times"></i></button>
        </div>
      }

      <!-- Task Search Context -->
      @if (taskSearchMode) {
        <div class="bg-gray-50 border border-gray-200 rounded p-2 mb-2">
          <div class="flex justify-between items-center mb-2">
            <span class="text-xs font-semibold text-gray-600">Link a Task</span>
            <button (click)="toggleTaskSearch()" class="text-gray-400 hover:text-gray-600 text-xs"><i class="fas fa-times"></i></button>
          </div>
          <input type="text" [(ngModel)]="taskSearchQuery" (input)="onTaskSearch()" placeholder="Search tasks..." class="w-full text-xs p-1.5 border border-gray-300 rounded mb-2 focus:outline-none focus:ring-1 focus:ring-blue-500">
          
          <div class="max-h-32 overflow-y-auto space-y-1">
            @for (task of searchResults; track task.id) {
              <div (click)="selectTask(task)" class="flex justify-between items-center p-1.5 bg-white border border-gray-100 rounded cursor-pointer hover:bg-blue-50">
                <span class="text-xs truncate font-medium text-gray-700">{{ task.title }}</span>
                <span class="text-[9px] px-1.5 py-0.5 rounded-full bg-gray-100">{{ task.status }}</span>
              </div>
            }
          </div>
        </div>
      }

      @if (selectedTaskRef) {
        <div class="flex items-center justify-between bg-purple-50 border border-purple-200 p-1.5 mb-2 rounded text-xs">
          <div class="flex items-center gap-2">
            <i class="fas fa-tasks text-purple-500"></i>
            <span class="font-medium text-purple-700 truncate max-w-[200px]">{{ selectedTaskRef.title }}</span>
          </div>
          <button (click)="selectedTaskRef = null" class="text-gray-400 hover:text-gray-600"><i class="fas fa-times"></i></button>
        </div>
      }

      <app-attachment-preview-strip [attachments]="stagedAttachments" [editable]="true" (remove)="removeAttachment($event)"></app-attachment-preview-strip>

      <div class="flex items-end gap-2">
        <button class="p-2 text-gray-400 hover:text-blue-500 transition-colors" (click)="fileInput.click()">
          <i class="fas fa-paperclip text-lg"></i>
        </button>
        <input type="file" #fileInput multiple class="hidden" (change)="onFileSelected($event)">
        
        <button class="p-2 text-gray-400 hover:text-purple-500 transition-colors" (click)="toggleTaskSearch()" [class.text-purple-500]="taskSearchMode">
          <i class="fas fa-tasks text-lg"></i>
        </button>

        <div class="flex-1 relative bg-gray-100 rounded-2xl">
          <textarea
            [(ngModel)]="messageText"
            (input)="onTyping()"
            (keydown.enter)="onEnter($event)"
            placeholder="Écrivez votre message..."
            class="w-full bg-transparent border-none focus:ring-0 resize-none py-2 px-4 max-h-32 text-sm text-gray-700"
            rows="1"
            maxlength="2000"
            #msgInput
          ></textarea>
        </div>

        <div class="flex flex-col items-center justify-end">
          <span class="text-[10px] text-gray-400 mb-1">{{ messageText.length }}/2000</span>
          <button
            class="p-2.5 rounded-full bg-blue-600 text-white hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-md"
            [disabled]="!canSend()"
            (click)="onSend()"
          >
            <i class="fas fa-paper-plane text-sm"></i>
          </button>
        </div>
      </div>
    </div>
  `
})
export class ChatInputBarComponent implements OnInit {
  @Input() agencyId!: number;
  @Input() replyTo: ReplyPreviewDTO | null = null;
  @Output() send = new EventEmitter<{ text: string, attachments: AttachmentDTO[], taskRefId?: number }>();
  @Output() cancelReply = new EventEmitter<void>();
  @Output() typing = new EventEmitter<boolean>();

  messageText = '';
  stagedAttachments: AttachmentDTO[] = [];
  
  taskSearchMode = false;
  taskSearchQuery = '';
  searchResults: TaskCardDTO[] = [];
  selectedTaskRef: TaskCardDTO | null = null;
  
  private searchSubject = new Subject<string>();
  
  @ViewChild('msgInput') msgInput!: ElementRef<HTMLTextAreaElement>;

  constructor(private chatService: AgencyChatService) {}

  ngOnInit() {
    this.searchSubject.pipe(debounceTime(300)).subscribe(query => {
      this.chatService.searchTasks(this.agencyId, query).subscribe(res => this.searchResults = res);
    });
  }

  onTyping() {
    this.typing.emit(this.messageText.length > 0);
    // auto-resize textarea
    const el = this.msgInput.nativeElement;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 128) + 'px';
  }

  onEnter(event: Event) {
    event.preventDefault();
    this.onSend();
  }

  canSend(): boolean {
    return this.messageText.trim().length > 0 || this.stagedAttachments.length > 0;
  }

  onSend() {
    if (!this.canSend()) return;
    this.send.emit({
      text: this.messageText.trim(),
      attachments: [...this.stagedAttachments],
      taskRefId: this.selectedTaskRef?.id
    });
    this.messageText = '';
    this.stagedAttachments = [];
    this.selectedTaskRef = null;
    this.replyTo = null;
    this.typing.emit(false);
    this.msgInput.nativeElement.style.height = 'auto';
  }

  onFileSelected(event: any) {
    const files: FileList = event.target.files;
    if (files.length === 0) return;
    
    // Convert to array
    const fileArray: File[] = [];
    for (let i = 0; i < files.length; i++) fileArray.push(files[i]);
    
    this.chatService.uploadFiles(this.agencyId, fileArray).subscribe({
      next: (res) => {
        this.stagedAttachments.push(...res);
        event.target.value = '';
      },
      error: (err) => {
        console.error('Upload failed', err);
        alert('File upload failed. Max 5 files, 10MB each. Only specific formats allowed.');
      }
    });
  }

  removeAttachment(index: number) {
    this.stagedAttachments.splice(index, 1);
  }

  toggleTaskSearch() {
    this.taskSearchMode = !this.taskSearchMode;
    if (this.taskSearchMode) {
      this.searchSubject.next('');
    }
  }

  onTaskSearch() {
    this.searchSubject.next(this.taskSearchQuery);
  }

  selectTask(task: TaskCardDTO) {
    this.selectedTaskRef = task;
    this.taskSearchMode = false;
    this.taskSearchQuery = '';
  }
}
