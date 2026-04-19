import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-task-kanban',
  template: `
    <div class="empty-state">
      <i class="fas fa-tasks"></i>
      <h3>Tableau Kanban des Tâches</h3>
      <p>Le suivi visuel des tâches sera disponible prochainement.</p>
    </div>
  `,
  styles: [`
    .empty-state {
      padding: 4rem 2rem;
      text-align: center;
      background: #fdfdfd;
      border: 2px dashed #eee;
      border-radius: var(--radius-lg);
      color: #999;
    }
    .empty-state i { font-size: 3rem; margin-bottom: 1rem; color: #ddd; }
    .empty-state h3 { color: #555; margin-bottom: 0.5rem; }
  `]
})
export class TaskKanbanComponent {
  @Input() agencyId!: number;
}
