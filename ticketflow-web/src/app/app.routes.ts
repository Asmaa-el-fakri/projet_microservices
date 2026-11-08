import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { ForbiddenPage } from './auth/forbidden-page';
import { CatalogPage } from './event/catalog-page';
import { EventDetailPage } from './event/event-detail-page';
import { ProfilePage } from './profile/profile-page';
import { roleGuard } from './auth/auth.guard';
import { EventFormPage } from './organizer/event-form-page';
import { OrganizerListPage } from './organizer/organizer-list-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'events' },
  { path: 'events', component: CatalogPage, title: 'Événements – TicketFlow' },
  { path: 'events/:id', component: EventDetailPage, title: 'Événement – TicketFlow' },
  { path: 'profile', component: ProfilePage, canActivate: [authGuard], title: 'Mon profil – TicketFlow' },
  { path: 'forbidden', component: ForbiddenPage, title: 'Accès refusé – TicketFlow' },
  { path: 'organizer', component: OrganizerListPage, canActivate: [roleGuard('ORGANIZER')], title: 'Mes événements – TicketFlow' },
  { path: 'organizer/new', component: EventFormPage, canActivate: [roleGuard('ORGANIZER')], title: 'Créer un événement – TicketFlow' },
  { path: 'organizer/:id/edit', component: EventFormPage, canActivate: [roleGuard('ORGANIZER')], title: 'Modifier un événement – TicketFlow' },
  { path: '**', redirectTo: 'events' },
];
