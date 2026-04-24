import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.component.html',
  styleUrls: ['./empty-state.component.css']
})
export class EmptyStateComponent {
  @Input() icon = 'fa-inbox';
  @Input() title = 'Nothing here yet';
  @Input() description = 'There is currently no content to display in this section.';
  @Input() actionLabel = '';
}