import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { ProfileStore } from '../../core/api/profile.store';
import { appConfig } from '../../core/config/app-config';
import { lang, t } from '../../core/i18n/i18n';

/** ISO-3166 alpha-2 codes; names come from the browser in the user's language. */
const COUNTRIES = ('AD AE AF AG AI AL AM AO AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BW BY BZ '
  + 'CA CD CF CG CH CI CK CL CM CN CO CR CU CV CW CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE '
  + 'GF GG GH GI GL GM GN GP GQ GR GT GU GW GY HK HN HR HT HU ID IE IL IM IN IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR '
  + 'KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ NA NC NE '
  + 'NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PR PS PT PW PY QA RE RO RS RU RW SA SB SC SD SE SG SH SI SK SL SM '
  + 'SN SO SR SS ST SV SX SY SZ TC TD TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG US UY UZ VA VC VE VG VI VN VU WF WS YE YT '
  + 'ZA ZM ZW').split(' ');

/** Country, currency and time zone (asked on the first visit) and how to connect Claude over MCP. */
@Component({
  selector: 'app-settings',
  imports: [FormsModule],
  template: `
    <div class="title-row">
      <div>
        <h1>{{ welcome() ? t('Bienvenido a Inventoco', 'Welcome to Inventoco') : t('Ajustes', 'Settings') }}</h1>
        <p class="sub">{{ welcome()
          ? t('Antes de empezar, confirma dónde estás: así mostramos tus precios en tu moneda y tus fechas en tu hora.', 'Before you start, confirm where you are: prices show in your currency and dates in your time.')
          : t('Tu país define la moneda y con qué precios públicos te comparas.', 'Your country sets the currency and which public prices you compare with.') }}</p>
      </div>
    </div>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @if (saved()) { <p class="toast" role="status">{{ t('Ajustes guardados.', 'Settings saved.') }}</p> }
    <div class="grid top">
      <section class="panel" style="grid-column: span 7" aria-labelledby="h-where">
        <header><h2 id="h-where">{{ t('Dónde estás', 'Where you are') }}</h2></header>
        <form class="form" (ngSubmit)="save()">
          <div class="cols">
            <div class="field">
              <label for="st-country">{{ t('País', 'Country') }}</label>
              <select id="st-country" class="input" [(ngModel)]="country" name="country" (ngModelChange)="currency = ''">
                @for (c of countries(); track c.code) { <option [value]="c.code">{{ c.name }}</option> }
              </select>
            </div>
            <div class="field">
              <label for="st-cur">{{ t('Moneda', 'Currency') }}</label>
              <select id="st-cur" class="input" [(ngModel)]="currency" name="currency">
                <option value="">{{ t('La del país', "The country's") }}</option>
                @for (c of currencies; track c) { <option [value]="c">{{ c }}</option> }
              </select>
            </div>
          </div>
          <div class="field">
            <label for="st-tz">{{ t('Zona horaria', 'Time zone') }}</label>
            <select id="st-tz" class="input" [(ngModel)]="timeZone" name="timeZone">
              @for (z of zones; track z) { <option [value]="z">{{ z }}</option> }
            </select>
            <span class="hint">{{ t('Las fechas se guardan como instantes y se muestran en esta zona.', 'Dates are stored as instants and shown in this zone.') }}</span>
          </div>
          <label class="check"><input class="switch" type="checkbox" [(ngModel)]="share" name="share"> {{ t('Compartir mis precios por defecto, sin mi nombre', 'Share my prices by default, without my name') }}</label>
          <span class="hint">{{ t('Se comparten el precio, el proveedor, su ciudad y la fecha; puedes cambiarlo en cada precio.', 'The price, supplier, its city and date are shared; you can change it on each price.') }}</span>
          <div class="actions">
            <button class="btn primary" type="submit" [disabled]="busy() || !country || !timeZone">{{ welcome() ? t('Empezar', 'Start') : t('Guardar', 'Save') }}</button>
          </div>
        </form>
      </section>
      <section class="panel" id="assistant" style="grid-column: span 5" aria-labelledby="h-ai">
        <header><h2 id="h-ai">{{ t('Pregúntale a Claude', 'Ask Claude') }}</h2></header>
        <p class="muted">{{ t('Conecta Claude a tu inventario y pídele en tus palabras: "usé medio kilo de arroz", "¿qué me falta para la semana?", "¿dónde está más barato el café?".',
          'Connect Claude to your inventory and ask in your own words: "I used half a kilo of rice", "what am I missing for the week?", "where is coffee cheapest?".') }}</p>
        <ol class="steps">
          <li>{{ t('En Claude, abre Ajustes → Conectores → Agregar conector personalizado.', 'In Claude, open Settings → Connectors → Add custom connector.') }}</li>
          <li>{{ t('Pega esta dirección:', 'Paste this address:') }}
            <div class="mcp-url"><span>{{ mcpUrl }}</span><button class="btn" type="button" (click)="copy()">{{ copied() ? t('Copiada', 'Copied') : t('Copiar', 'Copy') }}</button></div>
          </li>
          <li>{{ t('Entra con la misma cuenta y permite el acceso. Claude solo ve tu inventario.', 'Sign in with the same account and allow access. Claude only sees your inventory.') }}</li>
        </ol>
      </section>
    </div>
  `,
  styles: `
    .steps { display: grid; gap: 12px; margin: 14px 0 0; padding-left: 20px; color: var(--text); }
    .steps .mcp-url { margin-top: 8px; }
  `
})
export default class SettingsComponent implements OnInit {
  private api = inject(ApiService);
  private store = inject(ProfileStore);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  readonly t = t;
  readonly welcome = signal(false);
  readonly busy = signal(false);
  readonly saved = signal(false);
  readonly copied = signal(false);
  readonly error = signal<string | null>(null);
  readonly mcpUrl = `${appConfig().resource}/mcp`;
  readonly zones = Intl.supportedValuesOf('timeZone');
  readonly currencies = Intl.supportedValuesOf('currency');
  readonly countries = computed(() => {
    const names = new Intl.DisplayNames([lang() === 'es' ? 'es' : 'en'], { type: 'region' });
    return COUNTRIES.map((code) => ({ code, name: names.of(code) ?? code })).sort((a, b) => a.name.localeCompare(b.name));
  });

