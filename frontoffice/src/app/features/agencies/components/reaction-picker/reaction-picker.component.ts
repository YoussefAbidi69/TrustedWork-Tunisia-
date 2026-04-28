import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-reaction-picker',
  standalone: true,
  template: `
    <div class="reaction-picker-menu">
      @for (emoji of emojis; track emoji.key) {
        <button (click)="onSelect(emoji.key)" class="emoji-btn">
          {{ emoji.icon }}
        </button>
      }
    </div>
  `,
  styles: [`
    .reaction-picker-menu {
      display: flex; gap: 4px; padding: 4px; background: white;
      border: 1px solid #e2e8f0; border-radius: 999px;
      box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
    }
    .emoji-btn {
      background: transparent; border: none; font-size: 1.25rem;
      cursor: pointer; padding: 4px; border-radius: 999px;
      transition: transform 0.2s;
    }
    .emoji-btn:hover { transform: scale(1.2); background: #f1f5f9; }
  `]
})
export class ReactionPickerComponent {
  @Output() selectEmoji = new EventEmitter<string>();
  emojis = [
    { key: 'THUMBS_UP', icon: '👍' },
    { key: 'CHECK', icon: '✅' },
    { key: 'EYES', icon: '👀' },
    { key: 'FIRE', icon: '🔥' },
    { key: 'QUESTION', icon: '❓' },
    { key: 'PARTY', icon: '🎉' }
  ];
  onSelect(key: string) { this.selectEmoji.emit(key); }
}
