# TicketFlow – État du projet et reprise par le groupe

Ce document permet à une autre personne du groupe de reprendre le travail sans rien demander. Il complète `docs/PARTIE-A.md` (API et rôles de la partie A) et `docs/EVENTS.md` (contrat des événements Kafka).

Les points marqués **à vérifier** n'ont pas été confirmés au moment de l'écriture : lancer la commande indiquée avant de s'appuyer dessus.

## 1. Ce qui est fait

| Domaine | État |
|---|---|
| Catalogue et gestion des événements (event-service) | fait, testé, dans `develop` |
| Comptes, profil, rôles (customer-service, Keycloak) | fait, testé, dans `develop` |
| Sécurité JWT au Gateway **et** dans chaque service | fait, dans `develop` |
| Interface Angular (catalogue, connexion, profil, console organisateur, accessibilité) | fait, dans `develop` |
| PostgreSQL + Flyway pour event, customer, payment | fait |
| Docker Compose : PostgreSQL, Keycloak, Kafka, event-service, customer-service, site Angular (nginx) | fait |
| payment-service : paiement simulé, états, propriétaire vérifié, tests | fait, dans `develop` |
| Kafka aligné sur les fiches du cours (broker 4.0.0, topics `ticketflow.*`, message `data`) et événements `PaymentSucceeded` / `PaymentFailed` | code sur `feature/kafka-align` ; fusion dans `develop` **à vérifier** |
| Contrat des événements (`docs/EVENTS.md`) | à jour avec les fiches ; fusion **à vérifier** |
| Journal d'audit (FR-020) et demandes de fermeture visibles pour l'admin (customer-service) | code sur `feature/admin` ; build vert et fusion **à vérifier** |
| Pages Angular d'administration (clients, journal d'audit) | **pas commencées** |
| TP4 (OpenFeign) et TP5 (Circuit Breaker) dans booking-service | rendus, vérifiés de nouveau (section 8) |
| booking-service au-delà des TP | squelette, voir section 6 |
| ticket-service, notification-service, assistant IA, serveur MCP | **n'existent pas** |

Vérifier l'état réel du dépôt :

```cmd
git fetch projet
git branch -r
git log --oneline -15 projet/develop
```

## 2. Architecture et ports

| Composant | Port | Où il tourne aujourd'hui |
|---|---|---|
| ticketflow-web (Angular, nginx) | 4200 | Docker Compose |
| gateway-api | 8080 | IntelliJ |
| customer-service | 8081 | Docker Compose |
| event-service | 8082 | Docker Compose |
| booking-service | 8083 | IntelliJ |
| payment-service | 8084 | IntelliJ (pas encore de Dockerfile) |
| notification-service (prévu par la fiche Kafka) | 8085 | n'existe pas encore |
| Eureka (discovery-serveur) | 8761 | IntelliJ |
| Config Server | 8888 | IntelliJ |
| Keycloak | 8180 | Docker Compose |
| PostgreSQL | 5432 | Docker Compose (bases `event_db`, `customer_db`, `payment_db`) |
| Kafka (conteneur `kafka`) | 9092 depuis la machine, 19092 entre conteneurs | Docker Compose |

Règles de l'architecture (cahier des charges) :

- le navigateur n'appelle que le Gateway ;
- chaque service valide lui-même le jeton, sans faire confiance au seul Gateway ;
- une base par service, aucune jointure entre bases ;
- appel synchrone (OpenFeign) quand la réponse est nécessaire tout de suite, Kafka pour annoncer un fait.

## 3. Lancer l'ensemble

Prérequis : Docker Desktop, JDK 21 ou plus, IntelliJ. Dans cmd, pour le Maven de la racine : `set JAVA_HOME=<ton JDK>` puis `set PATH=%JAVA_HOME%\bin;%PATH%`.

Ordre de démarrage :

1. Infrastructure (cmd, dossier `infra`) : `docker compose up -d postgres keycloak kafka`. Attendre 30 secondes.
2. IntelliJ : `ConfigServerApplication`, puis `DiscoveryServeurApplication`.
3. Services dans Docker (cmd, dossier `infra`) : `docker compose up -d --build event-service customer-service web`.
4. IntelliJ : `PaymentServiceApplication`, `BookingServiceApplication`, puis `GatewayApiApplication`. Pour chaque service qui utilise PostgreSQL, définir la variable d'environnement `SPRING_DATASOURCE_PASSWORD=ticketflow_dev` (mot de passe de démonstration) dans Edit Configurations.
5. Attendre environ 2 minutes que les services s'enregistrent dans Eureka, puis ouvrir http://localhost:4200.

