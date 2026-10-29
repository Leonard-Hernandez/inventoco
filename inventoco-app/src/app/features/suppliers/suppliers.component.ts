import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Supplier, SupplierRank } from '../../core/api/models';
import { t } from '../../core/i18n/i18n';

/** Where the user buys, and which of those places is cheapest for what they buy. */
@Component({
  selector: 'app-suppliers',
  imports: [FormsModule],
  template: `
    <div class="title-row">
      <div>
        <h1>{{ t('Proveedores', 'Suppliers') }}</h1>
        <p class="sub">{{ finding() }}</p>
      </div>
      <button class="btn primary" type="button" (click)="edit(null)">{{ t('Agregar proveedor', 'Add a supplier') }}</button>
    </div>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    <div class="grid top">
      <section class="panel" style="grid-column: span 7" aria-labelledby="h-rank">
        <header><h2 id="h-rank">{{ t('Quién te vende más barato', 'Who sells to you cheapest') }}</h2><span class="meta">{{ t('180 días', '180 days') }}</span></header>
        @if (ranking().length) {
          <div class="table-wrap"><table>
            <thead><tr><th>{{ t('Proveedor', 'Supplier') }}</th><th class="n">{{ t('Frente al precio típico', 'Against the typical price') }}</th><th class="n">{{ t('Más barato en', 'Cheapest for') }}</th></tr></thead>
            <tbody>
              @for (r of ranking(); track r.supplierId) {
                <tr><td>{{ r.name }}<span class="sub">{{ r.prices }} {{ t('precios', 'prices') }}</span></td>
                  <td class="n"><span class="fig">{{ index(r.priceIndex) }}</span></td>
                  <td class="n"><span class="fig">{{ r.itemsCheapest }}</span> {{ t('productos', 'items') }}</td></tr>
              }
            </tbody>
          </table></div>
          <p class="note" style="margin-top:10px">{{ t('Cada precio se compara con el precio típico (la mediana) de ese producto.', 'Each price is compared with the typical price (the median) of that item.') }}</p>
        } @else {
          <p class="empty">{{ t('Anota precios del mismo producto en dos proveedores para compararlos.', 'Record prices of the same item at two suppliers to compare them.') }}</p>
        }
      </section>
      <section class="panel" style="grid-column: span 5" aria-labelledby="h-list">
        <header><h2 id="h-list">{{ t('Tus proveedores', 'Your suppliers') }}</h2></header>
        @if (own().length) {
          <div class="rows">
            @for (s of own(); track s.id) {
              <div class="row" style="justify-content:space-between">
                <span>{{ s.name }}@if (s.city) {<br><span class="faint" style="font-size:12px">{{ s.city }}</span>}</span>
                <button class="btn ghost" type="button" (click)="edit(s)">{{ t('Editar', 'Edit') }}</button>
              </div>
            }
          </div>
        } @else {
          <p class="empty">{{ t('Aún no tienes proveedores. También se crean al anotar un precio.', 'No suppliers yet. They are also created when you record a price.') }}</p>
        }
        @if (chains().length) {
          <h3 class="muted" style="font-size:13px;font-weight:500;margin:18px 0 4px">{{ t('Cadenas de tu país', 'Store chains in your country') }}</h3>
          <div class="rows">
            @for (s of chains(); track s.id) { <div class="row">{{ s.name }}</div> }
          </div>
        }
      </section>
    </div>
    @if (editing(); as e) {
      <div class="sheet-backdrop" (click)="editing.set(null)"></div>
      <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="sp-title" (keydown.escape)="editing.set(null)">
        <h2 id="sp-title">{{ e.id ? t('Editar proveedor', 'Edit supplier') : t('Nuevo proveedor', 'New supplier') }}</h2>
        <p class="sub">{{ t('Una tienda, un mercado o un distribuidor.', 'A shop, a market or a distributor.') }}</p>
        <form class="form" (ngSubmit)="save()">
          <div class="field"><label for="sp-name">{{ t('Nombre', 'Name') }}</label><input id="sp-name" class="input" required maxlength="160" [(ngModel)]="form.name" name="name" autofocus></div>
          <div class="field"><label for="sp-city">{{ t('Ciudad (opcional)', 'City (optional)') }}</label><input id="sp-city" class="input" maxlength="120" [(ngModel)]="form.city" name="city"></div>
          <div class="field"><label for="sp-url">{{ t('Sitio web (opcional)', 'Website (optional)') }}</label><input id="sp-url" class="input" type="url" maxlength="500" [(ngModel)]="form.url" name="url"></div>
          <div class="actions">
            @if (e.id) { <button class="btn ghost" type="button" style="margin-right:auto" (click)="archive(e.id)">{{ t('Archivar', 'Archive') }}</button> }
            <button class="btn" type="button" (click)="editing.set(null)">{{ t('Cancelar', 'Cancel') }}</button>
            <button class="btn primary" type="submit" [disabled]="busy() || !form.name.trim()">{{ t('Guardar', 'Save') }}</button>
          </div>
        </form>
      </section>
    }
  `
})
export default class SuppliersComponent implements OnInit {
  private api = inject(ApiService);

  readonly t = t;
  readonly suppliers = signal<Supplier[]>([]);
  readonly ranking = signal<SupplierRank[]>([]);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly editing = signal<{ id: string | null } | null>(null);
  readonly own = computed(() => this.suppliers().filter((s) => !s.storeChain));
  readonly chains = computed(() => this.suppliers().filter((s) => s.storeChain));
  readonly finding = computed(() => {
    const best = this.ranking()[0];
    if (!best || this.ranking().length < 2) return t('Compara proveedores anotando lo que pagas en cada uno.', 'Compare suppliers by recording what you pay at each one.');
    return t(`${best.name} es el más barato: ${this.index(best.priceIndex)} frente al precio típico.`, `${best.name} is the cheapest: ${this.index(best.priceIndex)} against the typical price.`);
  });
  form = { name: '', city: '', url: '' };

  async ngOnInit() {
    await this.reload();
  }

  async reload() {
    try {
      const [s, r] = await Promise.all([this.api.suppliers(), this.api.supplierRanking(180)]);
      this.suppliers.set(s);
      this.ranking.set(r);
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos cargar los proveedores.', 'We could not load the suppliers.')));
    }
  }

  edit(s: Supplier | null) {
    this.form = { name: s?.name ?? '', city: s?.city ?? '', url: s?.url ?? '' };
    this.editing.set({ id: s?.id ?? null });
  }

  async save() {
    const id = this.editing()?.id;
    this.busy.set(true);
    try {
      const body = { name: this.form.name.trim(), city: this.form.city || null, url: this.form.url || null };
      if (id) await this.api.updateSupplier(id, body); else await this.api.createSupplier(body);
      this.editing.set(null);
      await this.reload();
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos guardar el proveedor.', 'We could not save the supplier.')));
    } finally {
      this.busy.set(false);
    }
  }

  async archive(id: string) {
    try {
      await this.api.archiveSupplier(id);
      this.editing.set(null);
      await this.reload();
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos archivarlo.', 'We could not archive it.')));
    }
  }

  index(i: number): string {
    const pct = Math.round((1 - i) * 100);
    if (pct === 0) return t('igual', 'the same');
    return pct > 0 ? t(`${pct} % más barato`, `${pct}% cheaper`) : t(`${-pct} % más caro`, `${-pct}% dearer`);
  }
}
