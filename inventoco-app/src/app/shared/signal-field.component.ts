import { AfterViewInit, Component, ElementRef, OnDestroy, viewChild } from '@angular/core';

/**
 * Synapco signal field (synapco-design/final/shared/field.js): a faint dot lattice behind the
 * screen in the product's light. Dots near the pointer brighten and lean toward it; a tap sends
 * one ring outward. No idle animation; drawn once under prefers-reduced-motion.
 */
@Component({
  selector: 'app-signal-field',
  template: `<canvas #canvas id="field" aria-hidden="true"></canvas>`
})
export class SignalFieldComponent implements AfterViewInit, OnDestroy {
  private canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private cleanup: () => void = () => {};

  ngAfterViewInit() {
    const c = this.canvas().nativeElement;
    const x = c.getContext('2d');
    if (!x) return;
    const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const ink = getComputedStyle(document.documentElement).getPropertyValue('--field-ink').trim() || '#fff';
    const G = 24;
    const P = { reach: 220, base: 0.06, near: 0.26, grow: 0.7, lean: 0.06, cap: 0.5, speed: 360, band: 22, ring: 0.25, ringGrow: 0.6, life: 1400 };
    let W = 0, H = 0, px = -1e4, py = -1e4, raf = 0;
    let rings: { x: number; y: number; t: number }[] = [];

    const draw = (now: number) => {
      x.clearRect(0, 0, W, H);
      x.fillStyle = ink;
      for (let gy = G / 2; gy < H; gy += G) for (let gx = G / 2; gx < W; gx += G) {
        let a = P.base, r = 0.85, ox = 0, oy = 0;
        const dx = gx - px, dy = gy - py, dist = Math.hypot(dx, dy);
        if (dist < P.reach) { const k = (1 - dist / P.reach) ** 2; a += P.near * k; r += P.grow * k; ox = -dx * k * P.lean; oy = -dy * k * P.lean; }
        for (const p of rings) {
          const age = (now - p.t) / 1000, band = Math.abs(Math.hypot(gx - p.x, gy - p.y) - age * P.speed);
          if (band < P.band) { const q = (1 - band / P.band) * Math.max(0, 1 - (age * 1000) / P.life); a += P.ring * q; r += P.ringGrow * q; }
        }
        if (a < 0.03) continue;
        x.globalAlpha = Math.min(a, P.cap); x.beginPath(); x.arc(gx + ox, gy + oy, r, 0, 6.2832); x.fill();
      }
      x.globalAlpha = 1;
      rings = rings.filter((p) => now - p.t < P.life);
    };
    const tick = (now: number) => { draw(now); raf = rings.length ? requestAnimationFrame(tick) : 0; };
    const kick = () => { if (reduce) draw(performance.now()); else if (!raf) raf = requestAnimationFrame(tick); };
    const size = () => {
      const d = Math.min(2, devicePixelRatio || 1);
      W = innerWidth; H = innerHeight; c.width = W * d; c.height = H * d; x.setTransform(d, 0, 0, d, 0, 0);
      draw(performance.now());
    };
    const move = (e: PointerEvent) => { px = e.clientX; py = e.clientY; kick(); };
    const leave = () => { px = py = -1e4; kick(); };
    const down = (e: PointerEvent) => { if (!reduce) rings.push({ x: e.clientX, y: e.clientY, t: performance.now() }); kick(); };

    addEventListener('resize', size);
    addEventListener('pointermove', move);
    document.documentElement.addEventListener('pointerleave', leave);
    addEventListener('pointerdown', down);
    size();
    this.cleanup = () => {
      cancelAnimationFrame(raf);
      removeEventListener('resize', size);
      removeEventListener('pointermove', move);
      document.documentElement.removeEventListener('pointerleave', leave);
      removeEventListener('pointerdown', down);
    };
  }

  ngOnDestroy() {
    this.cleanup();
  }
}
