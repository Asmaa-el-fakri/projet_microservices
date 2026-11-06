import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { routes } from './app.routes';
import { provideHttpClient, withFetch } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(),
    // Configuration OAuth2 : envoie automatiquement le token dans les headers
    provideOAuthClient({
      resourceServer: {
        allowedUrls: ['http://localhost:8080'], // URL du Gateway
        sendAccessToken: true
      }
    })
  ]
};
