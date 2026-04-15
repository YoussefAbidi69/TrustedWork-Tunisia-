import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-community-page-header',
  templateUrl: './community-page-header.component.html',
  styleUrls: ['./community-page-header.component.css']
})
export class CommunityPageHeaderComponent {
  @Input() eyebrow = '';
  @Input() title = '';
  @Input() subtitle = '';
}
