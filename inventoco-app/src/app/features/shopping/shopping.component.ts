import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Purchase, PurchaseLine, Supplier } from '../../core/api/models';
import { ProfileStore } from '../../core/api/profile.store';
import { t } from '../../core/i18n/i18n';
import { date, money, qty, unitPrice } from '../../shared/format';

interface Line {
  source: PurchaseLine;
  bought: boolean;
  quantity: number | null;
  supplierId: string;
  amount: number | null;
}

/** What's missing and where it is cheapest; after the trip, what was bought and what it cost. */
@Component({
  selector: 'app-shopping',
  imports: [FormsModule, RouterLink],
  template: `
    <div class="title-row">
      <div>
        <h1>{{ t('Lista de compras', 'Shopping list') }}</h1>
        <p class="sub">{{ finding() }}</p>
      </div>
      <div class="seg-wrap">
        <div class="segmented" role="group" [attr.aria-label]="t('Para cuántos días', 'For how many days')">
          @for (h of horizons; track h) {
            <button type="button" [attr.aria-pressed]="horizon() === h" (click)="horizon.set(h)">{{ h }} {{ t('días', 'days') }}</button>
          }
        </div>
        <button class="btn" [class.primary]="!draft()" type="button" [disabled]="busy()" (click)="generate()">{{ draft() ? t('Rehacer lista', 'Rebuild list') : t('Armar lista', 'Build list') }}</button>
      </div>
    </div>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (done()) { <p class="toast" role="status">{{ t('Compra guardada. El inventario quedó al día.', 'Purchase saved. Your stock is up to date.') }}</p> }
    @if (loaded()) {
      @if (draft(); as d) {
        <div class="grid top">
          <section class="panel" style="grid-column: span 8" aria-labelledby="h-list">
            <header><h2 id="h-list">{{ t('Por comprar', 'To buy') }}</h2><span class="meta">{{ t('Armada el', 'Built on') }} {{ date(d.createdAt, zone()) }}</span></header>
            @if (lines().length) {
              <div class="rows">
                @for (l of lines(); track l.source.itemId) {
                  <div class="row buy" [class.off]="!l.bought">
                    <label class="check name"><input type="checkbox" [(ngModel)]="l.bought" [name]="'b' + $index">
                      <span><a [routerLink]="['/items', l.source.itemId]">{{ l.source.itemName }}</a><br>
                        <span class="faint" style="font-size:12px">{{ l.source.reason === 'LOW_STOCK' ? t('Bajo el mínimo', 'Under minimum') : t('Se acaba pronto', 'Runs out soon') }}@if (l.source.supplierName) { · {{ t('más barato en', 'cheapest at') }} {{ l.source.supplierName }} ({{ unitPrice(l.source.bestUnitPrice, d.currency) }}/{{ l.source.unit }}) }</span></span>
                    </label>
                    <div class="inputs">
                      <label class="field"><span class="hint">{{ t('Cantidad', 'Quantity') }} ({{ l.source.unit }})</span>
                        <input class="input fig" type="number" inputmode="decimal" min="0" step="any" [(ngModel)]="l.quantity" [name]="'q' + $index" [disabled]="!l.bought"></label>
                      <label class="field"><span class="hint">{{ t('Dónde', 'Where') }}</span>
                        <select class="input" [(ngModel)]="l.supplierId" [name]="'s' + $index" [disabled]="!l.bought">
                          <option value="">{{ t('Sin anotar', 'Not noted') }}</option>
                          @for (s of suppliers(); track s.id) { <option [value]="s.id">{{ s.name }}</option> }
                        </select></label>
                      <label class="field"><span class="hint">{{ t('Pagaste', 'You paid') }} ({{ d.currency }})</span>
                        <input class="input fig" type="number" inputmode="decimal" min="0" step="any" [(ngModel)]="l.amount" [name]="'a' + $index" [disabled]="!l.bought || !l.supplierId"></label>
                    </div>
                  </div>
                }
              </div>
              <div class="actions" style="margin-top:16px">
                <button class="btn primary" type="button" [disabled]="busy()" (click)="complete(d)">{{ t('Ya compré', 'I bought it') }}</button>
              </div>
            } @else {
              <p class="empty">{{ t('No falta nada para estos días.', 'Nothing is missing for these days.') }}</p>
            }
          </section>
          <section class="panel" style="grid-column: span 4" aria-labelledby="h-cost">
            <header><h2 id="h-cost">{{ t('Costo estimado', 'Estimated cost') }}</h2></header>
            <div class="lead">{{ money(d.estimatedTotal, d.currency) }}</div>
            <p class="muted" style="margin-top:4px">{{ t('Con el mejor precio conocido de cada producto.', 'At the best known price of each item.') }}</p>
            <div class="rows" style="margin-top:12px">
              <div class="row" style="justify-content:space-between"><span class="muted">{{ t('Ahorro frente a lo usual', 'Savings against usual') }}</span><span class="fig">{{ money(d.estimatedSavings, d.currency) }}</span></div>
              <div class="row" style="justify-content:space-between"><span class="muted">{{ t('Sin precio conocido', 'Without a known price') }}</span><span class="fig">{{ unknownPrices() }}</span></div>
            </div>
          </section>
        </div>
      } @else {
        <section class="panel">
          <p class="empty">{{ t('Arma la lista: incluye lo que está bajo el mínimo y lo que se acaba en los días que elijas.', 'Build the list: it includes what is under its minimum and what runs out within the days you choose.') }}</p>
        </section>
      }
      @if (history().length) {
        <section class="panel" style="margin-top:var(--gap)" aria-labelledby="h-hist">
          <header><h2 id="h-hist">{{ t('Compras anteriores', 'Past purchases') }}</h2></header>
          <div class="rows">
            @for (p of history(); track p.id) {
              <div class="row" style="justify-content:space-between"><span>{{ date(p.completedAt, zone(), true) }}</span><span class="faint">{{ p.lines.length ? p.lines.length + ' ' + t('productos sugeridos', 'suggested items') : t('Compra directa', 'Direct purchase') }}</span></div>
            }
          </div>
        </section>
      }
    } @else {
      <p class="loading">{{ t('Cargando…', 'Loading…') }}</p>
    }
  `,
  styles: `
    .buy { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr); gap: 12px 16px; align-items: end; }
    .buy .name { align-items: flex-start; color: var(--text); }
    .buy .name input { margin-top: 3px; }
    .buy .inputs { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; }
    .buy.off .name a { color: var(--faint); text-decoration: line-through; }
    @media (max-width: 860px) {
      .buy { grid-template-columns: minmax(0, 1fr); }
      .buy .inputs { grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr); }
      .buy .inputs .field:last-child { grid-column: 1 / -1; }
    }
  `
})
export default class ShoppingComponent implements OnInit {
  private api = inject(ApiService);
  private profile = inject(ProfileStore);

