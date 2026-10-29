/**
 * Runtime configuration, fetched from /config.json before bootstrap so one image serves
 * every environment (the container writes the file from its env vars at start).
 */
export interface AppConfig {
  /** Inventoco API base, e.g. /inventoco-api */
  apiUrl: string;
  /** authco issuer, e.g. https://demo.example/authco */
  authUrl: string;
  clientId: string;
  /** RFC 8707 resource: Inventoco audience in authco tokens (also the MCP server's base) */
  resource: string;
}

let current: AppConfig | null = null;

export async function loadAppConfig(): Promise<void> {
  const response = await fetch('/config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`config.json: HTTP ${response.status}`);
  }
  current = await response.json();
}

export function appConfig(): AppConfig {
  if (!current) {
    throw new Error('App config not loaded');
  }
  return current;
}

export const redirectUri = () => `${window.location.origin}/auth/callback`;
export const postLogoutRedirectUri = () => `${window.location.origin}/`;
