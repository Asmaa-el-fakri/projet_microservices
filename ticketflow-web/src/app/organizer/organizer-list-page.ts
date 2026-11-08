import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EventItem, EventStatus } from '../event/event.model';
import { apiErrorMessage } from '../shared/api-error';
import { OrganizerService } from './organizer.service';

type Action = 'publish' | 'close' | 'archive';

@Component({
  selector: 'app-organizer-list-page',
  imports: [FormsModule, RouterLink, DatePipe, DecimalPipe],
  template: `
    <main id="main" class="container">
      <div class="top">
        <h1>Mes événements</h1>
        <a routerLink="/organizer/new" class="btn">Créer un événement</a>
      </div>

      <div class="filter">
        <label for="status">Statut</label>
        <select id="status" name="status" [ngModel]="status" (ngModelChange)="onStatusChange($event)">
          <option value="">Tous</option>
          <option value="DRAFT">Brouillon</option>
          <option value="PUBLISHED">Publié</option>
          <option value="CLOSED">Clôturé</option>
          <option value="ARCHIVED">Archivé</option>
        </select>
      </div>

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
      <p class="status" aria-live="polite">
        @if (loading()) {
          Chargement…
        } @else if (message()) {
          {{ message() }}
        } @else if (!error()) {
          {{ totalElements() }} événement(s)
        }
      </p>

      @if (!loading() && !error() && events().length === 0) {
        <p>Aucun événement. Commencez par en créer un.</p>
      }

      @if (events().length > 0) {
        <div class="table-wrap">
          <table>
            <caption class="sr-only">Liste de mes événements</caption>
            <thead>
              <tr>
                <th scope="col">Titre</th>
                <th scope="col">Date</th>
                <th scope="col">Ville</th>
                <th scope="col">Prix</th>
                <th scope="col">Capacité</th>
                <th scope="col">Statut</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (e of events(); track e.id) {
                <tr>
                  <th scope="row">{{ e.title }}</th>
                  <td>{{ e.eventDate | date: 'dd/MM/yyyy HH:mm' }}</td>
                  <td>{{ e.city }}</td>
                  <td>{{ e.price === 0 ? 'Gratuit' : (e.price | number: '1.0-2') + ' MAD' }}</td>
                  <td>{{ e.capacity }}</td>
                  <td>{{ label(e.status) }}</td>
                  <td class="actions">
                    @if (canEdit(e)) {
                      <a [routerLink]="['/organizer', e.id, 'edit']" [attr.aria-label]="'Modifier ' + e.title">Modifier</a>
                    }
                    @if (e.status === 'DRAFT') {
                      <button type="button" (click)="act('publish', e)" [attr.aria-label]="'Publier ' + e.title">Publier</button>
                    }
                    @if (e.status === 'PUBLISHED') {
                      <button type="button" (click)="act('close', e)" [attr.aria-label]="'Clôturer ' + e.title">Clôturer</button>
                    }
                    @if (e.status === 'DRAFT' || e.status === 'CLOSED') {
                      <button type="button" class="secondary" (click)="act('archive', e)" [attr.aria-label]="'Archiver ' + e.title">Archiver</button>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (totalPages() > 1) {
        <nav class="pager" aria-label="Pagination">
          <button type="button" (click)="load(page() - 1)" [disabled]="page() === 0">Précédent</button>
          <span>Page {{ page() + 1 }} sur {{ totalPages() }}</span>
          <button type="button" (click)="load(page() + 1)" [disabled]="page() + 1 >= totalPages()">Suivant</button>
        </nav>
      }
    </main>
  `,
  styles: `
    .container { max-width: 1100px; margin: 0 auto; padding: 1rem; }
    .top { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem; }
    .btn { background: #1d4ed8; color: #fff; padding: 0.55rem 1rem; border-radius: 6px; text-decoration: none; }
    .filter { display: flex; align-items: center; gap: 0.5rem; margin: 1rem 0; }
    label { font-weight: 600; }
    select { padding: 0.5rem; border: 1px solid #6b7280; border-radius: 6px; font: inherit; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; border-collapse: collapse; background: #fff; }
    th, td { text-align: left; padding: 0.6rem; border-bottom: 1px solid #d1d5db; }
    thead th { background: #e5e7eb; }
    .actions { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center; }
    .actions a { color: #1d4ed8; }
    button { padding: 0.4rem 0.8rem; border: 0; border-radius: 6px; background: #1d4ed8; color: #fff; font: inherit; cursor: pointer; }
    button.secondary { background: #e5e7eb; color: #111827; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
    .error { color: #b91c1c; font-weight: 600; }
    .pager { display: flex; gap: 1rem; align-items: center; justify-content: center; margin: 1.5rem 0; }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
  `,
})
export class OrganizerListPage implements OnInit {
  private readonly service = inject(OrganizerService);

  status: EventStatus | '' = '';
  readonly size = 10;

  readonly events = signal<EventItem[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  private readonly labels: Record<EventStatus, string> = {
    DRAFT: 'Brouillon',
    PUBLISHED: 'Publié',
    CLOSED: 'Clôturé',
    ARCHIVED: 'Archivé',
  };

  private readonly done: Record<Action, string> = {
    publish: 'Événement publié.',
    close: 'Événement clôturé.',
    archive: 'Événement archivé.',
  };

  ngOnInit(): void {
    this.load(0);
  }

  label(status: EventStatus): string {
    return this.labels[status];
  }

  canEdit(e: EventItem): boolean {
    return e.status === 'DRAFT' || e.status === 'PUBLISHED';
  }

  onStatusChange(value: EventStatus | ''): void {
    this.status = value;
    this.message.set(null);
    this.load(0);
  }

  act(action: Action, e: EventItem): void {
    if (action === 'close' && !confirm(`Clôturer « ${e.title} » ? Il ne sera plus visible dans le catalogue.`)) {
      return;
    }
    if (action === 'archive' && !confirm(`Archiver « ${e.title} » ?`)) {
      return;
    }
    this.error.set(null);
    this.message.set(null);
    this.service[action](e.id).subscribe({
      next: () => {
        this.load(this.page());
        this.message.set(this.done[action]);
      },
      error: (err: HttpErrorResponse) => this.error.set(apiErrorMessage(err)),
    });
  }

  load(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list(this.status, page, this.size).subscribe({
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
        this.error.set(apiErrorMessage(err));
        this.loading.set(false);
      },
    });
  }
}
