import { HttpErrorResponse } from '@angular/common/http';

export function apiErrorMessage(err: HttpErrorResponse): string {
  if (err.status === 0) return 'Impossible de joindre le serveur.';
  if (err.status === 401) return 'Votre session a expiré. Reconnectez-vous.';
  if (err.status === 403) return "Vous n'avez pas les droits nécessaires.";
  if (err.status === 404) return "Événement introuvable (ou il ne vous appartient pas).";
  const detail = err.error?.detail;
  if ((err.status === 400 || err.status === 409) && typeof detail === 'string') return detail;
  if (err.status === 400) return 'Les informations saisies sont invalides.';
  return 'Une erreur est survenue. Réessayez plus tard.';
}
