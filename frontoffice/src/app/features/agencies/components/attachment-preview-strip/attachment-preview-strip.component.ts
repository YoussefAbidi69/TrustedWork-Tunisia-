import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AttachmentDTO } from '../../services/agency-chat.service';

@Component({
  selector: 'app-attachment-preview-strip',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-wrap gap-2 mb-2 p-2 bg-gray-50 rounded-lg border border-gray-100" *ngIf="attachments.length > 0">
      @for(file of attachments; track file.url; let i = $index) {
        <div class="relative group flex items-center bg-white border border-gray-200 rounded p-1.5 shadow-sm max-w-[200px]">
          @if(isImage(file.fileType)) {
            <img [src]="'http://localhost:8082' + file.url" class="w-8 h-8 object-cover rounded mr-2">
          } @else {
            <div class="w-8 h-8 bg-blue-100 text-blue-600 flex items-center justify-center rounded mr-2">
              <i class="fas fa-file-alt text-sm"></i>
            </div>
          }
          <div class="flex-1 min-w-0">
            <p class="text-[10px] font-medium text-gray-700 truncate" [title]="file.filename">{{ file.filename }}</p>
            <p class="text-[9px] text-gray-500">{{ formatSize(file.fileSize) }}</p>
          </div>
          @if(editable) {
            <button (click)="onRemove(i)" class="absolute -top-1.5 -right-1.5 w-4 h-4 bg-red-500 text-white rounded-full flex items-center justify-center text-[10px] opacity-0 group-hover:opacity-100 transition-opacity shadow-sm">
              <i class="fas fa-times"></i>
            </button>
          } @else {
             <a [href]="'http://localhost:8082' + file.url" target="_blank" class="absolute inset-0 z-10"></a>
          }
        </div>
      }
    </div>
  `
})
export class AttachmentPreviewStripComponent {
  @Input() attachments: AttachmentDTO[] = [];
  @Input() editable = false;
  @Output() remove = new EventEmitter<number>();

  isImage(type: string): boolean {
    return type?.startsWith('image/');
  }

  formatSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  onRemove(index: number) {
    this.remove.emit(index);
  }
}
