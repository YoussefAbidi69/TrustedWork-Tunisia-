import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.scss']
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: string;
  @Input() type: 'job' | 'application' = 'job';

  klass(): string {
    const s = (this.status || '').toUpperCase();
    if (this.type === 'job') {
      if (s === 'PUBLISHED') {
        return 'jb-badge jb-badge--success';
      }
      if (s === 'DRAFT') {
        return 'jb-badge jb-badge--muted';
      }
      if (s === 'CLOSED') {
        return 'jb-badge jb-badge--danger';
      }
      if (s === 'FLAGGED') {
        return 'jb-badge jb-badge--warn';
      }
      return 'jb-badge jb-badge--muted';
    }
    if (s === 'PENDING') {
      return 'jb-badge jb-badge--info';
    }
    if (s === 'SHORTLISTED') {
      return 'jb-badge jb-badge--accent';
    }
    if (s === 'ACCEPTED') {
      return 'jb-badge jb-badge--success';
    }
    if (s === 'REJECTED') {
      return 'jb-badge jb-badge--danger';
    }
    if (s === 'WITHDRAWN') {
      return 'jb-badge jb-badge--muted';
    }
    return 'jb-badge jb-badge--muted';
  }
}
