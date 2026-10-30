import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../core/api/api.service';
import { CatalogProduct, Dimension, Item, UnitInfo } from '../core/api/models';
import { t } from '../core/i18n/i18n';

/** Fabric and flooring are counted in meters and square meters, not in their base mm and cm². */
const HUMAN_UNITS: Partial<Record<Dimension, string>> = { LENGTH: 'm', AREA: 'm2' };

/**
 * Create or edit an item. The unit chosen at creation fixes how it is measured (count, weight,
 * volume, length or area); linking it to a shared catalog product is what lets the user see and share public prices.
 */
@Component({
  selector: 'app-item-form-sheet',
  imports: [FormsModule],
  template: `
    <div class="sheet-backdrop" (click)="closed.emit()"></div>
    <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="it-title" (keydown.escape)="closed.emit()">
      <h2 id="it-title">{{ item() ? t('Editar producto', 'Edit item') : t('Nuevo producto', 'New item') }}</h2>
      <p class="sub">{{ t('Cualquier cosa que guardes: por unidades, peso, volumen, metros o metros cuadrados.', 'Anything you keep: by units, weight, volume, meters or square meters.') }}</p>
      <form class="form" (ngSubmit)="save()">
        <div class="field">
          <label for="it-name">{{ t('Nombre', 'Name') }}</label>
          <input id="it-name" class="input" required maxlength="160" [(ngModel)]="name" name="name" (ngModelChange)="searchCatalog($event)" autofocus>
        </div>
        @if (matches().length && !catalogId) {
          <div class="field">
            <span class="hint">{{ t('¿Es alguno de estos? Así ves los precios que otros comparten.', 'Is it one of these? Then you see the prices others share.') }}</span>
            <div class="seg-wrap" style="flex-wrap:wrap">
              @for (m of matches(); track m.id) {
                <button class="btn" type="button" (click)="pick(m)">{{ m.name }}@if (m.brand) { · {{ m.brand }} }</button>
              }
            </div>
          </div>
        }
        @if (catalogId) {
          <p class="note">{{ t('Vinculado al catálogo compartido.', 'Linked to the shared catalog.') }} <button class="card-link" type="button" (click)="catalogId = null">{{ t('Quitar', 'Remove') }}</button></p>
        }
        <div class="cols">
          <div class="field">
            <label for="it-unit">{{ t('Se mide en', 'Measured in') }}</label>
            <select id="it-unit" class="input" required [(ngModel)]="unit" name="unit">
              @for (g of groups(); track g.dimension) {
                <optgroup [label]="g.label">
                  @for (u of g.units; track u.symbol) { <option [value]="u.symbol">{{ u.symbol }}</option> }
                </optgroup>
              }
            </select>
          </div>
          <div class="field">
            <label for="it-min">{{ t('Mínimo', 'Minimum') }}</label>
            <input id="it-min" class="input fig" type="number" inputmode="decimal" min="0" step="any" [(ngModel)]="minStock" name="minStock">
          </div>
          @if (!item()) {
            <div class="field">
              <label for="it-stock">{{ t('Hay ahora', 'In stock now') }}</label>
              <input id="it-stock" class="input fig" type="number" inputmode="decimal" min="0" step="any" [(ngModel)]="initialStock" name="initialStock">
            </div>
          }
        </div>
        <div class="cols">
          <div class="field">
            <label for="it-cat">{{ t('Categoría (opcional)', 'Category (optional)') }}</label>
            <input id="it-cat" class="input" maxlength="80" [(ngModel)]="category" name="category">
          </div>
          <div class="field">
            <label for="it-bar">{{ t('Código de barras (opcional)', 'Barcode (optional)') }}</label>
            <input id="it-bar" class="input fig" inputmode="numeric" maxlength="32" [(ngModel)]="barcode" name="barcode">
          </div>
        </div>
        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
        <div class="actions">
          <button class="btn" type="button" (click)="closed.emit()">{{ t('Cancelar', 'Cancel') }}</button>
          <button class="btn primary" type="submit" [disabled]="busy() || !name.trim() || !unit">{{ item() ? t('Guardar', 'Save') : t('Agregar', 'Add') }}</button>
        </div>
      </form>
    </section>
  `
})
export class ItemFormSheetComponent implements OnInit {
  private api = inject(ApiService);

  readonly item = input<Item | null>(null);
  readonly allUnits = input<UnitInfo[]>([]);
  readonly closed = output<void>();
  readonly saved = output<Item>();

  readonly t = t;
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly matches = signal<CatalogProduct[]>([]);
  readonly groups = computed(() => {
    const fixed = this.item()?.dimension;
    const labels: Record<Dimension, string> = {
      COUNT: t('Unidades', 'Count'), MASS: t('Peso', 'Weight'), VOLUME: t('Volumen', 'Volume'),
      LENGTH: t('Longitud', 'Length'), AREA: t('Área', 'Area')
    };
    return (Object.keys(labels) as Dimension[])
      .filter((d) => !fixed || d === fixed)
      .map((d) => ({ dimension: d, label: labels[d], units: this.allUnits().filter((u) => u.dimension === d) }));
  });

  name = '';
  unit = 'unit';
  minStock: number | null = null;
  initialStock: number | null = null;
  category = '';
  barcode = '';
  catalogId: string | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit() {
    const i = this.item();
    if (i) {
      this.name = i.name; this.unit = i.unit; this.minStock = i.minStock; this.category = i.category ?? '';
      this.barcode = i.barcode ?? ''; this.catalogId = i.catalogProductId;
    }
  }

  searchCatalog(text: string) {
    if (this.timer) clearTimeout(this.timer);
    if (text.trim().length < 3) { this.matches.set([]); return; }
    this.timer = setTimeout(async () => {
      try { this.matches.set((await this.api.catalog(text.trim())).slice(0, 5)); } catch { this.matches.set([]); }
    }, 300);
  }

  pick(m: CatalogProduct) {
    this.catalogId = m.id;
    if (this.allUnits().find((u) => u.symbol === this.unit)?.dimension === m.dimension) return;
    const units = this.allUnits().filter((u) => u.dimension === m.dimension);
    const unit = units.find((u) => u.symbol === HUMAN_UNITS[m.dimension]) ?? units.find((u) => u.base);
    if (unit) this.unit = unit.symbol;
  }

  async save() {
    this.busy.set(true);
    this.error.set(null);
    const body = {
      name: this.name.trim(), unit: this.unit, minStock: this.minStock, initialStock: this.initialStock,
      category: this.category || null, barcode: this.barcode || null, catalogProductId: this.catalogId
    };
    try {
      const i = this.item();
      this.saved.emit(i ? await this.api.updateItem(i.id, body) : await this.api.createItem(body));
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos guardar el producto.', 'We could not save the item.')));
    } finally {
      this.busy.set(false);
    }
  }
}
