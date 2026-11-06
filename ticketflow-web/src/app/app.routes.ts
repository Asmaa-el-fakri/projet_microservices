import { Routes } from '@angular/router';
import { CatalogPage } from './event/catalog-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'events' },
  { path: 'events', component: CatalogPage, title: 'Événements – TicketFlow' },
  { path: '**', redirectTo: 'events' },
];
