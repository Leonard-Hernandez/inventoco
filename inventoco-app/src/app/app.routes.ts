import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { profileGuard } from './core/api/profile.guard';

export const routes: Routes = [
  { path: 'auth/callback', loadComponent: () => import('./features/auth/auth-callback.component') },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./shell/shell.component'),
    children: [
      { path: 'settings', loadComponent: () => import('./features/settings/settings.component') },
      {
        path: '',
        canActivate: [profileGuard],
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./features/dashboard/dashboard.component') },
          { path: 'items', loadComponent: () => import('./features/items/items.component') },
          { path: 'items/:id', loadComponent: () => import('./features/items/item-detail.component') },
          { path: 'shopping', loadComponent: () => import('./features/shopping/shopping.component') },
          { path: 'suppliers', loadComponent: () => import('./features/suppliers/suppliers.component') }
        ]
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
