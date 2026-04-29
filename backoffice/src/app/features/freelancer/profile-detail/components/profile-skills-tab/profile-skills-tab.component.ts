import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Endorsement, Skill } from '../../../../../core/models/freelancer.model';

interface EndorsementViewModel extends Endorsement {
  endorserFullName: string;
  endorserInitials: string;
}

@Component({
  selector: 'app-profile-skills-tab',
  templateUrl: './profile-skills-tab.component.html',
  styleUrls: ['./profile-skills-tab.component.css']
})
export class ProfileSkillsTabComponent {
  @Input() skills: Skill[] = [];
  @Input() selectedSkill: Skill | null = null;
  @Input() endorsements: EndorsementViewModel[] = [];
  @Input() endorsementsLoading = false;
  @Input() endorsementsError = '';

  @Output() skillSelected = new EventEmitter<Skill>();
  @Output() endorsementsClosed = new EventEmitter<void>();
  @Output() skillDeleted = new EventEmitter<number>();
}