  readonly t = t; readonly money = money; readonly qty = qty; readonly unitPrice = unitPrice; readonly date = date;
  readonly horizons = [7, 14, 30];
  readonly horizon = signal(7);
  readonly draft = signal<Purchase | null>(null);
  readonly lines = signal<Line[]>([]);
  readonly suppliers = signal<Supplier[]>([]);
  readonly history = signal<Purchase[]>([]);
  readonly loaded = signal(false);
  readonly busy = signal(false);
  readonly done = signal(false);
  readonly error = signal<string | null>(null);
  readonly zone = computed(() => this.profile.timeZone());

  readonly finding = computed(() => {
    const d = this.draft();
    if (!d) return t('No hay una lista abierta.', 'There is no open list.');
    const n = d.lines.length;
    if (!n) return t('No falta nada.', 'Nothing is missing.');
    const items = n === 1 ? t('1 producto', '1 item') : t(`${n} productos`, `${n} items`);
    return d.estimatedSavings > 0
      ? t(`${items}. Comprando donde es más barato ahorras ${money(d.estimatedSavings, d.currency)}.`, `${items}. Buying where it is cheapest saves ${money(d.estimatedSavings, d.currency)}.`)
      : t(`${items} para los próximos ${d.horizonDays} días.`, `${items} for the next ${d.horizonDays} days.`);
  });
  readonly unknownPrices = computed(() => this.draft()?.lines.filter((l) => l.estimatedCost == null).length ?? 0);

  async ngOnInit() {
    try {
      const [draft, suppliers, history] = await Promise.all([this.api.draft(), this.api.suppliers(), this.api.purchases()]);
      this.suppliers.set(suppliers);
      this.history.set(history.slice(0, 6));
      this.show(draft);
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos cargar la lista.', 'We could not load the list.')));
    } finally {
      this.loaded.set(true);
    }
  }

  async generate() {
    this.busy.set(true);
    this.error.set(null);
    try {
      this.show(await this.api.generate(this.horizon()));
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos armar la lista.', 'We could not build the list.')));
    } finally {
      this.busy.set(false);
    }
  }

  async complete(d: Purchase) {
    const bought = this.lines().filter((l) => l.bought && l.quantity && l.quantity > 0);
    if (!bought.length) {
      this.error.set(t('Marca al menos un producto comprado.', 'Mark at least one item as bought.'));
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      await this.api.complete(d.id, bought.map((l) => ({
        itemId: l.source.itemId, quantity: l.quantity!, unit: l.source.unit,
        supplierId: l.supplierId || null, amount: l.supplierId ? l.amount : null
      })));
      this.show(null);
      this.history.set((await this.api.purchases()).slice(0, 6));
      this.done.set(true);
      setTimeout(() => this.done.set(false), 3500);
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos guardar la compra.', 'We could not save the purchase.')));
    } finally {
      this.busy.set(false);
    }
  }

  private show(d: Purchase | null) {
    this.draft.set(d);
    if (d) this.horizon.set(d.horizonDays);
    this.lines.set((d?.lines ?? []).map((l) => ({
      source: l, bought: true, quantity: l.suggestedQuantity, supplierId: l.supplierId ?? '', amount: null
    })));
  }
}
