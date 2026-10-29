import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ProfileStore } from './profile.store';

/** A new user first confirms country, currency and time zone: every amount and date depends on them. */
export const profileGuard: CanActivateFn = async () => {
  const store = inject(ProfileStore);
  const router = inject(Router);
  const profile = store.profile() ?? await store.load();
  return profile.onboarded ? true : router.parseUrl('/settings?welcome=1');
};
