import { Component, EventEmitter, Input, Output } from '@angular/core';

import { Community } from '../../../core/models/community.model';

@Component({
  selector: 'app-community-filter',
  templateUrl: './community-filter.component.html',
  styleUrls: ['./community-filter.component.css']
})
export class CommunityFilterComponent {
  @Input() communities: Community[] = [];
  @Input() activeCommunityId: number | null = null;

  @Output() selectCommunity = new EventEmitter<number | null>();

  selectAll(): void {
    this.selectCommunity.emit(null);
  }

  selectOne(communityId: number): void {
    this.selectCommunity.emit(communityId);
  }
}
