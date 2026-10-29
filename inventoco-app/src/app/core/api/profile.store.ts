import { inject, Injectable, signal } from '@angular/core';
import { ApiService } from './api.service';
import { Profile } from './models';

/** The signed-in user's profile: currency and time zone drive every amount and date on screen. */
@Injectable({ providedIn: 'root' })
export class ProfileStore {

  private api = inject(ApiService);
  readonly profile = signal<Profile | null>(null);
  private loading: Promise<Profile> | null = null;

  load(): Promise<Profile> {
    this.loading ??= this.api.me().then((p) => {
      this.profile.set(p);
      return p;
    }).finally(() => (this.loading = null));
    return this.loading;
  }

  set(p: Profile) {
    this.profile.set(p);
  }

  currency(): string {
    return this.profile()?.currency ?? 'USD';
  }

  timeZone(): string {
    return this.profile()?.timeZone ?? Intl.DateTimeFormat().resolvedOptions().timeZone;
  }
}
