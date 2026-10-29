import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { lang, Lang, setLang, t } from '../core/i18n/i18n';

/** Synapco T-frame: sidebar and top bar on one ground, the work in the canvas, five destinations on the phone. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app">
      <aside class="sidebar" [attr.aria-label]="t('Navegación', 'Navigation')">
        <a class="brand" routerLink="/">
          <svg viewBox="0 0 22 22" aria-hidden="true"><circle cx="6" cy="11" r="3" fill="#cde736"/><circle cx="16" cy="6" r="2" fill="#ffffff"/><circle cx="16" cy="16" r="2" fill="#ffffff"/><path d="M9 10 14 6.6M9 12l5 3.4" stroke="#cde736" stroke-width="1.2"/></svg>
          <span class="wordmark">Invento<i>co</i></span>
        </a>
        @for (n of nav; track n.path) {
          <a class="nav-item" [routerLink]="n.path" routerLinkActive ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{ exact: n.path === '/' }">{{ t(n.es, n.en) }}</a>
        }
        <span class="group-label">{{ t('Asistente', 'Assistant') }}</span>
        <a class="nav-item" routerLink="/settings" fragment="assistant">{{ t('Conectar con Claude', 'Connect Claude') }}</a>
        <div class="bottom">
          <a class="nav-item" routerLink="/settings" routerLinkActive ariaCurrentWhenActive="page">{{ t('Ajustes', 'Settings') }}</a>
          <button class="nav-item signout" type="button" (click)="auth.logout()">{{ t('Salir', 'Sign out') }}</button>
          <div class="lang" role="group" [attr.aria-label]="t('Idioma', 'Language')">
            <button type="button" [attr.aria-pressed]="lang() === 'es'" (click)="choose('es')">Español</button>
            <button type="button" [attr.aria-pressed]="lang() === 'en'" (click)="choose('en')">English</button>
          </div>
        </div>
      </aside>
      <header class="topbar">
        <a class="phone-brand" routerLink="/">
          <svg viewBox="0 0 22 22" aria-hidden="true"><circle cx="6" cy="11" r="3" fill="#cde736"/><circle cx="16" cy="6" r="2" fill="#fff"/><circle cx="16" cy="16" r="2" fill="#fff"/></svg>
          <span class="wordmark">Invento<i>co</i></span>
        </a>
        <a class="ask" routerLink="/settings" fragment="assistant">
          <svg viewBox="0 0 16 16" aria-hidden="true"><circle cx="7" cy="7" r="4.5"/><path d="m10.5 10.5 3 3"/></svg>
          <span>{{ t('Pregúntale a Claude por tu inventario', 'Ask Claude about your inventory') }}</span>
        </a>
        <div class="switcher"><span class="scope">Synapco ·</span> <span>{{ t('Inventoco · Mi inventario', 'Inventoco · My inventory') }}</span></div>
      </header>
      <main class="canvas">
        <router-outlet />
      </main>
      <nav class="bottom-nav" [attr.aria-label]="t('Navegación', 'Navigation')">
        <a routerLink="/" routerLinkActive ariaCurrentWhenActive="page" [routerLinkActiveOptions]="{ exact: true }"><svg viewBox="0 0 18 18" aria-hidden="true"><path d="M3 8.5 9 3.5l6 5V15H3z"/></svg>{{ t('Inicio', 'Home') }}</a>
        <a routerLink="/items" routerLinkActive ariaCurrentWhenActive="page"><svg viewBox="0 0 18 18" aria-hidden="true"><path d="M3 4.5h12M3 9h12M3 13.5h12"/></svg>{{ t('Inventario', 'Items') }}</a>
        <a routerLink="/shopping" routerLinkActive ariaCurrentWhenActive="page"><svg viewBox="0 0 18 18" aria-hidden="true"><path d="M3 4h2l1.6 7.5h7.4L15.5 6H6"/><circle cx="7.5" cy="14.5" r="1"/><circle cx="13" cy="14.5" r="1"/></svg>{{ t('Compras', 'Shopping') }}</a>
        <a routerLink="/suppliers" routerLinkActive ariaCurrentWhenActive="page"><svg viewBox="0 0 18 18" aria-hidden="true"><path d="M2.5 7.5 4 3.5h10l1.5 4M3.5 7.5V15h11V7.5M7 15v-4h4v4"/></svg>{{ t('Proveedores', 'Suppliers') }}</a>
        <a routerLink="/settings" routerLinkActive ariaCurrentWhenActive="page"><svg viewBox="0 0 18 18" aria-hidden="true"><circle cx="9" cy="9" r="2.5"/><path d="M9 2.5v2M9 13.5v2M2.5 9h2M13.5 9h2M4.4 4.4l1.4 1.4M12.2 12.2l1.4 1.4M4.4 13.6l1.4-1.4M12.2 5.8l1.4-1.4"/></svg>{{ t('Ajustes', 'Settings') }}</a>
      </nav>
    </div>
  `,
  styles: `
    .signout { width: 100%; border: 0; background: transparent; cursor: pointer; text-align: left; font: inherit; }
    .ask { text-decoration: none; }
    .ask:hover { color: var(--text); }
  `
})
export default class ShellComponent {
  readonly auth = inject(AuthService);
  readonly lang = lang;
  readonly t = t;
  readonly nav = [
    { path: '/', es: 'Inicio', en: 'Home' },
    { path: '/items', es: 'Inventario', en: 'Items' },
    { path: '/shopping', es: 'Lista de compras', en: 'Shopping list' },
    { path: '/suppliers', es: 'Proveedores', en: 'Suppliers' }
  ];

  choose(next: Lang) {
    setLang(next);
  }
}