Comptes de démonstration (fictifs, importés avec le realm Keycloak) : `client1 / client123`, `organizer1 / organizer123`, `organizer2 / organizer223`, `admin1 / admin123`.

Obtenir un jeton pour tester l'API (Git Bash) :

```bash
G=http://localhost:8080
token() { curl -s -X POST http://localhost:8180/realms/ticketflow/protocol/openid-connect/token -d grant_type=password -d client_id=ticketflow-web -d username=$1 -d password=$2 | sed -E 's/.*"access_token":"([^"]+)".*/\1/'; }
J='Content-Type: application/json'
CLIENT=$(token client1 client123)
```

Les raccourcis disparaissent à chaque nouvelle fenêtre Git Bash : les redéfinir.

Build et tests de tout le projet (cmd, racine) : `mvnw.cmd -B -ntp verify`. C'est ce que lance la CI de GitHub sur chaque pull request.

## 4. Configuration (dépôt GitHub `ticketflow-config`)

Toute la configuration est dans ce dépôt, lu par le Config Server. Les fichiers locaux gardent seulement le nom du service et l'adresse du Config Server.

| Fichier | Contenu |
|---|---|
| `application.yml` | commun : Eureka, actuator, `issuer-uri` Keycloak |
| `<service>.yml` | port, base de données, Flyway ; pour payment : Kafka (`localhost:9092`, `acks: all`, noms des topics) et options de simulation |
| `<service>-docker.yml` | utilisé avec le profil `docker` : adresses `postgres:5432`, `keycloak:8180`, `kafka:19092`, Eureka via `host.docker.internal` |
| `gateway-api.yml` | routes (customers, events, bookings, payments) et CORS |

Existent aujourd'hui : `application.yml`, `booking-service.yml`, `customer-service(.yml, -docker.yml)`, `event-service(.yml, -docker.yml)`, `gateway-api.yml`, `payment-service(.yml, -docker.yml)`.

Règles :

- aucun secret réel dans un dépôt : le mot de passe de la base vient d'une variable d'environnement ;
- une clé YAML en double (par exemple deux blocs `spring:`) invalide tout le fichier, et le Config Server répond `500` à *tous* les services qui le lisent ;
- après un changement sur GitHub, redémarrer le service concerné (le Gateway lit ses routes au démarrage) ;
- tester un fichier : `curl http://localhost:8888/<service>/default/main` doit répondre `200`.

## 5. Contrats entre services