  country = '';
  currency = '';
  timeZone = '';
  share = false;

  async ngOnInit() {
    try {
      const p = this.store.profile() ?? await this.store.load();
      this.welcome.set(!p.onboarded);
      if (p.onboarded) {
        this.country = p.country; this.currency = p.currency; this.timeZone = p.timeZone; this.share = p.sharePricesByDefault;
      } else {
        // First visit: suggest from the browser
        this.timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        const region = navigator.language.split('-')[1]?.toUpperCase();
        this.country = region && COUNTRIES.includes(region) ? region : p.country;
      }
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos cargar tu perfil.', 'We could not load your profile.')));
    }
    if (this.route.snapshot.fragment === 'assistant') {
      setTimeout(() => document.getElementById('assistant')?.scrollIntoView({ block: 'start' }));
    }
  }

  async save() {
    this.busy.set(true);
    this.error.set(null);
    try {
      const p = await this.api.updateMe({ country: this.country, currency: this.currency || null, timeZone: this.timeZone, sharePricesByDefault: this.share });
      this.store.set(p);
      this.currency = p.currency;
      if (this.welcome()) {
        await this.router.navigateByUrl('/items?new=1');
        return;
      }
      this.saved.set(true);
      setTimeout(() => this.saved.set(false), 2500);
    } catch (e) {
      this.error.set(errorMessage(e, t('No pudimos guardar los ajustes.', 'We could not save the settings.')));
    } finally {
      this.busy.set(false);
    }
  }

  async copy() {
    try {
      await navigator.clipboard.writeText(this.mcpUrl);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    } catch { /* clipboard blocked: the address is selectable */ }
  }
}
