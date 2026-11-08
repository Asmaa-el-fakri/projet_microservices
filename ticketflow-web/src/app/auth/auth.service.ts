import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_CLIENT_ID, KEYCLOAK_REALM, KEYCLOAK_URL } from '../config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = new Keycloak({
    url: KEYCLOAK_URL,
    realm: KEYCLOAK_REALM,
    clientId: KEYCLOAK_CLIENT_ID,
  });

  private readonly _authenticated = signal(false);
  private readonly _username = signal<string | null>(null);
  private readonly _roles = signal<string[]>([]);

  readonly authenticated = this._authenticated.asReadonly();
  readonly username = this._username.asReadonly();
  readonly roles = this._roles.asReadonly();

  /** Appelé au démarrage de l'application. Si Keycloak est arrêté, le site reste utilisable en visiteur. */
  async init(): Promise<void> {
    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256',
        checkLoginIframe: false,
      });
      this.refreshState(authenticated);
    } catch (error) {
      console.warn('Keycloak indisponible : mode visiteur uniquement', error);
      this.refreshState(false);
    }
  }

  login(redirectPath?: string): Promise<void> {
    return this.keycloak.login({
      redirectUri: window.location.origin + (redirectPath ?? window.location.pathname),
    });
  }

  register(): Promise<void> {
    return this.keycloak.register({ redirectUri: window.location.origin + window.location.pathname });
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: window.location.origin });
  }

  hasRole(role: string): boolean {
    return this._roles().includes(role);
  }

  /** Jeton valide (rafraîchi s'il expire dans moins de 30 s), ou null pour un visiteur. */
  async getToken(): Promise<string | null> {
    if (!this.keycloak.authenticated) {
      return null;
    }
    try {
      await this.keycloak.updateToken(30);
    } catch {
      this.refreshState(false);
      return null;
    }
    return this.keycloak.token ?? null;
  }

  private refreshState(authenticated: boolean): void {
    const parsed = this.keycloak.tokenParsed as unknown as
      | { preferred_username?: string; realm_access?: { roles?: string[] } }
      | undefined;
    this._authenticated.set(authenticated);
    this._username.set(authenticated ? (parsed?.preferred_username ?? null) : null);
    this._roles.set(authenticated ? (parsed?.realm_access?.roles ?? []) : []);
  }
}
