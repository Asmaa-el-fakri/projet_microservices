import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden-page',
  imports: [RouterLink],
  template: `
    <main id="main" class="container">
      <h1>Accès refusé</h1>
      <p>Vous n'avez pas les droits nécessaires pour consulter cette page.</p>
      <a routerLink="/events">Retour au catalogue</a>
    </main>
  `,
  styles: `.container { max-width: 800px; margin: 0 auto; padding: 1rem; }`,
})
export class ForbiddenPage {}
