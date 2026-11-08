export const environment = {
  production: false,
  // URL du Gateway (tous les appels API passeront par là)
  apiBaseUrl: 'http://localhost:8080',
  // Configuration Keycloak
  keycloak: {
    issuer: 'http://localhost:8180/realms/ticketflow',
    redirectUri: 'http://localhost:4200',
    clientId: 'ticketflow-web',
    scope: 'openid profile email',
    responseType: 'code', // PKCE
    showDebugInformation: true
  }
};
