import { Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../core/api/api.service';
import { Item, Supplier, UnitInfo } from '../core/api/models';
import { ProfileStore } from '../core/api/profile.store';
import { t } from '../core/i18n/i18n';
import { localInput, toInstant } from './format';

/** A price seen or paid: the package price at a supplier, optionally shared (anonymously) with everyone. */
@Component({
  selector: 'app-price-sheet',
  imports: [FormsModule],
  template: `
    <div class="sheet-backdrop" (click)="closed.emit()"></div>
    <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="pr-title" (keydown.escape)="closed.emit()">
      <h2 id="pr-title">{{ t('Anotar precio', 'Record a price') }}</h2>
      <p class="sub">{{ item().name }} · {{ t('el precio del paquete, en', 'the package price, in') }} {{ currency() }}</p>
      <form class="form" (ngSubmit)="save()">
        <div class="field">
          <label for="pr-sup">{{ t('Dónde', 'Where') }}</label>
          <select id="pr-sup" class="input" [(ngModel)]="supplierId" name="supplierId">
            @for (s of suppliers(); track s.id) { <option [value]="s.id">{{ s.name }}@if (s.city) { · {{ s.city }} }</option> }
            <option value="">{{ t('Otro proveedor…', 'Another supplier…') }}</option>
          </select>
        </div>
        @if (!supplierId) {
          <div class="field">
            <label for="pr-new">{{ t('Nombre del proveedor', 'Supplier name') }}</label>
            <input id="pr-new" class="input" maxlength="160" [(ngModel)]="newSupplier" name="newSupplier">
          </div>
        }
        <div class="cols">
          <div class="field">
            <label for="pr-amt">{{ t('Precio', 'Price') }}</label>
            <input id="pr-amt" class="input fig" type="number" inputmode="decimal" min="0" step="any" required [(ngModel)]="amount" name="amount">
          </div>
          <div class="field">
            <label for="pr-q">{{ t('Tamaño del paquete', 'Package size') }}</label>
            <input id="pr-q" class="input fig" type="number" inputmode="decimal" min="0" step="any" required [(ngModel)]="packageQuantity" name="packageQuantity">
          </div>
          <div class="field">
            <label for="pr-u">{{ t('Unidad', 'Unit') }}</label>
            <select id="pr-u" class="input" [(ngModel)]="packageUnit" name="packageUnit">
              @for (u of units(); track u.symbol) { <option [value]="u.symbol">{{ u.symbol }}</option> }
            </select>
          </div>
        </div>
        <div class="field">
          <label for="pr-at">{{ t('Cuándo', 'When') }}</label>
          <input id="pr-at" class="input" type="datetime-local" [(ngModel)]="at" name="at">
        </div>
        @if (item().catalogProductId) {
          <label class="check"><input class="switch" type="checkbox" [(ngModel)]="share" name="share"> {{ t('Compartir este precio, sin tu nombre, para que otros compren mejor', 'Share this price, without your name, so others buy better') }}</label>
        } @else {
          <p class="note">{{ t('Este producto no está en el catálogo compartido: el precio queda solo para ti.', 'This item is not in the shared catalog: the price stays private.') }}</p>
        }
        @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
        <div class="actions">
          <button class="btn" type="button" (click)="closed.emit()">{{ t('Cancelar', 'Cancel') }}</button>
          <button class="btn primary" type="submit" [disabled]="busy() || amount == null || !packageQuantity || (!supplierId && !newSupplier.trim())">{{ t('Guardar', 'Save') }}</button>
        </div>
      </form>
    </section>
  `
})
export class PriceSheetComponent implements OnInit {
  private api = inject(ApiService);
  private profile = inject(ProfileStore);

  readonly item = input.required<Item>();
  readonly allUnits = input<UnitInfo[]>([]);
  readonly closed = output<void>();
  readonly saved = output<void>();

  readonly t = t;
  readonly suppliers = signal<Supplier[]>([]);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly units = computed(() => this.allUnits().filter((u) => u.dimension === this.item().dimension));
  readonly currency = computed(() => this.profile.currency());

  supplierId = '';
  newSupplier = '';
  amount: number | null = null;
  packageQuantity: number | null = 1;
  packageUnit = '';
  at = '';
  share = false;

  async ngOnInit() {
    this.packageUnit = this.item().unit;
    this.at = localInput(this.profile.timeZone());
    this.share = this.profile.profile()?.sharePricesByDefault ?? false;
    try {
      this.suppliers.set(await this.api.suppliers());
      this.supplierId = this.suppliers()[0]?.id ?? '';
    } catch { /* the user can still type a new one */ }
  }

  async save() {
    if (this.amount == null || !this.packageQuantity) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      const supplierId = this.supplierId || (await this.api.createSupplier({ name: this.newSupplier.trim() })).id;
      this.supplierId = supplierId;
      await this.api.addPrice(this.item().id, {
        supplierId, amount: this.amount, packageQuantity: this.packageQuantity, packageUnit: this.packageUnit,
        observedAt: toInstant(this.at, this.profile.timeZone()),
        visibility: this.item().catalogProductId && this.share ? 'PUBLIC' : 'PRIVATE'
      });
      this.saved.emit();
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos guardar el precio.', 'We could not save the price.')));
    } finally {
      this.busy.set(false);
    }
  }
}
