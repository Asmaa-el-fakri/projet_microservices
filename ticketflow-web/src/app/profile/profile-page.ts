import { HttpErrorResponse } from '@angular/common/http';
import { Component, Injector, OnInit, afterNextRender, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from './profile.service';

@Component({
  selector: 'app-profile-page',
  imports: [FormsModule],
  template: `
    <main id="main" class="container">
      <h1>Mon profil</h1>

      @if (loading()) {
        <p aria-live="polite">Chargement…</p>
      } @else {
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
        @if (message()) {
          <p class="success" role="status">{{ message() }}</p>
        }

        <form (ngSubmit)="save()" aria-label="Modifier mon profil">
          <div class="field">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" [value]="email()" disabled
                   autocomplete="email" aria-describedby="email-hint" />
            <small id="email-hint">L'email ne peut pas être modifié ici.</small>
          </div>
          <div class="field">
            <label for="firstName">Prénom</label>
            <input id="firstName" name="firstName" type="text" required
                   autocomplete="given-name" [(ngModel)]="firstName" />
          </div>
          <div class="field">
            <label for="lastName">Nom</label>
            <input id="lastName" name="lastName" type="text" required
                   autocomplete="family-name" [(ngModel)]="lastName" />
          </div>
          <div class="field">
            <label for="phone">Téléphone</label>
            <input id="phone" name="phone" type="tel" autocomplete="tel" [(ngModel)]="phone" />
          </div>
          <button type="submit" [disabled]="saving()">Enregistrer</button>
        </form>

        <section class="danger" aria-labelledby="close-title">
          <h2 id="close-title">Fermer mon compte</h2>
          @if (closureSent()) {
            <p id="closure-msg" tabindex="-1" role="status">Votre demande de fermeture a été enregistrée.</p>
          } @else if (confirmingClose()) {
            <p>Voulez-vous vraiment demander la fermeture de votre compte ?</p>
            <button type="button" class="warn" (click)="confirmClose()">Oui, fermer mon compte</button>
            <button type="button" id="cancel-close" class="secondary" (click)="cancelClose()">Annuler</button>
          } @else {
            <button type="button" id="ask-close" class="warn" (click)="askClose()">
              Demander la fermeture de mon compte
            </button>
          }
        </section>
      }
    </main>
  `,
  styles: `
    .container { max-width: 600px; margin: 0 auto; padding: 1rem; }
    .field { display: flex; flex-direction: column; gap: 0.25rem; margin-bottom: 1rem; }
    label { font-weight: 600; }
    input { padding: 0.5rem; border: 1px solid #6b7280; border-radius: 6px; font: inherit; }
    input:disabled { background: #e5e7eb; }
    small { color: #4b5563; }
    button { padding: 0.55rem 1rem; border: 0; border-radius: 6px; background: #1d4ed8; color: #fff; font: inherit; cursor: pointer; margin-right: 0.5rem; }
    button.secondary { background: #e5e7eb; color: #111827; }
    button.warn { background: #b91c1c; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
    .error { color: #b91c1c; font-weight: 600; }
    .success { color: #15803d; font-weight: 600; }
    .danger { margin-top: 2.5rem; padding-top: 1rem; border-top: 1px solid #d1d5db; }
  `,
})
export class ProfilePage implements OnInit {
  private readonly service = inject(ProfileService);
  private readonly injector = inject(Injector);

  firstName = '';
  lastName = '';
  phone = '';

  readonly email = signal('');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly message = signal<string | null>(null);
  readonly confirmingClose = signal(false);
  readonly closureSent = signal(false);

  ngOnInit(): void {
    this.service.getMe().subscribe({
      next: (p) => {
        this.firstName = p.firstName ?? '';
        this.lastName = p.lastName ?? '';
        this.phone = p.phone ?? '';
        this.email.set(p.email);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.messageFor(err));
        this.loading.set(false);
      },
    });
  }

  save(): void {
    this.error.set(null);
    this.message.set(null);
    if (!this.firstName.trim() || !this.lastName.trim()) {
      this.error.set('Le prénom et le nom sont obligatoires.');
      return;
    }
    this.saving.set(true);
    this.service
      .updateMe({
        firstName: this.firstName.trim(),
        lastName: this.lastName.trim(),
        phone: this.phone.trim() || null,
      })
      .subscribe({
        next: () => {
          this.message.set('Profil enregistré.');
          this.saving.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.error.set(this.messageFor(err));
          this.saving.set(false);
        },
      });
  }

  askClose(): void {
    this.confirmingClose.set(true);
    this.focusLater('cancel-close');
  }

  cancelClose(): void {
    this.confirmingClose.set(false);
    this.focusLater('ask-close');
  }

  confirmClose(): void {
    this.error.set(null);
    this.service.requestClosure().subscribe({
      next: () => {
        this.closureSent.set(true);
        this.confirmingClose.set(false);
        this.focusLater('closure-msg');
      },
      error: (err: HttpErrorResponse) => this.error.set(this.messageFor(err)),
    });
  }

  // Remet le focus clavier sur un element qui vient d'apparaitre dans la page
  private focusLater(id: string): void {
    afterNextRender(() => document.getElementById(id)?.focus(), { injector: this.injector });
  }

  private messageFor(err: HttpErrorResponse): string {
    if (err.status === 0) return 'Impossible de joindre le serveur.';
    if (err.status === 401) return 'Votre session a expiré. Reconnectez-vous.';
    if (err.status === 400) return 'Les informations saisies sont invalides.';
    return 'Une erreur est survenue. Réessayez plus tard.';
  }
}