- **Événements Kafka** : voir `docs/EVENTS.md`. Topics `ticketflow.payment.succeeded`, `ticketflow.payment.failed` (implémentés), puis `ticketflow.booking.created`, `ticketflow.booking.confirmed`, `ticketflow.booking.cancelled`, `ticketflow.ticket.issued` (proposés) et `ticketflow.notification.dlt`. Message JSON texte avec `eventId`, `eventType`, `version`, `occurredAt`, `data` (plus `aggregateId` et `correlationId`). Clé = identifiant de la réservation, 3 partitions, consommateurs idempotents.
- **Écart avec la fiche du cours** : `data.customerRef` (le `sub` Keycloak) à la place de `data.customerId`, car payment-service ne connaît pas le `customerId`. À remplacer quand booking publiera `BookingCreated`.
- **API de payment-service** : `POST /api/payments` (`bookingId`, `amount`, `currency`), `POST /api/payments/{id}/simulate` (`SUCCESS`, `FAILURE` ou `CANCEL`), `GET /api/payments/{id}`. Réservé au rôle `CLIENT`, seul le propriétaire voit son paiement (`404` sinon), un paiement terminé ne change plus (`409`).
- **API de event-service** : `GET /api/events/{id}` reste **public** (booking-service l'appelle par OpenFeign et attend `id`, `title`, `summary`, `eventDate`, `city`, `venue`, `category`, `price`, `capacity`, `status`). Ne pas le casser.

## 6. Ce qui reste, par partie

### booking-service (partie B) : la pièce qui bloque le reste

État actuel : il crée une réservation `PENDING_PAYMENT` après avoir vérifié l'événement (Feign + circuit breaker, voir TP4 et TP5). Il manque :

- le **prix n'est jamais enregistré** (`unitPrice` et `totalPrice` restent vides) ;
- aucune **transition d'état** : rien ne passe en `CONFIRMED`, `CANCELLED` ou `EXPIRED` ;
- aucun contrôle de capacité atomique, aucune expiration, aucune idempotence (FR-007, FR-008) ;
- `customerId` est envoyé par l'appelant et `GET /api/bookings?customerId=` lit les réservations de n'importe qui : contraire à la section 7.2 et à FR-013. L'identité doit venir du jeton ;
- pas de sécurité, base H2 (données perdues), pas de Flyway, pas de Dockerfile, pas de Kafka.

À faire : enregistrer le prix, publier `BookingCreated`, `BookingConfirmed` et `BookingCancelled`, consommer `PaymentSucceeded` et `PaymentFailed`, sécuriser, migrer vers PostgreSQL + Flyway. **Sans casser les TP rendus** (section 8).

### Services manquants

| Service | Rôle | Dépend de |
|---|---|---|
| ticket-service | un billet unique par place, code non prédictible sans donnée personnelle (FR-011) | `ticketflow.booking.confirmed` |
| notification-service (port 8085 sur la fiche) | e-mails et messages, reprises, déduplication par `eventId`, topic d'échec `ticketflow.notification.dlt` (FR-014) | événements Kafka |
| assistant IA + serveur MCP | questions publiques et personnelles, outils allowlistés (FR-015 à FR-019) | API de event et booking |

Un nouveau service suit le modèle de `payment-service` : module Maven, `application.yaml` (import du Config Server), `SecurityConfig` avec `/actuator/health` public, migration Flyway, base `<nom>_db` dans `infra/postgres/init/01-create-databases.sql`, fichiers `<service>.yml` et `<service>-docker.yml` sur GitHub, route dans `gateway-api.yml`. Pour Kafka, suivre les fiches du cours (`spring-boot-starter-kafka`, `@KafkaListener`, `DefaultErrorHandler` avec `DeadLetterPublishingRecoverer`).

### Partie A : fin de travail

- pages Angular d'administration (liste des clients avec filtre des demandes de fermeture, journal d'audit), réservées à `ADMIN` ;
- fusionner `feature/admin` après build vert ;
- captures d'écran de la section 8 de `docs/PARTIE-A.md`.

### Transversal

- Dockerfile de payment-service, puis Config Server, Eureka, Gateway et booking dans Docker Compose pour tout lancer d'une seule commande ;
- description OpenAPI des API (section 7.2 du cahier des charges) ;
- préfixe d'API versionné (`/api/v1`) : à trancher ensemble, les services actuels utilisent `/api/...`.

## 7. Décisions à prendre ensemble

1. **Création du paiement** : booking appelle payment (REST, appels entre services à authentifier), ou booking publie `BookingCreated` et payment crée lui-même le paiement (recommandé : le montant vient de booking, pas du client).
2. **Qui modifie booking-service** : la partie B, ou une autre personne sur une branche à part (éviter les conflits Git).
3. **Authentification des appels OpenFeign** de booking vers event : sans objet tant que `GET /api/events/{id}` est public ; à revoir si on le protège.
4. **Préfixe `/api/v1`** : l'ajouter partout ou le laisser de côté.
5. **`customerId` ou `customerRef`** dans les événements de paiement (voir section 5).

## 8. Travaux pratiques rendus (à ne pas casser)

Le professeur évalue les TP à partir des étiquettes Git `tp1-rendu` à `tp5-rendu` et du code de `booking-service`.

