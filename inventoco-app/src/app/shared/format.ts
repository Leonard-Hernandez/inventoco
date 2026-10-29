import { locale, t } from '../core/i18n/i18n';

/** Money in a currency, following the language's separators: $12.900 (es) / $12,900 (en). */
export function money(amount: number | null | undefined, currency: string, digits?: number): string {
  if (amount == null) return '—';
  const fraction = digits ?? (Math.abs(amount) >= 1000 || Number.isInteger(amount) ? 0 : 2);
  return new Intl.NumberFormat(locale(), {
    style: 'currency', currency, currencyDisplay: 'narrowSymbol',
    minimumFractionDigits: fraction, maximumFractionDigits: fraction
  }).format(amount);
}

/** A price per unit: small values keep enough decimals to compare (COP per gram, USD per ml). */
export function unitPrice(amount: number | null | undefined, currency: string): string {
  if (amount == null) return '—';
  const digits = Math.abs(amount) >= 100 ? 0 : Math.abs(amount) >= 1 ? 2 : 4;
  return money(amount, currency, digits);
}

export function qty(value: number | null | undefined, maxDigits = 3): string {
  if (value == null) return '—';
  return new Intl.NumberFormat(locale(), { maximumFractionDigits: maxDigits }).format(value);
}

/** An instant shown in the user's time zone. */
export function dateTime(iso: string | null | undefined, timeZone: string): string {
  if (!iso) return '—';
  return new Intl.DateTimeFormat(locale(), { timeZone, day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }).format(new Date(iso));
}

export function date(iso: string | null | undefined, timeZone: string, withYear = false): string {
  if (!iso) return '—';
  return new Intl.DateTimeFormat(locale(), { timeZone, day: 'numeric', month: 'short', ...(withYear ? { year: 'numeric' } : {}) }).format(new Date(iso));
}

/** "today", "tomorrow", "in 4 days" for a depletion date. */
export function runsOut(daysLeft: number | null | undefined): string {
  if (daysLeft == null) return t('Sin consumo registrado', 'No consumption recorded');
  if (daysLeft < 1) return t('Se acaba hoy', 'Runs out today');
  if (daysLeft < 2) return t('Se acaba mañana', 'Runs out tomorrow');
  const d = Math.floor(daysLeft);
  return t(`Se acaba en ${d} días`, `Runs out in ${d} days`);
}

export function confidence(c: string): string {
  switch (c) {
    case 'HIGH': return t('Confianza alta', 'High confidence');
    case 'MEDIUM': return t('Confianza media', 'Medium confidence');
    default: return t('Pocos datos aún', 'Few records yet');
  }
}

export function movementLabel(type: string): string {
  switch (type) {
    case 'PURCHASE': return t('Compra', 'Purchase');
    case 'CONSUMPTION': return t('Consumo', 'Used');
    case 'WASTE': return t('Desperdicio', 'Wasted');
    default: return t('Conteo', 'Count');
  }
}

/** Width of a stock bar: the shelf is full at twice the minimum (or at the stock itself with no minimum). */
export function fill(stock: number, minStock: number): { width: number; min: number | null } {
  const full = minStock > 0 ? minStock * 2 : Math.max(stock, 1);
  return { width: Math.min(100, (stock / full) * 100), min: minStock > 0 ? 50 : null };
}

/** The current instant as an ISO string for a datetime-local default, in the user's zone. */
export function localInput(timeZone: string, at = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
    .formatToParts(at).reduce<Record<string, string>>((acc, p) => ({ ...acc, [p.type]: p.value }), {});
  return `${parts['year']}-${parts['month']}-${parts['day']}T${parts['hour']}:${parts['minute']}`;
}

/** A datetime-local value typed in the user's zone, as an instant. */
export function toInstant(local: string, timeZone: string): string | null {
  if (!local) return null;
  const asUtc = new Date(`${local}:00Z`);
  // offset of the zone at that moment: format the UTC guess in the zone and compare
  const shown = new Date(new Intl.DateTimeFormat('en-US', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' })
    .format(asUtc).replace(/(\d+)\/(\d+)\/(\d+), (\d+):(\d+):(\d+)/, '$3-$1-$2T$4:$5:$6Z'));
  return new Date(asUtc.getTime() - (shown.getTime() - asUtc.getTime())).toISOString();
}
