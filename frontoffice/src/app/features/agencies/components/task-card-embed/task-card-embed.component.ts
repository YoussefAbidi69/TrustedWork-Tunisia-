import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskCardDTO } from '../../services/agency-chat.service';

@Component({
  selector: 'app-task-card-embed',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="task-card-embed shadow-sm rounded-lg border border-gray-200 bg-white p-3 mb-2 max-w-sm">
      <div class="flex justify-between items-start mb-2">
        <h4 class="text-sm font-semibold text-gray-800 line-clamp-1">{{ task.title }}</h4>
        <span class="px-2 py-0.5 text-xs font-medium rounded-full" [ngClass]="getPriorityClass(task.priority)">
          {{ task.priority }}
        </span>
      </div>
      <div class="flex items-center gap-2 mb-2 text-xs text-gray-500">
        <span class="font-medium px-2 py-0.5 rounded-md bg-gray-100">{{ task.status }}</span>
        @if (task.dueDate) {
          <span><i class="far fa-calendar-alt mr-1"></i>{{ task.dueDate | date:'shortDate' }}</span>
        }
      </div>
      <div class="flex justify-between items-center mt-3">
        <div class="flex -space-x-2">
          @if (task.assigneePhoto) {
            <img [src]="'http://localhost:8082' + task.assigneePhoto" class="w-6 h-6 rounded-full border-2 border-white" [title]="task.assigneeName">
          } @else if (task.assigneeName) {
            <div class="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-[10px] font-bold border-2 border-white" [title]="task.assigneeName">
              {{ task.assigneeName.charAt(0) }}
            </div>
          }
        </div>
        <div class="w-16">
          <div class="h-1.5 w-full bg-gray-200 rounded-full overflow-hidden">
            <div class="h-full bg-blue-500" [style.width.%]="task.progressPercent"></div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class TaskCardEmbedComponent {
  @Input({ required: true }) task!: TaskCardDTO;

  getPriorityClass(priority: string): string {
    switch(priority) {
      case 'URGENTE': return 'bg-red-100 text-red-700';
      case 'HAUTE': return 'bg-orange-100 text-orange-700';
      case 'MOYENNE': return 'bg-yellow-100 text-yellow-700';
      case 'FAIBLE': return 'bg-green-100 text-green-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  }
}
