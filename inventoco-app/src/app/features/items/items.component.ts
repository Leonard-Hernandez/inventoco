import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Item } from '../../core/api/models';
import { t } from '../../core/i18n/i18n';
import { fill, qty } from '../../shared/format';
import { ItemFormSheetComponent } from '../../shared/item-form-sheet.component';
import { MovementSheetComponent } from '../../shared/movement-sheet.component';
import { UnitsStore } from '../../shared/units.store';

/** Everything the user keeps, with how full each shelf is and a quick "used / bought". */
@Component({
  selector: 'app-items',
  imports: [FormsModule, RouterLink, ItemFormSheetComponent, MovementSheetComponent],
  template: `
    <div class="title-row">
      <div>
        <h1>{{ t('Inventario', 'Items') }}</h1>
        <p class="sub">{{ finding() }}</p>
      </div>
      <button class="btn primary" type="button" (click)="creating.set(true)">{{ t('Agregar producto', 'Add an item') }}</button>
    </div>
    <div class="toolbar">
      <input class="input" type="search" [placeholder]="t('Buscar por nombre, categoría o código', 'Search by name, category or barcode')"
             [attr.aria-label]="t('Buscar', 'Search')" [(ngModel)]="query" (ngModelChange)="onQuery()">
      <div class="segmented" role="group" [attr.aria-label]="t('Mostrar', 'Show')">
        <button type="button" [attr.aria-pressed]="!lowOnly()" (click)="lowOnly.set(false)">{{ t('Todos', 'All') }}</button>
        <button type="button" [attr.aria-pressed]="lowOnly()" (click)="lowOnly.set(true)">{{ t('Bajo el mínimo', 'Under minimum') }}</button>
      </div>
    </div>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    <section class="panel">
      @if (loaded()) {
        @if (shown().length) {
          <div class="rows">
            @for (i of shown(); track i.id) {
              <div class="row line">
                <a class="item-row" [routerLink]="['/items', i.id]">
                  <span><span class="name">{{ i.name }}</span><br><span class="meta">{{ i.category ?? t('Sin categoría', 'No category') }}@if (i.lowStock) { · <span class="pill warn">{{ t('Bajo el mínimo', 'Under minimum') }}</span> }</span></span>
                  <span class="fig">{{ qty(i.stock) }}<small>{{ i.unit }}</small></span>
                  <span class="stockbar" role="img" [attr.aria-label]="t('Mínimo ', 'Minimum ') + qty(i.minStock) + ' ' + i.unit">
                    <i [class.low]="i.lowStock" [style.width.%]="fill(i.stock, i.minStock).width"></i>
                    @if (fill(i.stock, i.minStock).min; as m) { <b [style.left.%]="m"></b> }
                  </span>
                </a>
                <button class="btn" type="button" (click)="moving.set(i)">{{ t('Registrar', 'Record') }}</button>
              </div>
            }
          </div>
        } @else if (all().length) {
          <p class="empty">{{ t('Nada coincide con la búsqueda.', 'Nothing matches the search.') }}</p>
        } @else {
          <p class="empty">{{ t('Agrega lo primero que quieras controlar: arroz, detergente, café…', 'Add the first thing you want to keep track of: rice, detergent, coffee…') }}</p>
        }
      } @else {
        <p class="loading">{{ t('Cargando…', 'Loading…') }}</p>
      }
    </section>
    @if (creating()) {
      <app-item-form-sheet [allUnits]="units.units()" (closed)="creating.set(false)" (saved)="created($event)" />
    }
    @if (moving(); as m) {
      <app-movement-sheet [item]="m" [allUnits]="units.units()" (closed)="moving.set(null)" (saved)="moved()" />
    }
  `,
  styles: `
    .line { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 16px; }
  `
})
export default class ItemsComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  readonly units = inject(UnitsStore);

  readonly t = t; readonly qty = qty; readonly fill = fill;
  readonly all = signal<Item[]>([]);
  readonly loaded = signal(false);
  readonly error = signal<string | null>(null);
  readonly lowOnly = signal(false);
  readonly creating = signal(false);
  readonly moving = signal<Item | null>(null);
  readonly search = signal('');
  query = '';

  readonly shown = computed(() => {
    const q = this.search().toLowerCase();
    return this.all().filter((i) => (!this.lowOnly() || i.lowStock)
      && (!q || i.name.toLowerCase().includes(q) || (i.category ?? '').toLowerCase().includes(q) || i.barcode === q));
  });
  readonly finding = computed(() => {
    const items = this.all();
    if (!this.loaded()) return '';
    if (!items.length) return t('Aún no tienes productos.', 'You have no items yet.');
    const low = items.filter((i) => i.lowStock).length;
    return low
      ? t(`${low} de ${items.length} productos están en el mínimo o por debajo.`, `${low} of ${items.length} items are at or under their minimum.`)
      : t(`Los ${items.length} productos están sobre el mínimo.`, `All ${items.length} items are above their minimum.`);
  });

  async ngOnInit() {
    if (this.route.snapshot.queryParamMap.get('new')) this.creating.set(true);
    await Promise.all([this.units.load(), this.reload()]);
  }

  onQuery() {
    this.search.set(this.query.trim());
  }

  async reload() {
    try {
      this.all.set(await this.api.items());
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos cargar el inventario.', 'We could not load the items.')));
    } finally {
      this.loaded.set(true);
    }
  }

  created(item: Item) {
    this.creating.set(false);
    this.router.navigate(['/items', item.id]);
  }

  async moved() {
    this.moving.set(null);
    await this.reload();
  }
}
