import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EventStatus } from '../event/event.model';
import { apiErrorMessage } from '../shared/api-error';
import { EventPayload, OrganizerService } from './organizer.service';

function toLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

@Component({
  selector: 'app-event-form-page',
  imports: [FormsModule, RouterLink],
  template: `
    <main id="main" class="container">
      <a routerLink="/organizer" class="back">← Mes événements</a>

      @if (isEdit()) {
        <h1>Modifier l'événement</h1>
      } @else {
        <h1>Créer un événement</h1>
      }

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }

      @if (loading()) {
        <p aria-live="polite">Chargement…</p>
      } @else if (!loadFailed()) {
        @if (status() === 'PUBLISHED') {
          <p class="hint">Événement publié : la capacité peut augmenter, mais pas diminuer.</p>
        }
        @if (readOnly()) {
          <p class="error" role="alert">Cet événement est clôturé ou archivé : il ne peut plus être modifié.</p>
        }

        <form (ngSubmit)="save()" aria-label="Formulaire d'événement">
          <div class="field">
            <label for="title">Titre</label>
            <input id="title" name="title" type="text" required maxlength="150" [(ngModel)]="title" [disabled]="readOnly()" />
          </div>
          <div class="field">
            <label for="summary">Résumé</label>
            <textarea id="summary" name="summary" rows="4" required [(ngModel)]="summary" [disabled]="readOnly()"></textarea>
          </div>
          <div class="field">
            <label for="eventDate">Date et heure</label>
            <input id="eventDate" name="eventDate" type="datetime-local" required [(ngModel)]="eventDate" [disabled]="readOnly()" />
          </div>
          <div class="field">
            <label for="city">Ville</label>
            <input id="city" name="city" type="text" required [(ngModel)]="city" [disabled]="readOnly()" />
          </div>
          <div class="field">
            <label for="venue">Lieu</label>
            <input id="venue" name="venue" type="text" required [(ngModel)]="venue" [disabled]="readOnly()" />
          </div>
          <div class="field">
            <label for="category">Catégorie</label>
            <input id="category" name="category" type="text" required [(ngModel)]="category" [disabled]="readOnly()" />
          </div>
          <div class="field">
            <label for="price">Prix (0 pour gratuit)</label>
            <input id="price" name="price" type="number" min="0" step="0.01" required [(ngModel)]="price" [disabled]="readOnly()" />
          </div>
          <div class="field">
            <label for="capacity">Capacité (nombre de places)</label>
            <input id="capacity" name="capacity" type="number" min="1" step="1" required [(ngModel)]="capacity" [disabled]="readOnly()" />
          </div>

          <button type="submit" [disabled]="readOnly() || saving()">Enregistrer</button>
          <a routerLink="/organizer" class="cancel">Annuler</a>
        </form>
      }
    </main>
  `,
  styles: `
    .container { max-width: 640px; margin: 0 auto; padding: 1rem; }
    .back { display: inline-block; margin-bottom: 1rem; color: #1d4ed8; }
    .field { display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1rem; }
    label { font-weight: 600; }
    input, textarea { padding: 0.5rem; border: 1px solid #6b7280; border-radius: 6px; font: inherit; }
    input:disabled, textarea:disabled { background: #e5e7eb; }
    button { padding: 0.55rem 1rem; border: 0; border-radius: 6px; background: #1d4ed8; color: #fff; font: inherit; cursor: pointer; margin-right: 1rem; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
    .cancel { color: #1d4ed8; }
    .hint { color: #4b5563; }
    .error { color: #b91c1c; font-weight: 600; }
  `,
})
export class EventFormPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(OrganizerService);

  private id: number | null = null;

  title = '';
  summary = '';
  eventDate = '';
  city = '';
  venue = '';
  category = '';
  price: number | null = 0;
  capacity: number | null = 100;

  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly loadFailed = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly status = signal<EventStatus | null>(null);
  readonly readOnly = computed(() => this.status() === 'CLOSED' || this.status() === 'ARCHIVED');

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam === null) {
      return;
    }
    const id = Number(idParam);
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set('Identifiant invalide.');
      this.loadFailed.set(true);
      return;
    }
    this.id = id;
    this.isEdit.set(true);
    this.loading.set(true);
    this.service.get(id).subscribe({
      next: (e) => {
        this.title = e.title;
        this.summary = e.summary;
        this.eventDate = toLocalInput(e.eventDate);
        this.city = e.city;
        this.venue = e.venue;
        this.category = e.category;
        this.price = e.price;
        this.capacity = e.capacity;
        this.status.set(e.status);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.loadFailed.set(true);
        this.loading.set(false);
      },
    });
  }

  save(): void {
    this.error.set(null);
    const problem = this.validate();
    if (problem) {
      this.error.set(problem);
      return;
    }
    const payload: EventPayload = {
      title: this.title.trim(),
      summary: this.summary.trim(),
      eventDate: new Date(this.eventDate).toISOString(),
      city: this.city.trim(),
      venue: this.venue.trim(),
      category: this.category.trim(),
      price: this.price as number,
      capacity: this.capacity as number,
    };
    this.saving.set(true);
    const request = this.id === null ? this.service.create(payload) : this.service.update(this.id, payload);
    request.subscribe({
      next: () => this.router.navigate(['/organizer']),
      error: (err: HttpErrorResponse) => {
        this.error.set(apiErrorMessage(err));
        this.saving.set(false);
      },
    });
  }

  private validate(): string | null {
    if (![this.title, this.summary, this.city, this.venue, this.category].every((v) => v.trim())) {
      return 'Tous les champs texte sont obligatoires.';
    }
    if (!this.eventDate || Number.isNaN(new Date(this.eventDate).getTime())) {
      return 'La date est obligatoire.';
    }
    if (new Date(this.eventDate).getTime() <= Date.now()) {
      return "La date de l'événement doit être dans le futur.";
    }
    if (this.price === null || this.price < 0) {
      return 'Le prix doit être positif ou nul.';
    }
    if (this.capacity === null || !Number.isInteger(this.capacity) || this.capacity < 1) {
      return 'La capacité doit être un entier supérieur ou égal à 1.';
    }
    return null;
  }
}
