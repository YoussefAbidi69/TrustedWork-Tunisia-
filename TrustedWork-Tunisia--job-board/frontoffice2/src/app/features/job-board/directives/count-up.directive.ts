import {
  AfterViewInit,
  Directive,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  Renderer2,
  SimpleChanges
} from '@angular/core';

/** Animates a numeric text node from zero to a target value over a duration (ms). */
@Directive({
  selector: '[appCountUp], [countUp]',
  standalone: true
})
export class CountUpDirective implements AfterViewInit, OnChanges, OnDestroy {
  @Input('appCountUp') appCountUp = 0;
  @Input('countUp') countUpAlias = 0;
  @Input() countDuration = 1000;
  @Input('countUpDuration') countUpDuration = 1000;
  @Input() countDecimals = 0;
  @Input('countUpDecimals') countUpDecimalsInput: number | undefined;

  private raf = 0;
  private viewReady = false;

  constructor(
    private el: ElementRef<HTMLElement>,
    private renderer: Renderer2
  ) {}

  get target(): number {
    const v = this.countUpAlias || this.appCountUp;
    return typeof v === 'number' && !Number.isNaN(v) ? v : 0;
  }

  get duration(): number {
    return Math.max(1, this.countUpDuration || this.countDuration);
  }

  get decimals(): number {
    return this.countUpDecimalsInput != null ? this.countUpDecimalsInput : this.countDecimals;
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.run();
  }

  ngOnChanges(_changes: SimpleChanges): void {
    if (this.viewReady) {
      this.run();
    }
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.raf);
  }

  private run(): void {
    cancelAnimationFrame(this.raf);
    const reduce =
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduce) {
      this.renderer.setProperty(this.el.nativeElement, 'textContent', this.format(this.target));
      return;
    }
    const start = performance.now();
    const from = 0;
    const to = this.target;
    const dur = this.duration;
    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / dur);
      const eased = 1 - Math.pow(1 - t, 3);
      const val = from + (to - from) * eased;
      this.renderer.setProperty(this.el.nativeElement, 'textContent', this.format(val));
      if (t < 1) {
        this.raf = requestAnimationFrame(tick);
      }
    };
    this.raf = requestAnimationFrame(tick);
  }

  private format(v: number): string {
    return v.toFixed(this.decimals);
  }
}
