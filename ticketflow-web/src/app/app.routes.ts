import { Routes } from '@angular/router';
import { CatalogPage } from './event/catalog-page';
import { EventDetailPage } from './event/event-detail-page';
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'events' },
  { path: 'events', component: CatalogPage, title: 'Événements – TicketFlow' },
  { path: '**', redirectTo: 'events' },
  { path: 'events/:id', component: EventDetailPage, title: 'Événement – TicketFlow' },

];
