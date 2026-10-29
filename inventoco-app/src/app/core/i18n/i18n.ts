import { signal } from '@angular/core';

/**
 * Synapco apps speak Spanish (Colombia) and English. The language comes from ?lang=, then the
 * visitor's last choice (shared by the family under "synapco-lang"), then the browser.
 * Templates and code write each text twice: t('es', 'en').
 */
export type Lang = 'es' | 'en';

function initialLang(): Lang {
  try {
    const fromUrl = new URLSearchParams(location.search).get('lang');
    const stored = fromUrl ?? localStorage.getItem('synapco-lang');
    if (stored === 'es' || stored === 'en') return stored;
  } catch { /* storage blocked */ }
  return navigator.language?.toLowerCase().startsWith('es') ? 'es' : 'en';
}

export const lang = signal<Lang>(initialLang());
document.documentElement.lang = lang() === 'es' ? 'es-CO' : 'en';

export function t(es: string, en: string): string {
  return lang() === 'es' ? es : en;
}

/** Numbers and dates follow the language; amounts keep their own currency. */
export function locale(): string {
  return lang() === 'es' ? 'es-CO' : 'en-US';
}

export function setLang(next: Lang) {
  try { localStorage.setItem('synapco-lang', next); } catch { /* storage blocked */ }
  lang.set(next);
  document.documentElement.lang = next === 'es' ? 'es-CO' : 'en';
}
