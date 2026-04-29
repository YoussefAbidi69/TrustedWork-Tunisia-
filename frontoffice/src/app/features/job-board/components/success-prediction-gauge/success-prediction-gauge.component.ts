import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';

@Component({
  selector: 'app-success-prediction-gauge',
  templateUrl: './success-prediction-gauge.component.html',
  styleUrls: ['./success-prediction-gauge.component.scss']
})
export class SuccessPredictionGaugeComponent implements OnChanges, OnDestroy {
  @Input() probability = 0;
  @Input() confidenceLabel: string | null = null;
  @Input() size: 'full' | 'compact' = 'full';

  needleDeg = 0;
  showConfidence = false;
  private timer = 0;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['probability']) {
      window.clearTimeout(this.timer);
      const reduce =
        typeof window !== 'undefined' &&
        window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      const p = Math.min(1, Math.max(0, this.probability));
      this.needleDeg = p * 180;
      this.showConfidence = false;
      if (reduce) {
        this.showConfidence = true;
        return;
      }
      this.timer = window.setTimeout(() => (this.showConfidence = true), 1500);
    }
  }

  ngOnDestroy(): void {
    window.clearTimeout(this.timer);
  }

  zoneLabel(): string {
    const pct = this.probability * 100;
    if (pct < 40) {
      return 'Low';
    }
    if (pct <= 70) {
      return 'Moderate';
    }
    return 'High';
  }
}
