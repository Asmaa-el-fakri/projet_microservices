# TicketFlow – Projet Microservices

Projet de la matière Architecture microservices (2026/2027) : plateforme de billetterie
en Spring Boot / Spring Cloud, décrite dans le cahier des charges TicketFlow.

## Membres de l'équipe

- El Fakri Asmaa
- Faten Karkouri
- Nada Chirifi
- Aya Moufti
- Saad Ouguoujil

## Services

| Service | Port | Rôle |
|---|---|---|
| config-server | 8888 | Configuration centralisée, lue depuis le dépôt GitHub `ticketflow-config` |
| discovery-serveur | 8761 | Registre Eureka |
| gateway-api | 8080 | Point d'entrée, routes explicites via `lb://` |
| customer-service | 8081 | Comptes et profils clients |
| event-service | 8082 | Catalogue d'événements |

## Branches

- `main` : version du projet
- `develop` : intégration du travail en cours
- Branches des travaux pratiques :
    - `feature/step-01-customer-service` : customer-service
    - `step-02-gateway` : API Gateway
    - `step-03-discovery` : serveur Eureka
    - `step-04-config` : Config Server et configuration centralisée via GitHub

## Lancer le projet

Démarrer dans cet ordre :

1. config-server, avec la variable d'environnement
   `CONFIG_GIT_URI=https://github.com/Asmaa-el-fakri/ticketflow-config.git`
2. discovery-serveur
3. customer-service
4. gateway-api
  - `step-05-openfeign` : TP4, communication synchrone booking-service → event-service avec OpenFeign
Vérifier ensuite `http://localhost:8761` (services UP), puis `http://localhost:8080/api/customers`.
