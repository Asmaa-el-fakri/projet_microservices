import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-site-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="site-header">
      <span class="brand">TicketFlow</span>
      <nav aria-label="Navigation principale" class="nav">
        <a routerLink="/events" routerLinkActive="active">Événements</a>
        @if (auth.authenticated()) {
          <a routerLink="/profile" routerLinkActive="active">Mon profil</a>
        }
      </nav>
      <div class="user">
        @if (auth.authenticated()) {
          <span>{{ auth.username() }}</span>
          <button type="button" (click)="auth.logout()">Se déconnecter</button>
        } @else {
          <button type="button" (click)="auth.login()">Se connecter</button>
          <button type="button" class="secondary" (click)="auth.register()">S'inscrire</button>
        }
      </div>
    </header>
  `,
  styles: `
    .site-header { display: flex; align-items: center; gap: 1.5rem; flex-wrap: wrap; }
    .nav { display: flex; gap: 1rem; }
    .nav a { color: #fff; text-decoration: none; padding: 0.25rem 0; border-bottom: 2px solid transparent; }
    .nav a:hover, .nav a.active { border-bottom-color: #f59e0b; }
    .user { margin-left: auto; display: flex; align-items: center; gap: 0.5rem; }
    button { padding: 0.4rem 0.9rem; border: 0; border-radius: 6px; background: #1d4ed8; color: #fff; font: inherit; cursor: pointer; }
    button.secondary { background: #e5e7eb; color: #111827; }
  `,
})
export class SiteHeader {
  readonly auth = inject(AuthService);
}
