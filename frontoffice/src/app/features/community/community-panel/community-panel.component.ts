import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-community-panel',
  templateUrl: './community-panel.component.html',
  styleUrls: ['./community-panel.component.css']
})
export class CommunityPanelComponent {
  @Input() title = '';
}
