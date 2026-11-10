
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { EventItem } from './event.model';
import { EventService } from './event.service';

@Component({
  selector: 'app-catalog-page',
  imports: [FormsModule, DatePipe, DecimalPipe, RouterLink],
  template: `
    <main id="main" class="container">
      <h1>Événements</h1>

      <form class="filters" (ngSubmit)="onSubmit()" aria-label="Recherche d'événements">
        <div class="field">
          <label for="q">Recherche</label>
          <input id="q" name="q" type="search" maxlength="100"
                 placeholder="Titre ou résumé" [(ngModel)]="q" />
        </div>

        <div class="field">
          <label for="city">Ville</label>
          <input id="city" name="city" type="text" [(ngModel)]="city" />
        </div>

        <div class="field">
          <label for="category">Catégorie</label>
          <input id="category" name="category" type="text" [(ngModel)]="category" />
        </div>

        <div class="field">
          <label for="dateFrom">Du</label>
          <input id="dateFrom" name="dateFrom" type="date" [(ngModel)]="dateFrom" />
        </div>

        <div class="field">
          <label for="dateTo">Au</label>
          <input id="dateTo" name="dateTo" type="date" [(ngModel)]="dateTo" />
        </div>

        <div class="field">
          <label for="minPrice">Prix min</label>
          <input id="minPrice" name="minPrice" type="number"
                 min="0" step="0.01" [(ngModel)]="minPrice" />
        </div>

        <div class="field">
          <label for="maxPrice">Prix max</label>
          <input id="maxPrice" name="maxPrice" type="number"
                 min="0" step="0.01" [(ngModel)]="maxPrice" />
        </div>

        <div class="field">
          <label for="sort">Trier par</label>
          <select id="sort" name="sort" [(ngModel)]="sort" (ngModelChange)="onSubmit()">
            <option value="eventDate">Date</option>
            <option value="price">Prix</option>
            <option value="title">Titre</option>
          </select>
        </div>

        <div class="field">
          <label for="direction">Ordre</label>
          <select id="direction" name="direction" [(ngModel)]="direction" (ngModelChange)="onSubmit()">
            <option value="asc">Croissant</option>
            <option value="desc">Décroissant</option>
          </select>
        </div>

        <div class="field">
          <label for="size">Par page</label>
          <select id="size" name="size" [(ngModel)]="size" (ngModelChange)="onSubmit()">
            <option [ngValue]="3">3</option>
            <option [ngValue]="6">6</option>
            <option [ngValue]="12">12</option>
          </select>
        </div>

        <div class="actions">
          <button type="submit">Rechercher</button>
          <button type="button" class="secondary" (click)="reset()">
            Réinitialiser
          </button>
        </div>
      </form>

      @if (formError()) {
        <p class="error" role="alert">{{ formError() }}</p>
      }

      <p class="status" aria-live="polite">
        @if (loading()) {
          Chargement…
        } @else if (!error()) {
          {{ totalElements() }} événement(s) trouvé(s)
        }
      </p>

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      @if (!loading() && !error() && events().length === 0) {
        <p>Aucun événement ne correspond à votre recherche.</p>
      }

      <ul class="cards">
        @for (e of events(); track e.id) {
          <li class="card">
            <h2>
              <a [routerLink]="['/events', e.id]">{{ e.title }}</a>
            </h2>

            <p class="meta">
              {{ e.category }} · {{ e.city }} · {{ e.venue }}
            </p>

            <p>{{ e.summary }}</p>

            <p>
              <time [attr.datetime]="e.eventDate">
                {{ e.eventDate | date: 'dd/MM/yyyy HH:mm' }}
              </time>
            </p>

            <p class="price">
              {{ e.price === 0 ? 'Gratuit' : (e.price | number: '1.0-2') + ' MAD' }}
            </p>
          </li>
        }
      </ul>

      @if (totalPages() > 1) {
        <nav class="pager" aria-label="Pagination">
          <button type="button" (click)="goTo(page() - 1)"
                  [disabled]="page() === 0">
            Précédent
          </button>

          <span>Page {{ page() + 1 }} sur {{ totalPages() }}</span>

          <button type="button" (click)="goTo(page() + 1)"
                  [disabled]="page() + 1 >= totalPages()">
            Suivant
          </button>
        </nav>
      }
    </main>
  `,
  styles: `
    .container {
      max-width: 1100px;
      margin: 0 auto;
      padding: 1rem;
    }

    .filters {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
      gap: 0.75rem;
      align-items: end;
      margin-bottom: 1rem;
    }

    .field {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    label {
      font-weight: 600;
      font-size: 0.9rem;
    }

    input, select {
      padding: 0.5rem;
      border: 1px solid #6b7280;
      border-radius: 6px;
      font: inherit;
    }

    .actions {
      display: flex;
      gap: 0.5rem;
    }

    button {
      padding: 0.55rem 1rem;
      border: 0;
      border-radius: 6px;
      background: #1d4ed8;
      color: #fff;
      font: inherit;
      cursor: pointer;
    }

    button.secondary {
      background: #e5e7eb;
      color: #111827;
    }

    button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .cards {
      list-style: none;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 1rem;
    }

    .card {
      border: 1px solid #d1d5db;
      border-radius: 10px;
      padding: 1rem;
      background: #fff;
    }

    .card h2 {
      margin: 0 0 0.25rem;
      font-size: 1.15rem;
    }

    .card h2 a {
      color: inherit;
    }

    .meta {
      color: #4b5563;
      font-size: 0.9rem;
    }

    .price {
      font-weight: 700;
    }

    .error {
      color: #b91c1c;
      font-weight: 600;
    }

    .pager {
      display: flex;
      gap: 1rem;
      align-items: center;
      justify-content: center;
      margin: 1.5rem 0;
    }
  `,
})
export class CatalogPage implements OnInit {
  private readonly service = inject(EventService);

