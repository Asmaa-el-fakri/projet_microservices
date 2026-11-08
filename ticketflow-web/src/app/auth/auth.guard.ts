import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Page réservée aux utilisateurs connectés : sinon, redirection vers Keycloak. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (!auth.authenticated()) {
    auth.login(state.url);
    return false;
  }
  return true;
};

/** Page réservée à un rôle (ORGANIZER, ADMIN…). Servira à la console organisateur. */
export const roleGuard = (role: string): CanActivateFn => (_route, state) => {
  const auth = inject(AuthService);
  if (!auth.authenticated()) {
    auth.login(state.url);
    return false;
  }
  return auth.hasRole(role) ? true : inject(Router).createUrlTree(['/forbidden']);
};
