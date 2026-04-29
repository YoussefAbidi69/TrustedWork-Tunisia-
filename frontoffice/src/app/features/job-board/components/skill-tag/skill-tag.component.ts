import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-skill-tag',
  templateUrl: './skill-tag.component.html',
  styleUrls: ['./skill-tag.component.scss']
})
export class SkillTagComponent {
  @Input({ required: true }) skillName!: string;
  @Input() isAiDetected = false;
}
