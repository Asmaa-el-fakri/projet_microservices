# notification-service

Microservice TicketFlow chargé d'**envoyer et de suivre les notifications** déclenchées par les événements métier publiés sur Kafka. Il ne contient aucune logique de réservation, de paiement, d'événement ou de profil client.

- Package : `com.ticketflow.notification_service` · Port par défaut : `8085` (à confirmer, voir INTEGRATION.md)
- Base propriétaire : `notification_db` (PostgreSQL, migrations Flyway)
- Canal actuel : email (adaptateur SMTP + adaptateur de développement qui journalise)

## Fonctionnement

```
Kafka (topics ticketflow.*) ──► NotificationEventListener
                                   │ EventParser (JSON → EventEnvelope)
                                   │ EventValidator (contrat v1)
                                   ▼
                          NotificationIngestService ── doublon ? ──► ignoré
                                   │ destinataires, préférences, gabarit
                                   ▼
                       table notifications (PENDING / SKIPPED)
                                   ▼
                       NotificationDeliveryService ──► NotificationSender (email)
                                   │ succès → SENT
                                   │ échec  → PENDING + backoff, puis FAILED
                                   ▼
                     NotificationRetryScheduler (relance les PENDING échus)
```

Garanties :

- **Idempotence** : contrainte unique `(event_id, channel, recipient)` + vérification préalable. Un même événement rejoué ne crée pas de seconde notification.
- **Un échec d'envoi n'impacte jamais le métier** : il est enregistré (`attempt_count`, `last_error`) et rejoué avec backoff exponentiel (30 s, 60 s, 120 s... plafonné à 1 h) jusqu'à `max-attempts`, puis `FAILED`.
- **Messages invalides** (JSON illisible, version non supportée, destinataire invalide) : non rejoués, envoyés au topic `<topic>.DLT`.
- **Erreurs techniques** (base indisponible) : 3 tentatives côté consommateur, puis `<topic>.DLT`.
- **Mode développement honnête** : avec `notification.email.mode=log` (défaut), aucun email n'est envoyé ; la notification est journalisée (adresse masquée, sans contenu) et enregistrée avec `provider=log`.

## Contrat d'événement (v1)

Tous les messages sont du JSON UTF-8 (valeur Kafka en String, clé libre, par exemple `aggregateId`).

```json
{
  "eventId": "7b1c2a4e-5d3f-4c8a-9a51-0d2f6e8b1a10",
  "eventType": "BookingConfirmed",
  "eventVersion": 1,
  "occurredAt": "2026-11-15T12:00:00Z",
  "aggregateId": "booking-42",
  "correlationId": "req-9f3a",
  "payload": {
    "recipientEmail": "client@example.com",
    "eventTitle": "Concert de printemps",
    "eventDate": "2027-03-20 20:00",
    "bookingId": "B-42"
  }
}
```

| Champ | Obligatoire | Règle |
|---|---|---|
| `eventId` | oui | UUID unique, clé d'idempotence |
| `eventType` | oui | voir tableau ci-dessous |
| `eventVersion` | oui | `1` uniquement |
| `occurredAt` | oui | ISO-8601 UTC |
| `aggregateId`, `correlationId` | non | 100 caractères max, conservés pour le suivi |
| `payload` | selon le type | voir ci-dessous |

| `eventType` | Notification | Destinataire | Champs de payload utilisés |
|---|---|---|---|
| `BookingConfirmed` | Réservation confirmée | obligatoire | `eventTitle`, `eventDate`, `bookingId` |
| `BookingExpired` | Réservation expirée | obligatoire | `eventTitle`, `bookingId` |
| `PaymentSucceeded` | Paiement reçu | obligatoire | `amount`, `currency`, `bookingId` |
| `PaymentFailed` | Échec du paiement | obligatoire | `amount`, `currency`, `bookingId` |
| `TicketIssued` | Billet disponible | obligatoire | `eventTitle`, `eventDate` (jamais le code du billet) |
| `EventPublished` | Nouvel événement | optionnel (diffusion) | `eventTitle`, `eventDate` |
| `EventClosed` | Événement clôturé / modifié | optionnel (diffusion) | `eventTitle`, `eventDate` |
| `NotificationRequested` | Message libre / rappel | obligatoire | `subject`, `message` |
| `BookingCreated` | aucune (consommé puis ignoré) | – | – |

