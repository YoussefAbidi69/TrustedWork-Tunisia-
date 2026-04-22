import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-section-header',
  templateUrl: './section-header.component.html',
  styleUrls: ['./section-header.component.css']
})
export class SectionHeaderComponent {
  @Input() eyebrow = 'Section';
  @Input() title = 'Section title';
  @Input() subtitle = '';
  @Input() actionLabel = '';
}