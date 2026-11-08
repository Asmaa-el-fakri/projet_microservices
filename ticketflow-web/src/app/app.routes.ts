import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { ForbiddenPage } from './auth/forbidden-page';
import { CatalogPage } from './event/catalog-page';
import { EventDetailPage } from './event/event-detail-page';
import { ProfilePage } from './profile/profile-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'events' },
  { path: 'events', component: CatalogPage, title: 'Événements – TicketFlow' },
  { path: 'events/:id', component: EventDetailPage, title: 'Événement – TicketFlow' },
  { path: 'profile', component: ProfilePage, canActivate: [authGuard], title: 'Mon profil – TicketFlow' },
  { path: 'forbidden', component: ForbiddenPage, title: 'Accès refusé – TicketFlow' },
  { path: '**', redirectTo: 'events' },
];
