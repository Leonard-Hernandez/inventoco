import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Forecast, Item, Movement, Price, PublicPrice } from '../../core/api/models';
import { ProfileStore } from '../../core/api/profile.store';
import { t } from '../../core/i18n/i18n';
import { confidence, date, dateTime, fill, money, movementLabel, qty, runsOut, unitPrice } from '../../shared/format';
import { ItemFormSheetComponent } from '../../shared/item-form-sheet.component';
import { MovementSheetComponent } from '../../shared/movement-sheet.component';
import { PriceChartComponent } from '../../shared/price-chart.component';
import { PriceSheetComponent } from '../../shared/price-sheet.component';
import { UnitsStore } from '../../shared/units.store';

/** One item: how long it lasts, what it costs where, and what others pay for it. */
@Component({
  selector: 'app-item-detail',
  imports: [RouterLink, MovementSheetComponent, PriceSheetComponent, ItemFormSheetComponent, PriceChartComponent],
  template: `
    @if (item(); as i) {
      <div class="title-row">
        <div>
          <h1>{{ i.name }}</h1>
          <p class="sub">{{ finding() }}</p>
        </div>
        <div class="seg-wrap">
          <button class="btn" type="button" (click)="editing.set(true)">{{ t('Editar', 'Edit') }}</button>
          <button class="btn primary" type="button" (click)="moving.set(true)">{{ t('Registrar movimiento', 'Record a movement') }}</button>
        </div>
      </div>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
      <div class="grid top">
        <section class="panel" style="grid-column: span 8" aria-labelledby="h-stock">
          <header><h2 id="h-stock">{{ t('Existencias', 'Stock') }}</h2>@if (i.category) {<span class="meta">{{ i.category }}</span>}</header>
          <div class="lead">{{ qty(i.stock) }}<small>{{ i.unit }}</small></div>
          <p class="muted" style="margin:4px 0 14px">{{ t('Mínimo', 'Minimum') }} <span class="fig">{{ qty(i.minStock) }}</span> {{ i.unit }}@if (i.lowStock) { · <span class="pill warn">{{ t('Bajo el mínimo', 'Under minimum') }}</span> }</p>
          <div class="stockbar" role="img" [attr.aria-label]="t('Mínimo ', 'Minimum ') + qty(i.minStock) + ' ' + i.unit">
            <i [class.low]="i.lowStock" [style.width.%]="fill(i.stock, i.minStock).width"></i>
            @if (fill(i.stock, i.minStock).min; as m) { <b [style.left.%]="m"></b> }
          </div>
          <div class="ruler" aria-hidden="true"></div>
          @if (forecast(); as f) {
            <div class="kpis" style="margin-top:18px">
              <div class="kpi"><div class="kpi-label">{{ t('Uso diario', 'Daily use') }}</div><div class="kpi-value fig">{{ qty(f.dailyUse) }}<small>{{ i.unit }}</small></div></div>
              <div class="kpi"><div class="kpi-label">{{ t('Se acaba', 'Runs out') }}</div><div class="kpi-value fig">{{ f.depletesAt ? date(f.depletesAt, zone()) : '—' }}</div></div>
              <div class="kpi"><div class="kpi-label">{{ t('Predicción', 'Forecast') }}</div><div class="kpi-value"><span class="pill" [class.pos]="f.confidence === 'HIGH'" [class.warn]="f.confidence === 'LOW'">{{ confidence(f.confidence) }}</span></div></div>
            </div>
          }
        </section>
        <section class="panel" style="grid-column: span 4" aria-labelledby="h-mv">
          <header><h2 id="h-mv">{{ t('Movimientos', 'Movements') }}</h2><span class="meta">{{ t('Últimos', 'Latest') }}</span></header>
          @if (movements().length) {
            <div class="rows">
              @for (m of movements().slice(0, 8); track m.id) {
                <div class="row" style="justify-content:space-between">
                  <span>{{ movementLabel(m.type) }}<br><span class="faint" style="font-size:12px">{{ dateTime(m.occurredAt, zone()) }}@if (m.note) { · {{ m.note }} }</span></span>
                  <span class="fig">{{ m.quantity > 0 ? '+' : '' }}{{ qty(m.quantity) }}<small>{{ m.unit }}</small></span>
                </div>
              }
            </div>
          } @else {
            <p class="empty">{{ t('Registra lo que usas para predecir cuándo se acaba.', 'Record what you use to forecast when it runs out.') }}</p>
          }
        </section>
        <section class="panel" style="grid-column: span 8" aria-labelledby="h-pr">
          <header><h2 id="h-pr">{{ t('Precios', 'Prices') }}</h2><button class="btn" type="button" (click)="pricing.set(true)">{{ t('Anotar precio', 'Record a price') }}</button></header>
          @if (prices().length) {
            <div class="chart-head"><h3>{{ t('Precio por', 'Price per') }} {{ i.unit }}</h3><span class="legend">{{ priceSpan() }}</span></div>
            <app-price-chart [prices]="prices()" [currency]="currency()" [unit]="i.unit" [timeZone]="zone()" />
            <div class="table-wrap" style="margin-top:14px"><table>
              <thead><tr><th>{{ t('Dónde', 'Where') }}</th><th class="n hide-phone">{{ t('Paquete', 'Package') }}</th><th class="n hide-phone">{{ t('Precio', 'Price') }}</th><th class="n">{{ t('Por', 'Per') }} {{ i.unit }}</th><th class="n">{{ t('Fecha', 'Date') }}</th></tr></thead>
              <tbody>
                @for (p of prices(); track p.id) {
                  <tr>
                    <td>{{ p.supplierName }}@if (p.visibility === 'PUBLIC') {<span class="sub">{{ t('Compartido', 'Shared') }}</span>}</td>
                    <td class="n hide-phone"><span class="fig">{{ qty(p.packageQuantity) }}</span> {{ p.packageUnit }}</td>
                    <td class="n hide-phone"><span class="fig">{{ money(p.amount, p.currency) }}</span></td>
                    <td class="n"><span class="fig">{{ unitPrice(p.unitPrice, p.currency) }}</span></td>
                    <td class="n"><span class="fig">{{ date(p.observedAt, zone(), true) }}</span></td>
                  </tr>
                }
              </tbody>
            </table></div>
          } @else {
            <p class="empty">{{ t('Anota lo que pagas y dónde: así ves cuál proveedor te conviene.', 'Write down what you pay and where: then you see which supplier suits you.') }}</p>
          }
        </section>
        <section class="panel" style="grid-column: span 4" aria-labelledby="h-pub">
          <header><h2 id="h-pub">{{ t('Lo que pagan otros', 'What others pay') }}</h2><span class="meta">{{ t('90 días', '90 days') }}</span></header>
          @if (!i.catalogProductId) {
            <p class="empty">{{ t('Agrega el código de barras o vincúlalo al catálogo para ver precios de otros.', 'Add the barcode or link it to the catalog to see other people\\'s prices.') }}</p>
          } @else if (publicPrices().length) {
            <div class="rows">
              @for (p of publicPrices().slice(0, 8); track $index) {
                <div class="row" style="justify-content:space-between">
                  <span>{{ p.supplierName }}<br><span class="faint" style="font-size:12px">{{ p.city ?? p.country }} · {{ date(p.observedAt, zone()) }}@if (p.storeListing) { · {{ t('precio de la cadena', 'store listing') }} }</span></span>
                  <span class="fig">{{ unitPrice(p.unitPrice, p.currency) }}<small>/{{ p.unitPriceUnit }}</small></span>
                </div>
              }
            </div>
            <p class="note" style="margin-top:10px">{{ t('Nadie ve quién compartió cada precio.', 'Nobody sees who shared each price.') }}</p>
          } @else {
            <p class="empty">{{ t('Nadie ha compartido precios de este producto en tu país todavía.', 'Nobody has shared prices for this item in your country yet.') }}</p>
          }
        </section>
        <div class="seg-wrap" style="grid-column: span 12">
          @if (confirmArchive()) {
            <span class="muted">{{ t('Deja de aparecer en listas y predicciones; su historia se guarda.', 'It leaves lists and forecasts; its history is kept.') }}</span>
            <button class="btn" type="button" (click)="confirmArchive.set(false)">{{ t('No', 'No') }}</button>
            <button class="btn" type="button" (click)="archive()">{{ t('Sí, archivar', 'Yes, archive') }}</button>
          } @else {
            <button class="card-link" type="button" (click)="confirmArchive.set(true)">{{ t('Archivar producto', 'Archive item') }}</button>
          }
        </div>
      </div>
      @if (moving()) { <app-movement-sheet [item]="i" [allUnits]="units.units()" (closed)="moving.set(false)" (saved)="after(moving)" /> }
      @if (pricing()) { <app-price-sheet [item]="i" [allUnits]="units.units()" (closed)="pricing.set(false)" (saved)="after(pricing)" /> }
      @if (editing()) { <app-item-form-sheet [item]="i" [allUnits]="units.units()" (closed)="editing.set(false)" (saved)="edited($event)" /> }
    } @else if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
      <a class="card-link" routerLink="/items">{{ t('Volver al inventario', 'Back to items') }}</a>
    } @else {
      <p class="loading">{{ t('Cargando…', 'Loading…') }}</p>
    }
  `
})
export default class ItemDetailComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private profile = inject(ProfileStore);
  readonly units = inject(UnitsStore);

  readonly id = input.required<string>();
  readonly t = t; readonly qty = qty; readonly money = money; readonly unitPrice = unitPrice; readonly date = date;
  readonly dateTime = dateTime; readonly fill = fill; readonly confidence = confidence; readonly movementLabel = movementLabel;

  readonly item = signal<Item | null>(null);
  readonly forecast = signal<Forecast | null>(null);
  readonly movements = signal<Movement[]>([]);
  readonly prices = signal<Price[]>([]);
  readonly publicPrices = signal<PublicPrice[]>([]);
  readonly error = signal<string | null>(null);
  readonly moving = signal(false);
  readonly pricing = signal(false);
  readonly editing = signal(false);
  readonly confirmArchive = signal(false);
  readonly zone = computed(() => this.profile.timeZone());
  readonly currency = computed(() => this.profile.currency());

  readonly finding = computed(() => {
    const f = this.forecast(), i = this.item();
    if (!f || !i) return '';
    if (f.daysLeft == null) return t('Aún no hay consumo registrado: anota lo que usas y te digo cuándo se acaba.', 'No consumption recorded yet: record what you use and you will see when it runs out.');
    const days = Math.floor(f.daysLeft);
    return t(`Te alcanza para ${days} días al ritmo de ${qty(f.dailyUse)} ${i.unit} diarios.`, `It lasts ${days} days at ${qty(f.dailyUse)} ${i.unit} a day.`)
      + (f.confidence === 'LOW' ? ' ' + t('Con pocos datos, tómalo como una idea.', 'With few records, take it as a rough idea.') : '');
  });

  readonly priceSpan = computed(() => {
    const ps = this.prices();
    if (!ps.length) return '';
    const v = ps.map((p) => p.unitPrice);
    const c = ps[0].currency;
    return `${unitPrice(Math.min(...v), c)} – ${unitPrice(Math.max(...v), c)}`;
  });

  async ngOnInit() {
    await this.units.load().catch(() => undefined);
    await this.reload();
  }

  async reload() {
    try {
      const id = this.id();
      const [item, forecast, movements, prices, publics] = await Promise.all([
        this.api.item(id), this.api.forecast(id), this.api.movements(id), this.api.prices(id), this.api.publicPrices(id)
      ]);
      this.item.set(item); this.forecast.set(forecast); this.movements.set(movements);
      this.prices.set(prices); this.publicPrices.set(publics);
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos cargar el producto.', 'We could not load the item.')));
    }
  }

  async after(sheet: { set(v: boolean): void }) {
    sheet.set(false);
    await this.reload();
  }

  async edited(item: Item) {
    this.editing.set(false);
    this.item.set(item);
    await this.reload();
  }

  async archive() {
    const i = this.item();
    if (!i) return;
    try {
      await this.api.archiveItem(i.id);
      await this.router.navigateByUrl('/items');
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos archivarlo.', 'We could not archive it.')));
    }
  }
}
