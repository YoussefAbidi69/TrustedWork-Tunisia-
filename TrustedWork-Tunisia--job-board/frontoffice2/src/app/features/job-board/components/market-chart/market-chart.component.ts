import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { MarketInsight, TrendDirection } from '../../models/job-board.models';

@Component({
  selector: 'app-market-chart',
  templateUrl: './market-chart.component.html',
  styleUrls: ['./market-chart.component.scss']
})
export class MarketChartComponent implements OnChanges {
  @Input() insights: MarketInsight[] = [];

  maxCount = 1;
  sorted: MarketInsight[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['insights']) {
      this.sorted = [...(this.insights || [])].sort((a, b) => b.count - a.count);
      this.maxCount = Math.max(1, ...this.sorted.map((i) => i.count));
    }
  }

  barClass(t: TrendDirection): string {
    if (t === 'RISING') {
      return 'mc-bar mc-bar--rise';
    }
    if (t === 'DECLINING') {
      return 'mc-bar mc-bar--fall';
    }
    return 'mc-bar mc-bar--stable';
  }

  icon(t: TrendDirection): string {
    if (t === 'RISING') {
      return 'fa-arrow-trend-up';
    }
    if (t === 'DECLINING') {
      return 'fa-arrow-trend-down';
    }
    return 'fa-minus';
  }
}