  // Valeurs du formulaire
  q = '';
  city = '';
  category = '';
  dateFrom = '';
  dateTo = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  sort = 'eventDate';
  direction: 'asc' | 'desc' = 'asc';
  size = 6;

  // État venant du serveur
  readonly events = signal<EventItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  ngOnInit(): void {
    this.load(0);
  }

  onSubmit(): void {
    if (this.minPrice !== null && this.maxPrice !== null &&
      this.minPrice > this.maxPrice) {
      this.formError.set(
        'Le prix minimum ne peut pas dépasser le prix maximum.'
      );
      return;
    }

    if (this.dateFrom && this.dateTo && this.dateFrom > this.dateTo) {
      this.formError.set(
        'La date de début ne peut pas être après la date de fin.'
      );
      return;
    }

    this.formError.set(null);
    this.load(0);
  }

  reset(): void {
    this.q = '';
    this.city = '';
    this.category = '';
    this.dateFrom = '';
    this.dateTo = '';
    this.minPrice = null;
    this.maxPrice = null;
    this.sort = 'eventDate';
    this.direction = 'asc';
    this.size = 6;

    this.formError.set(null);
    this.load(0);
  }

  goTo(p: number): void {
    if (p >= 0 && p < this.totalPages()) {
      this.load(p);
    }
  }

  private load(page: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.service.search({
      q: this.q,
      city: this.city,
      category: this.category,
      dateFrom: this.dateFrom,
      dateTo: this.dateTo,
      minPrice: this.minPrice,
      maxPrice: this.maxPrice,
      sort: this.sort,
      direction: this.direction,
      page,
      size: this.size,
    }).subscribe({
      next: (res) => {
        this.events.set(res.content);
        this.page.set(res.page.number);
        this.totalPages.set(res.page.totalPages);
        this.totalElements.set(res.page.totalElements);
        this.loading.set(false);
      },

      error: (err: HttpErrorResponse) => {
        this.events.set([]);
        this.totalPages.set(0);
        this.totalElements.set(0);

        this.error.set(
          err.status === 0
            ? 'Impossible de joindre le serveur. Vérifie que le Gateway est démarré.'
            : err.status === 400
              ? 'Filtres invalides.'
              : 'Une erreur est survenue. Réessaie plus tard.'
        );

        this.loading.set(false);
      },
    });
  }
}
