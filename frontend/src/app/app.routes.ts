import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/layout').then((m) => m.LayoutComponent),
  },
  {
    path: 'docs',
    loadComponent: () => import('./docs/docs').then((m) => m.DocsComponent),
  },
];
