import { Component, Input } from '@angular/core';
import { FraudSignalDto } from '../../models/job-board.models';

@Component({
  selector: 'app-fraud-shield',
  templateUrl: './fraud-shield.component.html',
  styleUrls: ['./fraud-shield.component.scss']
})
export class FraudShieldComponent {
  readonly Math = Math;

  @Input() fraudRiskScore = 0;
  @Input() signals: FraudSignalDto[] = [];
  @Input() size: 'full' | 'compact' = 'full';
  expanded = false;

  tier(): 'low' | 'mid' | 'high' {
    if (this.fraudRiskScore < 0.3) {
      return 'low';
    }
    if (this.fraudRiskScore <= 0.6) {
      return 'mid';
    }
    return 'high';
  }
}
