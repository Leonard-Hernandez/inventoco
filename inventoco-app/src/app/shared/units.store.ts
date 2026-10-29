import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from '../core/api/api.service';
import { UnitInfo } from '../core/api/models';

/** The units the API accepts, loaded once. */
@Injectable({ providedIn: 'root' })
export class UnitsStore {
  private api = inject(ApiService);
  readonly units = signal<UnitInfo[]>([]);

  async load() {
    if (!this.units().length) this.units.set(await this.api.units());
    return this.units();
  }
}
