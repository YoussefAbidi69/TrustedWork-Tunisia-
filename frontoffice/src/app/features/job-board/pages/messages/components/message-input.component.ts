import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-message-input',
  template: `
    <div class="chat-input-area">
      <button class="attachment-btn"><i class="fas fa-paperclip"></i></button>
      <div class="input-wrapper">
        <textarea 
          [(ngModel)]="content" 
          placeholder="Type your message..." 
          (keydown.enter)="$event.preventDefault(); onSend()"
          [disabled]="disabled">
        </textarea>
      </div>
      <button class="send-btn" (click)="onSend()" [disabled]="!content.trim() || disabled">
        <i class="fas" [class.fa-paper-plane]="!disabled" [class.fa-spinner]="disabled" [class.fa-spin]="disabled"></i>
      </button>
    </div>
  `
})
export class MessageInputComponent {
  @Input() disabled = false;
  @Output() send = new EventEmitter<string>();
  
  content = '';

  onSend() {
    if (this.content.trim() && !this.disabled) {
      this.send.emit(this.content);
      this.content = '';
    }
  }
}
