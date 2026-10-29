import { Component, computed, ElementRef, inject, input, OnDestroy, signal } from '@angular/core';
import { Price } from '../core/api/models';
import { t } from '../core/i18n/i18n';
import { date, unitPrice } from './format';

/** Unit price over time: one dot per recorded price, the line through them, cheapest and dearest labelled. */
@Component({
  selector: 'app-price-chart',
  host: { style: 'display: block' },
  template: `
    @if (geometry(); as g) {
      <figure class="price-chart" [attr.aria-label]="t('Precio por ', 'Price per ') + unit() + ' ' + t('en el tiempo', 'over time')">
        <svg [attr.viewBox]="'0 0 ' + W() + ' ' + H" role="img">
          @for (y of g.ticks; track y.v) {
            <line class="grid-line" [attr.x1]="L" [attr.x2]="W()" [attr.y1]="y.y" [attr.y2]="y.y" />
            <text class="axis" [attr.x]="L - 8" [attr.y]="y.y + 4" text-anchor="end">{{ y.label }}</text>
          }
          <polyline class="line" [attr.points]="g.line" />
          @for (p of g.points; track p.id) {
            <circle class="dot" [attr.cx]="p.x" [attr.cy]="p.y" r="3.5"><title>{{ p.title }}</title></circle>
          }
          <text class="axis" [attr.x]="L" [attr.y]="H - 2">{{ g.from }}</text>
          <text class="axis" [attr.x]="W()" [attr.y]="H - 2" text-anchor="end">{{ g.to }}</text>
        </svg>
      </figure>
    }
  `
})
export class PriceChartComponent implements OnDestroy {
  readonly prices = input.required<Price[]>();
  readonly currency = input.required<string>();
  readonly unit = input.required<string>();
  readonly timeZone = input.required<string>();
  readonly t = t;
  /** Drawn at the width it is shown, so labels keep their size on a phone. */
  readonly W = signal(640);
  readonly H = 200; readonly L = 64; readonly T = 8; readonly B = 22;
  private observer = new ResizeObserver(([e]) => this.W.set(Math.max(280, Math.round(e.contentRect.width))));

  constructor() {
    this.observer.observe(inject(ElementRef).nativeElement);
  }

  ngOnDestroy() {
    this.observer.disconnect();
  }

  readonly geometry = computed(() => {
    const list = [...this.prices()].sort((a, b) => a.observedAt.localeCompare(b.observedAt));
    if (list.length < 2) return null;
    const times = list.map((p) => new Date(p.observedAt).getTime());
    const values = list.map((p) => p.unitPrice);
    const t0 = Math.min(...times), t1 = Math.max(...times);
    let lo = Math.min(...values), hi = Math.max(...values);
    if (lo === hi) { lo *= 0.9; hi *= 1.1; }
    const pad = (hi - lo) * 0.15; lo = Math.max(0, lo - pad); hi += pad;
    const x = (time: number) => this.L + (t1 === t0 ? 0.5 : (time - t0) / (t1 - t0)) * (this.W() - this.L - 8);
    const y = (v: number) => this.T + (1 - (v - lo) / (hi - lo)) * (this.H - this.T - this.B);
    const points = list.map((p, i) => ({
      id: p.id, x: x(times[i]), y: y(p.unitPrice),
      title: `${p.supplierName ?? ''} · ${unitPrice(p.unitPrice, this.currency())} / ${this.unit()} · ${date(p.observedAt, this.timeZone(), true)}`
    }));
    const ticks = [lo, (lo + hi) / 2, hi].map((v) => ({ v, y: y(v), label: unitPrice(v, this.currency()) }));
    return {
      points, ticks,
      line: points.map((p) => `${p.x},${p.y}`).join(' '),
      from: date(list[0].observedAt, this.timeZone(), true),
      to: date(list[list.length - 1].observedAt, this.timeZone(), true)
    };
  });
}
