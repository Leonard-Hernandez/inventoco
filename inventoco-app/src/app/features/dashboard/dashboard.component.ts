import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Dashboard } from '../../core/api/models';
import { ProfileStore } from '../../core/api/profile.store';
import { t } from '../../core/i18n/i18n';
import { confidence, fill, money, qty, runsOut } from '../../shared/format';

/** Home: what to buy soon, what the user saved and where it is cheapest. */
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  template: `
    <div class="title-row">
      <div>
        <h1>{{ t('Inicio', 'Home') }}</h1>
        <p class="sub">{{ finding() }}</p>
      </div>
      <button class="btn primary" type="button" [disabled]="busy()" (click)="buildList()">{{ t('Armar lista de compras', 'Build the shopping list') }}</button>
    </div>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (data(); as d) {
      <div class="grid top">
        <section class="panel" style="grid-column: span 8" aria-labelledby="h-soon">
          <header><h2 id="h-soon">{{ t('Por comprar pronto', 'Buy soon') }}</h2><a class="card-link" routerLink="/items">{{ t('Ver inventario', 'See all items') }}</a></header>
          @if (d.buySoon.length) {
            <div class="rows">
              @for (f of d.buySoon; track f.itemId) {
                <a class="row item-row" [routerLink]="['/items', f.itemId]">
                  <span><span class="name">{{ f.name }}</span><br><span class="meta">{{ runsOut(f.daysLeft) }} · {{ confidence(f.confidence) }}</span></span>
                  <span class="fig">{{ qty(f.stock) }}<small>{{ f.unit }}</small></span>
                  <span class="stockbar" role="img" [attr.aria-label]="t('Mínimo ', 'Minimum ') + qty(f.minStock) + ' ' + f.unit">
                    <i [class.low]="f.lowStock" [style.width.%]="fill(f.stock, f.minStock).width"></i>
                    @if (fill(f.stock, f.minStock).min; as m) { <b [style.left.%]="m"></b> }
                  </span>
                </a>
              }
            </div>
          } @else {
            <p class="empty">{{ t('Nada se acaba en los próximos 7 días.', 'Nothing runs out in the next 7 days.') }}</p>
          }
        </section>
        <section class="panel" style="grid-column: span 4" aria-labelledby="h-save">
          <header><h2 id="h-save">{{ t('Ahorro', 'Savings') }}</h2><span class="meta">{{ t('Últimos 90 días', 'Last 90 days') }}</span></header>
          <div class="lead">{{ money(d.savings.realized, d.savings.currency) }}</div>
          <p class="muted" style="margin-top:4px">{{ savingsLine() }}</p>
          <div class="rows" style="margin-top:12px">
            <div class="row" style="justify-content:space-between"><span class="muted">{{ t('Gastado', 'Spent') }}</span><span class="fig">{{ money(d.savings.spent, d.savings.currency) }}</span></div>
            <div class="row" style="justify-content:space-between"><span class="muted">{{ t('Ahorro posible en la lista', 'Possible savings on the list') }}</span><span class="fig">{{ money(d.savings.estimatedOnShoppingList, d.savings.currency) }}</span></div>
          </div>
        </section>
        <section class="panel" style="grid-column: span 12" aria-labelledby="h-sup">
          <header><h2 id="h-sup">{{ t('Dónde te sale más barato', 'Where it costs you less') }}</h2><a class="card-link" routerLink="/suppliers">{{ t('Ver proveedores', 'See suppliers') }}</a></header>
          @if (d.topSuppliers.length) {
            <div class="table-wrap"><table>
              <thead><tr><th>{{ t('Proveedor', 'Supplier') }}</th><th class="n">{{ t('Frente al precio típico', 'Against the typical price') }}</th><th class="n">{{ t('Más barato en', 'Cheapest for') }}</th><th class="n">{{ t('Precios', 'Prices') }}</th></tr></thead>
              <tbody>
                @for (s of d.topSuppliers; track s.supplierId) {
                  <tr><td>{{ s.name }}@if (s.city) {<span class="sub">{{ s.city }}</span>}</td>
                    <td class="n"><span class="fig">{{ index(s.priceIndex) }}</span></td>
                    <td class="n"><span class="fig">{{ s.itemsCheapest }}</span> {{ t('productos', 'items') }}</td>
                    <td class="n"><span class="fig">{{ s.prices }}</span></td></tr>
                }
              </tbody>
            </table></div>
          } @else {
            <p class="empty">{{ t('Registra precios en dos o más proveedores para compararlos.', 'Record prices at two or more suppliers to compare them.') }}</p>
          }
        </section>
      </div>
    } @else if (!error()) {
      <p class="loading">{{ t('Cargando…', 'Loading…') }}</p>
    }
  `
})
export default class DashboardComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private profile = inject(ProfileStore);

  readonly data = signal<Dashboard | null>(null);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly t = t; readonly money = money; readonly qty = qty; readonly runsOut = runsOut; readonly confidence = confidence; readonly fill = fill;

  readonly finding = computed(() => {
    const d = this.data();
    if (!d) return '';
    const n = d.buySoon.length;
    if (!n) return t('Tienes de todo para la próxima semana.', 'You have everything for the coming week.');
    const save = d.savings.estimatedOnShoppingList;
    const first = n === 1 ? t('1 producto se acaba o está bajo el mínimo esta semana', '1 item runs out or is under its minimum this week')
      : t(`${n} productos se acaban o están bajo el mínimo esta semana`, `${n} items run out or are under their minimum this week`);
    return save > 0 ? `${first}. ${t('Comprando donde es más barato ahorras', 'Buying where it is cheapest saves')} ${money(save, d.savings.currency)}.` : `${first}.`;
  });

  readonly savingsLine = computed(() => {
    const s = this.data()?.savings;
    if (!s || !s.purchases) return t('Aún no hay compras registradas con precio.', 'No purchases with a price recorded yet.');
    return s.realized >= 0
      ? t(`Frente a lo que sueles pagar, en ${s.purchases} compras.`, `Against what you usually pay, over ${s.purchases} purchases.`)
      : t(`Pagaste más de lo usual en ${s.purchases} compras.`, `You paid more than usual over ${s.purchases} purchases.`);
  });

  async ngOnInit() {
    try {
      this.data.set(await this.api.dashboard());
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos cargar el inicio.', 'We could not load the home screen.')));
    }
  }

  async buildList() {
    this.busy.set(true);
    try {
      await this.api.generate(7);
      await this.router.navigateByUrl('/shopping');
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos armar la lista.', 'We could not build the list.')));
    } finally {
      this.busy.set(false);
    }
  }

  /** 0.92 -> "8 % less" / "8 % más barato" */
  index(i: number): string {
    const pct = Math.round((1 - i) * 100);
    if (pct === 0) return t('Igual', 'Same');
    return pct > 0 ? t(`${pct} % más barato`, `${pct}% cheaper`) : t(`${-pct} % más caro`, `${-pct}% dearer`);
  }

  currency() { return this.profile.currency(); }
}
