import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../core/api/api.service';
import { Item, MovementType, UnitInfo } from '../core/api/models';
import { ProfileStore } from '../core/api/profile.store';
import { t } from '../core/i18n/i18n';
import { localInput, qty, toInstant } from './format';

/** One stock change: used, bought, wasted or counted. Quick on a phone: the quantity is the only required field. */
@Component({
  selector: 'app-movement-sheet',
  imports: [FormsModule],
  template: `
    <div class="sheet-backdrop" (click)="closed.emit()"></div>
    <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="mv-title" (keydown.escape)="closed.emit()">
      <h2 id="mv-title">{{ item().name }}</h2>
      <p class="sub">{{ t('Hay', 'In stock:') }} <span class="fig">{{ qty(item().stock) }}</span> {{ item().unit }}</p>
      <form class="form" (ngSubmit)="save()">
        <div class="segmented" role="group" [attr.aria-label]="t('Qué pasó', 'What happened')">
          @for (k of kinds; track k.type) {
            <button type="button" [attr.aria-pressed]="type() === k.type" (click)="type.set(k.type)">{{ t(k.es, k.en) }}</button>
          }
        </div>
        <div class="cols">
          <div class="field">
            <label for="mv-q">{{ type() === 'ADJUSTMENT' ? t('Cuánto hay ahora', 'How much there is now') : t('Cantidad', 'Quantity') }}</label>
            <input id="mv-q" class="input fig" type="number" inputmode="decimal" min="0" step="any" required [(ngModel)]="quantity" name="quantity" autofocus>
          </div>
          <div class="field">
            <label for="mv-u">{{ t('Unidad', 'Unit') }}</label>
            <select id="mv-u" class="input" [(ngModel)]="unit" name="unit">
              @for (u of units(); track u.symbol) { <option [value]="u.symbol">{{ u.symbol }}</option> }
            </select>
          </div>
        </div>
        <div class="field">
          <label for="mv-at">{{ t('Cuándo', 'When') }}</label>
          <input id="mv-at" class="input" type="datetime-local" [(ngModel)]="at" name="at">
          <span class="hint">{{ t('En tu zona horaria', 'In your time zone') }} ({{ zone() }})</span>
        </div>
        <div class="field">
          <label for="mv-note">{{ t('Nota (opcional)', 'Note (optional)') }}</label>
          <input id="mv-note" class="input" maxlength="500" [(ngModel)]="note" name="note">
        </div>
        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
        <div class="actions">
          <button class="btn" type="button" (click)="closed.emit()">{{ t('Cancelar', 'Cancel') }}</button>
          <button class="btn primary" type="submit" [disabled]="busy() || quantity == null">{{ t('Guardar', 'Save') }}</button>
        </div>
      </form>
    </section>
  `
})
export class MovementSheetComponent implements OnInit {
  private api = inject(ApiService);
  private profile = inject(ProfileStore);

  readonly item = input.required<Item>();
  readonly allUnits = input<UnitInfo[]>([]);
  readonly initialType = input<MovementType>('CONSUMPTION');
  readonly closed = output<void>();
  readonly saved = output<void>();

  readonly t = t; readonly qty = qty;
  readonly kinds = [
    { type: 'CONSUMPTION' as const, es: 'Usé', en: 'Used' },
    { type: 'PURCHASE' as const, es: 'Compré', en: 'Bought' },
    { type: 'WASTE' as const, es: 'Se dañó', en: 'Wasted' },
    { type: 'ADJUSTMENT' as const, es: 'Conté', en: 'Counted' }
  ];
  readonly type = signal<MovementType>('CONSUMPTION');
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly zone = computed(() => this.profile.timeZone());
  readonly units = computed(() => this.allUnits().filter((u) => u.dimension === this.item().dimension));

  quantity: number | null = null;
  unit = '';
  at = '';
  note = '';

  ngOnInit() {
    this.type.set(this.initialType());
    this.unit = this.item().unit;
    this.at = localInput(this.zone());
  }

  async save() {
    if (this.quantity == null) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.api.move(this.item().id, {
        type: this.type(), quantity: this.quantity, unit: this.unit,
        occurredAt: toInstant(this.at, this.zone()), note: this.note || null
      });
      this.saved.emit();
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos guardar el movimiento.', 'We could not save the movement.')));
    } finally {
      this.busy.set(false);
    }
  }
}
