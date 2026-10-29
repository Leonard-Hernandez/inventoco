import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { appConfig } from '../config/app-config';
import {
  BoughtLine, CatalogProduct, Dashboard, Forecast, Item, ItemRequest, Movement, MovementType, Price, Profile,
  PublicPrice, Purchase, Savings, Supplier, SupplierRank, UnitInfo, Visibility
} from './models';

/** Thin typed client of inventoco-api. Every call returns a promise; errors keep the API's code and message. */
@Injectable({ providedIn: 'root' })
export class ApiService {

  private http = inject(HttpClient);

  private url(path: string) {
    return `${appConfig().apiUrl}${path}`;
  }

  private run<T>(o: Observable<T>): Promise<T> {
    return firstValueFrom(o);
  }

  me() { return this.run(this.http.get<Profile>(this.url('/api/me'))); }
  updateMe(body: { country: string; currency?: string | null; timeZone: string; sharePricesByDefault: boolean }) {
    return this.run(this.http.put<Profile>(this.url('/api/me'), body));
  }
  units() { return this.run(this.http.get<UnitInfo[]>(this.url('/api/units'))); }

  items(q?: string, lowStock = false) {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (lowStock) params = params.set('lowStock', 'true');
    return this.run(this.http.get<Item[]>(this.url('/api/items'), { params }));
  }
  item(id: string) { return this.run(this.http.get<Item>(this.url(`/api/items/${id}`))); }
  createItem(body: ItemRequest) { return this.run(this.http.post<Item>(this.url('/api/items'), body)); }
  updateItem(id: string, body: ItemRequest) { return this.run(this.http.put<Item>(this.url(`/api/items/${id}`), body)); }
  archiveItem(id: string) { return this.run(this.http.delete<void>(this.url(`/api/items/${id}`))); }

  movements(itemId: string) { return this.run(this.http.get<Movement[]>(this.url(`/api/items/${itemId}/movements`))); }
  move(itemId: string, body: { type: MovementType; quantity: number; unit?: string | null; occurredAt?: string | null; note?: string | null }) {
    return this.run(this.http.post<Movement>(this.url(`/api/items/${itemId}/movements`), body));
  }

  prices(itemId: string) { return this.run(this.http.get<Price[]>(this.url(`/api/items/${itemId}/prices`))); }
  addPrice(itemId: string, body: { supplierId: string; amount: number; packageQuantity: number; packageUnit?: string | null; observedAt?: string | null; visibility?: Visibility | null }) {
    return this.run(this.http.post<Price>(this.url(`/api/items/${itemId}/prices`), body));
  }
  publicPrices(itemId: string) { return this.run(this.http.get<PublicPrice[]>(this.url(`/api/items/${itemId}/public-prices`))); }
  forecast(itemId: string) { return this.run(this.http.get<Forecast>(this.url(`/api/items/${itemId}/forecast`))); }

  catalog(q: string) { return this.run(this.http.get<CatalogProduct[]>(this.url('/api/catalog'), { params: { q } })); }

  suppliers() { return this.run(this.http.get<Supplier[]>(this.url('/api/suppliers'))); }
  createSupplier(body: { name: string; city?: string | null; url?: string | null }) {
    return this.run(this.http.post<Supplier>(this.url('/api/suppliers'), body));
  }
  updateSupplier(id: string, body: { name: string; city?: string | null; url?: string | null }) {
    return this.run(this.http.put<Supplier>(this.url(`/api/suppliers/${id}`), body));
  }
  archiveSupplier(id: string) { return this.run(this.http.delete<void>(this.url(`/api/suppliers/${id}`))); }

  async draft(): Promise<Purchase | null> {
    const r = await this.run(this.http.get<Purchase>(this.url('/api/purchases/draft'), { observe: 'response' }));
    return r.status === 204 ? null : r.body;
  }
  generate(horizonDays: number) {
    return this.run(this.http.post<Purchase>(this.url('/api/purchases/generate'), null, { params: { horizonDays } }));
  }
  complete(id: string, lines: BoughtLine[], occurredAt?: string | null) {
    return this.run(this.http.post<Purchase>(this.url(`/api/purchases/${id}/complete`), { lines, occurredAt }));
  }
  recordPurchase(lines: BoughtLine[], occurredAt?: string | null) {
    return this.run(this.http.post<Purchase>(this.url('/api/purchases'), { lines, occurredAt }));
  }
  purchases() { return this.run(this.http.get<Purchase[]>(this.url('/api/purchases'))); }

  dashboard() { return this.run(this.http.get<Dashboard>(this.url('/api/insights/dashboard'))); }
  forecasts(horizonDays = 7) { return this.run(this.http.get<Forecast[]>(this.url('/api/insights/forecasts'), { params: { horizonDays } })); }
  savings(days: number) { return this.run(this.http.get<Savings>(this.url('/api/insights/savings'), { params: { days } })); }
  supplierRanking(days: number) { return this.run(this.http.get<SupplierRank[]>(this.url('/api/insights/suppliers'), { params: { days } })); }
}

/** The message to show for a failed call: the API's own (it names the problem), or a generic one. */
export function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error.message === 'string') {
    return error.error.message;
  }
  return fallback;
}