- **TP4 (OpenFeign)** : `EventClient` appelle `GET /api/events/{id}` par le nom Eureka `event-service`, délais de 2 s (connexion) et 3 s (lecture). Un `404` reste un `404`.
- **TP5 (Circuit Breaker)** : instance `EventClientgetByIdLong`, fenêtre de 10 appels, minimum 5, seuil 50 %, 10 s en `OPEN`, 3 appels en `HALF_OPEN`, les `404` et `400` ignorés ; une panne ou un circuit ouvert donne `503`, jamais une réservation fictive.
- Vérifié de nouveau : `git diff --stat tp5-rendu develop -- services/booking-service` ne montre aucun changement.
- Mesure du TP5 : avec event-service arrêté, les dix appels répondent `503` ; le circuit s'ouvre au 5e échec (`notPermittedCalls` à 5 ensuite), puis se referme après la reprise. Le tableau du sujet annonce l'ouverture « au 4e échec » alors que `minimum-number-of-calls` vaut 5 : c'est le sujet qui est inexact sur ce point.
- Avant de modifier `booking-service`, relancer l'exercice du TP5 (cinq `404`, un `201`, dix `503`, lecture de `/actuator/circuitbreakers`, reprise) et `mvnw.cmd -B -ntp -pl services/booking-service -am verify`.

## 9. Pièges rencontrés (à éviter)

- **Fichier collé mais pas enregistré** : un fichier vide compile sans erreur et casse plus tard (Flyway applique une migration vide, la sécurité par défaut protège `/actuator/health`). Après avoir collé du code, faire **Ctrl+S**, puis contrôler (cmd, racine) :
  `for /r services\<service>\src %f in (*) do @if %~zf==0 echo VIDE: %f`
- **Fichier créé dans le mauvais module** : une migration d'un service créée dans un autre service. Vérifier avec `dir /s /b services\*<nom du fichier>*`.
- **Fichier de test dans `src/main`** : les bibliothèques de test sont absentes, la compilation échoue. Les tests vont dans `src/test/java`.
- **Deux fichiers `application.yaml`** : celui de `src/main/resources` (import du Config Server) et celui de `src/test/resources` (`spring.cloud.config.enabled: false`, utilisé par les tests). Ne pas les confondre.
- **Adresse du Config Server dans Docker** : une variable `SPRING_CONFIG_IMPORT` ne remplace pas l'import du fichier ; utiliser `spring.config.import: "configserver:${CONFIG_SERVER_URL:http://localhost:8888}"` et passer `CONFIG_SERVER_URL` au conteneur.
- **Modifier un conteneur** : `docker compose up -d --build --force-recreate <service>` (dans `infra`) ; sans `--force-recreate`, l'ancien conteneur est conservé.
- **Eureka garde l'ancienne instance** environ 90 secondes après un redémarrage : un `503` du Gateway juste après un redémarrage est normal.
- **Kafka** : `localhost:9092` depuis IntelliJ, `kafka:19092` depuis un conteneur ; un conteneur qui utilise `localhost:9092` se connecte à lui-même et échoue. Commandes dans le conteneur : `docker exec kafka /opt/kafka/bin/<script>.sh --bootstrap-server kafka:19092 ...`.
- **Flyway** : ne jamais modifier une migration déjà appliquée (erreur de signature). Ajouter une nouvelle version. En développement, on peut recréer une base : `DROP DATABASE` puis `CREATE DATABASE` (le service doit être arrêté).
- **Comptes Keycloak créés par inscription** : perdus quand le conteneur est supprimé (`docker compose down`). Seuls les comptes de démonstration du realm sont recréés. Ne pas utiliser `-v` pour garder les données PostgreSQL.
- **Git** : toujours « Create a merge commit » pour les pull requests (jamais squash ni rebase, sinon les branches empilées entrent en conflit). Après chaque fusion : `git checkout develop` puis `git pull projet develop`.
- **Maven** : `mvnw.cmd` est à la racine du projet, pas dans `infra` ni dans un service.
- **Dépendances Spring Boot 4** : plusieurs starters ont été renommés. Vérifier le nom exact avant de modifier un `pom.xml` (par exemple `spring-boot-starter-security-oauth2-resource-server`, `spring-boot-starter-flyway`, `spring-boot-starter-kafka`).

## 10. Avant de reprendre

1. `git checkout develop`, `git pull projet develop`, `git log --oneline -10`.
2. Lancer `mvnw.cmd -B -ntp verify` : tout doit être en `BUILD SUCCESS`.
3. Dérouler le scénario de démonstration de `docs/PARTIE-A.md` (section 6).
4. Tester le paiement (section 5) avec un jeton `client1` : créer un paiement, le simuler, lire l'événement dans Kafka :
   `docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:19092 --topic ticketflow.payment.succeeded --from-beginning --max-messages 1 --timeout-ms 15000 --property print.key=true`
5. Choisir, avec le groupe, les décisions de la section 7, puis répartir la section 6.
