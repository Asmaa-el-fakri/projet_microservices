# TicketFlow – Contrat des événements Kafka

Ce document décrit les messages échangés entre services sur Kafka. Il suit les fiches Kafka du cours (topics `ticketflow.*`, message avec `eventId`, `eventType`, `version`, `occurredAt`, `data`). Un producteur ne change pas un champ existant sans prévenir les consommateurs.

Statuts : **implémenté** (déjà publié par le code), **proposé** (à valider avec la partie B ou C avant de coder).

## Règles communes

- Un message est une chaîne JSON (clé et valeur en `String`). Le consommateur lit le JSON avec Jackson, sans dépendre des classes du producteur, et ignore les champs inconnus.
- Clé du message : l'identifiant de la réservation (`bookingId`, en texte). Tous les événements d'une même réservation arrivent dans l'ordre sur la même partition.
- Chaque topic a 3 partitions (configuration du broker).
- Livraison « au moins une fois » : un message peut arriver deux fois. **Tout consommateur est idempotent** : il enregistre les `eventId` déjà traités (contrainte d'unicité en base) et ignore les doublons.
- Un message illisible ou impossible à traiter, après les réessais, part dans le topic `ticketflow.notification.dlt`.
- Un échec de Kafka ou d'un consommateur n'annule jamais une réservation ni un paiement (cahier des charges, section 3.8).
- Évolution du schéma : on ajoute des champs, on n'en retire pas et on ne change pas leur sens. Un changement incompatible incrémente `version`.
- Données personnelles minimisées : jamais d'e-mail, de nom ni de téléphone dans un événement.

## Enveloppe commune

| Champ | Type | Description |
|---|---|---|
| `eventId` | texte (UUID) | identifiant unique du message, sert à dédoublonner |
| `eventType` | texte | nom de l'événement (`PaymentSucceeded`, …) |
| `version` | entier | version du schéma, `1` aujourd'hui |
| `occurredAt` | date UTC (ISO 8601) | moment du changement d'état |
| `aggregateId` | texte | identifiant de l'objet concerné (exigence de la section 7.3, en plus de la fiche) |
| `correlationId` | texte | `booking-<id>` : relie tous les événements d'une réservation (section 7.3, en plus de la fiche) |
| `data` | objet | contenu propre à chaque événement |

Les champs `aggregateId` et `correlationId` s'ajoutent à ceux de la fiche. Un consommateur qui n'en a pas besoin les ignore.

## Topics

| Topic | Producteur | Événement | Statut |
|---|---|---|---|
| `ticketflow.payment.succeeded` | payment-service | `PaymentSucceeded` | implémenté |
| `ticketflow.payment.failed` | payment-service | `PaymentFailed` | implémenté |
| `ticketflow.booking.cancelled` | booking-service | `BookingCancelled` | proposé (topic de la fiche) |
| `ticketflow.booking.created` | booking-service | `BookingCreated` | proposé (hors fiche) |
| `ticketflow.booking.confirmed` | booking-service | `BookingConfirmed` | proposé (hors fiche) |
| `ticketflow.ticket.issued` | ticket-service | `TicketIssued` | proposé (hors fiche) |
| `ticketflow.notification.dlt` | notification-service | messages en échec | prévu par la fiche |

## payment-service : `ticketflow.payment.succeeded` et `ticketflow.payment.failed`

Statut : **implémenté**. Le message est publié **après** l'enregistrement du paiement en base.

| Événement | Topic | Quand | Contenu de `data` |
|---|---|---|---|
| `PaymentSucceeded` | `ticketflow.payment.succeeded` | le paiement passe en `SUCCEEDED` | `paymentId`, `bookingId`, `customerRef`, `amount`, `currency`, `reason` (vide) |
| `PaymentFailed` | `ticketflow.payment.failed` | le paiement passe en `FAILED` ou `CANCELLED` | `paymentId`, `bookingId`, `customerRef`, `amount`, `currency`, `reason` : `FAILURE` ou `CANCELLED` |

Exemple :

```json
{
  "eventId": "ac0a0b4d-6757-4d24-8e0a-05cd19826e5d",
  "eventType": "PaymentSucceeded",
  "version": 1,
  "occurredAt": "2026-11-10T08:08:16.050504500Z",
  "aggregateId": "3",
  "correlationId": "booking-9",
  "data": {
    "paymentId": 3,
    "bookingId": 9,
    "customerRef": "8f1c2a7e-0000-4000-8000-000000000001",
    "amount": 150.00,
    "currency": "MAD",
    "reason": null
  }
}
```

**Différence avec l'exemple de la fiche.** La fiche met un `customerId` dans `data`. Payment-service ne connaît que l'identifiant Keycloak du client (le `sub` du jeton), pas son `customerId`. Il le publie donc sous le nom `customerRef`. Quand booking publiera `BookingCreated` avec le `customerId` de la réservation, payment-service pourra le reprendre et publier `customerId` à la place.

Si Kafka est arrêté à l'instant de la publication, le paiement reste valide et l'événement est perdu (limite connue : pas de « transactional outbox »).

## booking-service : à publier (statut **proposé**)

Clé : `bookingId`.

| Événement | Topic | Quand | Contenu de `data` |
|---|---|---|---|
| `BookingCreated` | `ticketflow.booking.created` | la réservation est créée en attente de paiement | `bookingId`, `catalogEventId`, `quantity`, `unitPrice`, `totalPrice`, `currency`, `customerId` |
| `BookingConfirmed` | `ticketflow.booking.confirmed` | le paiement a réussi | `bookingId`, `catalogEventId`, `quantity`, `customerId` |
| `BookingCancelled` | `ticketflow.booking.cancelled` | paiement échoué ou annulé, ou annulation du client | `bookingId`, `reason` |

Précisions :

- `catalogEventId` est l'identifiant de l'événement du catalogue (event-service) : le nom `eventId` est déjà pris par l'enveloppe.
- Le prix est figé à la création de la réservation, à partir d'event-service.
- Booking consomme `ticketflow.payment.succeeded` (réservation `CONFIRMED`) et `ticketflow.payment.failed` (réservation `CANCELLED`), avec le groupe `booking-service`.

## ticket-service : à publier (statut **proposé**)

Topic `ticketflow.ticket.issued`, événement `TicketIssued` : `data` contient `ticketId`, `bookingId`, `catalogEventId`. Le code de contrôle d'un billet ne circule jamais dans un événement : il est réservé à l'API de ticket-service.

## Chaîne d'une réservation

```
booking-service  --ticketflow.booking.created-->     payment-service
payment-service  --ticketflow.payment.succeeded-->   booking-service
booking-service  --ticketflow.booking.confirmed-->   ticket-service, notification-service
ticket-service   --ticketflow.ticket.issued-->       notification-service
payment-service  --ticketflow.payment.failed-->      booking-service (annulation), notification-service
booking-service  --ticketflow.booking.cancelled-->   notification-service
```

## Adresses du broker

| Client | Adresse |
|---|---|
| Service lancé depuis IntelliJ (ta machine) | `localhost:9092` |
| Service ou commande dans Docker Compose | `kafka:19092` |

Conteneur : `kafka`, image `apache/kafka:4.0.0`.
