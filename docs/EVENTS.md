# TicketFlow – Contrat des événements Kafka

Ce document décrit les messages échangés entre services sur Kafka. Il sert de contrat : un producteur ne change pas un champ existant sans prévenir les consommateurs.

Statuts : **implémenté** (déjà publié par le code), **proposé** (à valider avec la partie B ou C avant de coder).

## Règles communes

- Un message est un objet JSON, sans en-tête de type Java : le consommateur ne dépend pas des classes du producteur.
- Clé du message : l'identifiant de la réservation (`bookingId`, en texte). Tous les événements d'une même réservation arrivent dans l'ordre sur la même partition.
- Livraison « au moins une fois » : un message peut arriver deux fois. **Tout consommateur est idempotent** : il mémorise les `eventId` déjà traités et ignore les doublons.
- Un échec de Kafka ou d'un consommateur n'annule jamais une réservation ni un paiement (cahier des charges, section 3.8).
- Évolution du schéma : on ajoute des champs, on n'en retire pas et on ne change pas leur sens. Un changement incompatible incrémente `version`.
- Données personnelles minimisées : jamais d'e-mail, de nom ni de téléphone dans un événement. Un identifiant opaque (le `sub` Keycloak) n'est transmis que si un consommateur en a besoin.

## Enveloppe commune

| Champ | Type | Description |
|---|---|---|
| `eventId` | texte (UUID) | identifiant unique du message, sert à dédoublonner |
| `type` | texte | nom de l'événement (`PaymentSucceeded`, …) |
| `version` | entier | version du schéma, `1` aujourd'hui |
| `occurredAt` | date UTC (ISO 8601) | moment du changement d'état |
| `aggregateId` | texte | identifiant de l'objet concerné par l'événement |
| `correlationId` | texte | `booking-<id>` : relie tous les événements d'une réservation |

## Topic `payments.events` – producteur : payment-service

Clé : `bookingId`. Statut : **implémenté**.

| Événement | Quand | Champs en plus de l'enveloppe |
|---|---|---|
| `PaymentSucceeded` | le paiement passe en `SUCCEEDED` | `paymentId`, `bookingId`, `amount`, `currency`, `reason` (vide) |
| `PaymentFailed` | le paiement passe en `FAILED` ou `CANCELLED` | `paymentId`, `bookingId`, `amount`, `currency`, `reason` : `FAILURE` ou `CANCELLED` |

Exemple :

```json
{
  "eventId": "ac0a0b4d-6757-4d24-8e0a-05cd19826e5d",
  "type": "PaymentSucceeded",
  "version": 1,
  "occurredAt": "2026-11-10T08:08:16.050504500Z",
  "aggregateId": "3",
  "correlationId": "booking-9",
  "paymentId": 3,
  "bookingId": 9,
  "amount": 150.00,
  "currency": "MAD",
  "reason": null
}
```

Le message est publié **après** l'enregistrement du paiement en base. Si Kafka est arrêté à cet instant, le paiement reste valide et l'événement est perdu (limite connue : pas de « transactional outbox »).

## Topic `bookings.events` – producteur : booking-service

Clé : `bookingId`. Statut : **proposé** (booking-service ne publie encore rien).

| Événement | Quand | Champs en plus de l'enveloppe |
|---|---|---|
| `BookingCreated` | la réservation est créée en attente de paiement | `bookingId`, `catalogEventId`, `quantity`, `unitPrice`, `totalPrice`, `currency`, `customerRef` |
| `BookingConfirmed` | le paiement a réussi et la réservation est confirmée | `bookingId`, `catalogEventId`, `quantity`, `customerRef` |
| `BookingCancelled` | la réservation est annulée (paiement échoué ou annulé) | `bookingId`, `reason` |
| `BookingExpired` | le délai de paiement est dépassé | `bookingId` |

Précisions :

- `catalogEventId` est l'identifiant de l'événement du catalogue (event-service). Le nom `eventId` est déjà pris par l'enveloppe.
- `customerRef` est le `sub` du jeton Keycloak du client, jamais fourni par l'appelant.
- Le prix est figé à la création de la réservation (`unitPrice`, `totalPrice`), à partir d'event-service.
- Avec `BookingCreated`, payment-service peut créer lui-même le paiement `PENDING` : le montant et le propriétaire viennent de booking, pas du client.

## Topic `tickets.events` – producteur : ticket-service

Clé : `bookingId`. Statut : **proposé**.

| Événement | Quand | Champs en plus de l'enveloppe |
|---|---|---|
| `TicketIssued` | un billet est émis pour une réservation confirmée | `ticketId`, `bookingId`, `catalogEventId` |

Le code de contrôle d'un billet ne circule jamais dans un événement : il est réservé à l'API de ticket-service.

## Chaîne d'une réservation

```
booking-service   --BookingCreated-->    payment-service
payment-service   --PaymentSucceeded-->  booking-service
booking-service   --BookingConfirmed-->  ticket-service, notification-service
ticket-service    --TicketIssued-->      notification-service
payment-service   --PaymentFailed-->     booking-service (annulation), notification-service
```

## Adresses du broker

| Client | Adresse |
|---|---|
| Service lancé depuis IntelliJ | `localhost:9094` |
| Service dans Docker Compose | `kafka:9092` |