Destinataires : `payload.recipientEmail` (un) et/ou `payload.recipientEmails` (liste, 500 max). Les valeurs du payload sont nettoyées (caractères de contrôle supprimés, longueur bornée) avant d'être insérées dans un email.

Minimisation des données : le producteur ne place dans l'événement que l'adresse du destinataire et le strict nécessaire. Rien d'autre n'est copié du profil client.

### Topics consommés (configurables : `notification.kafka.topics`)

`ticketflow.events.event`, `ticketflow.events.booking`, `ticketflow.events.payment`, `ticketflow.events.ticket`, `ticketflow.notifications.requested`, plus leurs `.DLT`.

### Producteurs à intégrer plus tard

Aucun producteur n'est supposé exister. Voir INTEGRATION.md, section « Checklist producteurs ».

## Préférences et désabonnement

Les préférences appartiennent au Customer Service. Ce service expose l'interface `PreferenceService` (par défaut `AllowAllPreferenceService`, qui autorise tout). Les notifications transactionnelles (réservation, paiement, billet) ne sont jamais bloquées ; les notifications informatives (`EventPublished`, `EventClosed`, `NotificationRequested`) sont enregistrées en `SKIPPED` si la préférence les refuse. Pour brancher la vraie source, implémenter `PreferenceService` (appel d'une API interne du Customer Service avec authentification de service, ou projection d'événements) et remplacer `AllowAllPreferenceService`.

## API REST (JWT Keycloak, via le Gateway uniquement si exposée)

| Endpoint | Rôle | Description |
|---|---|---|
| `GET /api/notifications/me?page&size` | CLIENT, ORGANIZER, ADMIN | Historique de l'utilisateur, identifié par la claim `email` du jeton (jamais par paramètre) |
| `GET /api/notifications?status&page&size` | ADMIN | Supervision, filtrable par statut |
| `GET /api/notifications/{id}` | ADMIN | Détail d'une notification |
| `GET /actuator/health` | public | Santé |

Les rôles sont lus dans `realm_access.roles` du jeton.

## Configuration (variables d'environnement)

| Variable | Défaut | Rôle |
|---|---|---|
| `CONFIG_SERVER_URL` | `http://localhost:8888` | Config Server |
| `NOTIFICATION_DB_URL` | `jdbc:postgresql://localhost:5432/notification_db` | Base |
| `NOTIFICATION_DB_USERNAME` / `NOTIFICATION_DB_PASSWORD` | aucun (obligatoires) | Identifiants base |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9094` | Kafka (`kafka:9092` dans Docker) |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Eureka |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/ticketflow` | Émetteur JWT (vérifier le nom du realm) |
| `NOTIFICATION_EMAIL_MODE` | `log` | `log` ou `smtp` |
| `NOTIFICATION_EMAIL_FROM` | `no-reply@ticketflow.local` | Expéditeur |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | aucun | SMTP (mode `smtp`) |
| `SERVER_PORT` | `8085` | Port HTTP |

Aucun secret n'est présent dans le code ni dans le dépôt de configuration.

## Build, tests, exécution

```powershell
# depuis la racine du dépôt TicketFlow
.\mvnw.cmd -pl services/notification-service -am test
.\mvnw.cmd -pl services/notification-service -am package
```

Les tests actuels sont des tests unitaires (JUnit 5 + Mockito, sans base ni Kafka) : parsing, validation, création, doublons, livraison, échecs, reprises, statuts. Il n'y a pas de test d'intégration avec Kafka/PostgreSQL réels (voir INTEGRATION.md pour la vérification manuelle).

## Structure

```
config/       NotificationProperties, KafkaConfig (DLT), SecurityConfig, AppConfig
controllers/  NotificationController
dto/          EventEnvelope, NotificationResponse, PageResponse, ApiError
entities/     Notification, NotificationStatus, NotificationChannel
repos/        NotificationRepository
exceptions/   InvalidEventException, DeliveryException, NotificationNotFoundException, GlobalExceptionHandler
messaging/    EventParser, NotificationEventListener
service/      Ingest, Delivery, RetryScheduler, Query, Template, RecipientResolver, EventValidator, PreferenceService
adapter/      NotificationSender, SmtpEmailSender, LoggingEmailSender, SenderRegistry
```

Ajouter un canal (SMS, Telegram) : une valeur dans `NotificationChannel`, une implémentation de `NotificationSender`, et la contrainte `ck_notification_channel` à étendre par une migration.
