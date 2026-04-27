import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  templateUrl: './stat-card.component.html',
  styleUrls: ['./stat-card.component.css']
})
export class StatCardComponent {
  @Input() label = 'Metric';
  @Input() value = '0';
  @Input() hint = '';
  @Input() trend = '';
  @Input() icon = 'fa-chart-line';
  @Input() tone: 'default' | 'accent' | 'success' | 'warning' = 'default';

  get toneClass(): string {
    return `stat-card--${this.tone}`;
  }
}