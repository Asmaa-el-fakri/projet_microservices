import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EventItem } from './event.model';
import { EventService } from './event.service';

type LoadError = 'notfound' | 'unreachable' | 'other';

@Component({
  selector: 'app-event-detail-page',
  imports: [DatePipe, DecimalPipe, RouterLink],
  template: `
    <main id="main" class="container">
      <a routerLink="/events" class="back">← Retour au catalogue</a>

      @if (loading()) {
        <p aria-live="polite">Chargement…</p>
      } @else if (error() === 'notfound') {
        <h1>Événement introuvable</h1>
        <p>Cet événement n'existe pas ou n'est plus disponible.</p>
      } @else if (error() === 'unreachable') {
        <p class="error" role="alert">Impossible de joindre le serveur. Vérifie que le Gateway est démarré.</p>
      } @else if (error()) {
        <p class="error" role="alert">Une erreur est survenue. Réessaie plus tard.</p>
      } @else if (item(); as e) {
        <article>
          <h1>{{ e.title }}</h1>
          <p class="meta">{{ e.category }}</p>
          <p class="summary">{{ e.summary }}</p>

          <dl>
            <dt>Date</dt>
            <dd><time [attr.datetime]="e.eventDate">{{ e.eventDate | date: 'dd/MM/yyyy HH:mm' }}</time></dd>
            <dt>Lieu</dt>
            <dd>{{ e.venue }}, {{ e.city }}</dd>
            <dt>Prix</dt>
            <dd>{{ e.price === 0 ? 'Gratuit' : (e.price | number: '1.0-2') + ' MAD' }}</dd>
            <dt>Capacité</dt>
            <dd>{{ e.capacity }} places</dd>
          </dl>

          <button type="button" disabled>Réserver</button>
          <p class="hint">La réservation sera disponible après la mise en place de la connexion.</p>
        </article>
      }
    </main>
  `,
  styles: `
    .container { max-width: 800px; margin: 0 auto; padding: 1rem; }
    .back { display: inline-block; margin-bottom: 1rem; color: #1d4ed8; }
    .meta { color: #4b5563; }
    .summary { font-size: 1.1rem; }
    dl { display: grid; grid-template-columns: max-content 1fr; gap: 0.5rem 1.5rem; margin: 1.5rem 0; }
    dt { font-weight: 600; }
    dd { margin: 0; }
    button { padding: 0.6rem 1.2rem; border: 0; border-radius: 6px; background: #1d4ed8; color: #fff; font: inherit; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
    .hint { color: #4b5563; font-size: 0.9rem; }
    .error { color: #b91c1c; font-weight: 600; }
  `,
})
export class EventDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(EventService);
  private readonly titleService = inject(Title);

  readonly item = signal<EventItem | null>(null);
  readonly loading = signal(true);
  readonly error = signal<LoadError | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set('notfound');
      this.loading.set(false);
      return;
    }
    this.service.get(id).subscribe({
      next: (e) => {
        this.item.set(e);
        this.titleService.setTitle(`${e.title} – TicketFlow`);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(err.status === 404 ? 'notfound' : err.status === 0 ? 'unreachable' : 'other');
        this.loading.set(false);
      },
    });
  }
}
