# TP6 — Apache Kafka en ligne de commande

**Module :** Architecture Microservices  
**Année universitaire :** 2026/2027  
**Sujet :** Manipulation d'un broker Kafka dans Docker

---

# Exercice 1 — Créer et décrire des topics

## 1. Création du premier topic

**Commande exécutée :**

```bash
./kafka-topics.sh --bootstrap-server $BS --create --topic ticketflow.payment.succeeded --partitions 3 --replication-factor 1
```

**Résultat obtenu :**

```text
Created topic ticketflow.payment.succeeded.
```

Le topic `ticketflow.payment.succeeded` a été créé avec succès.

## 2. Vérification de la liste des topics

**Commande exécutée :**

```bash
./kafka-topics.sh --bootstrap-server $BS --list
```

Le topic apparaît correctement dans la liste.

## 3. Description du topic

**Commande exécutée :**

```bash
./kafka-topics.sh --bootstrap-server $BS --describe --topic ticketflow.payment.succeeded
```

**Résultat obtenu :**

```text
Topic: ticketflow.payment.succeeded
PartitionCount: 3
ReplicationFactor: 1

Partition: 0 Leader: 1 Replicas: 1 Isr: 1
Partition: 1 Leader: 1 Replicas: 1 Isr: 1
Partition: 2 Leader: 1 Replicas: 1 Isr: 1
```

**Interprétation :**

- `PartitionCount: 3` : le topic possède trois partitions.
- `ReplicationFactor: 1` : chaque partition possède une seule réplique.
- `Leader: 1` : le broker numéro 1 est responsable de la partition.
- `Replicas: 1` : une seule copie de la partition existe.
- `Isr: 1` : la réplique est synchronisée.

## 4. Création des deux autres topics

Les deux autres topics ont été créés avec la même configuration.

**Création de `ticketflow.payment.failed` :**

```bash
./kafka-topics.sh --bootstrap-server $BS --create --topic ticketflow.payment.failed --partitions 3 --replication-factor 1
```

**Création de `ticketflow.booking.cancelled` :**

```bash
./kafka-topics.sh --bootstrap-server $BS --create --topic ticketflow.booking.cancelled --partitions 3 --replication-factor 1
```

Les deux topics ont été créés avec succès.

## 5. Vérification finale des topics

**Commande exécutée :**

```bash
./kafka-topics.sh --bootstrap-server $BS --list
```

**Résultat obtenu :**

```text
ticketflow.booking.cancelled
ticketflow.payment.failed
ticketflow.payment.succeeded
```

Les trois topics ont été créés avec succès.

## 6. Test du facteur de réplication 3

**Commande exécutée :**

```bash
./kafka-topics.sh --bootstrap-server $BS --create --topic test.rf3 --partitions 3 --replication-factor 3
```

**Résultat obtenu :**

```text
InvalidReplicationFactorException: Unable to replicate the partition 3 time(s): The target replication factor of 3 cannot be reached because only 1 broker(s) are registered.
```

**Interprétation :**

Kafka affiche une erreur car le facteur de réplication demandé est égal à 3, alors que notre environnement contient un seul broker.

Pour créer trois répliques d'une partition, il faut au minimum trois brokers différents.

## 7. Conclusion

Cet exercice m'a permis d'apprendre à créer, lister et décrire des topics Kafka en utilisant les commandes CLI.

J'ai également compris le fonctionnement des partitions, du leader et des répliques.

---

# Exercice 2 — Production et consommation des messages

## 1. Lancement du Producer

**Commande exécutée :**

```bash
./kafka-console-producer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded
```

Cette commande permet de lancer un producteur qui envoie des messages dans le topic `ticketflow.payment.succeeded`.

## 2. Envoi des messages

Trois messages JSON ont été envoyés :

```text
{"bookingId":1,"amount":300.00}
{"bookingId":2,"amount":150.00}
{"bookingId":3,"amount":90.00}
```

Chaque message représente une information liée à une réservation et à son montant.

## 3. Lancement du Consumer

**Commande exécutée :**

```bash
./kafka-console-consumer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --from-beginning
```

Cette commande permet de lire les messages du topic depuis le début.

Lors de la première lecture, quatre messages ont été affichés, dont une occurrence supplémentaire du message associé à `bookingId:1`.

Lors d'une deuxième lecture, les trois messages attendus ont été affichés :

```text
{"bookingId":1,"amount":300.00}
{"bookingId":2,"amount":150.00}
{"bookingId":3,"amount":90.00}
Processed a total of 3 messages
```

## 4. Test sans `--from-beginning`

**Commande exécutée :**

```bash
./kafka-console-consumer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded
```

**Résultat obtenu :**

```text
Processed a total of 0 messages
```

Aucun ancien message n'a été affiché.

## 5. Interprétation

L'option `--from-beginning` permet de lire les anciens messages depuis le début du topic.

Sans cette option, un nouveau consommateur sans offset enregistré commence par défaut à la fin du topic et attend les nouveaux messages.

La lecture des messages ne provoque pas leur suppression : Kafka les conserve selon sa politique de rétention.

## 6. Conclusion

Cet exercice m'a permis de comprendre le fonctionnement du Producer et du Consumer dans Apache Kafka.

Le Producer permet d'envoyer des messages dans un topic, tandis que le Consumer permet de les lire.

J'ai également compris que Kafka conserve les messages même après leur lecture.

---

# Exercice 3 — Clés, partitions et offsets

## 1. Envoi des messages avec des clés

Pour associer une clé à chaque message, j'ai utilisé la commande suivante :

```bash
./kafka-console-producer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --property parse.key=true --property key.separator=:
```

Les messages ont été envoyés avec les clés `booking-10`, `booking-11` et `booking-12`.

```text
booking-10:{"event":"PaymentSucceeded","bookingId":10}
booking-11:{"event":"PaymentSucceeded","bookingId":11}
booking-10:{"event":"BookingConfirmed","bookingId":10}
booking-12:{"event":"PaymentSucceeded","bookingId":12}
booking-11:{"event":"BookingConfirmed","bookingId":11}
booking-10:{"event":"TicketIssued","bookingId":10}
booking-12:{"event":"BookingConfirmed","bookingId":12}
booking-11:{"event":"TicketIssued","bookingId":11}
```

## 2. Lecture des messages avec affichage des partitions et des clés

**Commande exécutée :**

```bash
./kafka-console-consumer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --from-beginning --property print.partition=true --property print.offset=true --property print.key=true
```

Cette commande permet d'afficher la partition, l'offset, la clé et la valeur de chaque message.

## 3. Réponses aux questions

**Question 1 : Les messages ayant la même clé sont-ils dans la même partition ?**

Oui. Par exemple, tous les messages ayant la clé `booking-10` sont envoyés dans la partition 0.

**Question 2 : Dans quel ordre sont-ils enregistrés ?**

Les messages ayant la même clé sont enregistrés dans l'ordre de leur arrivée dans la partition.

Pour `booking-10`, les trois événements sont :

1. `PaymentSucceeded`
2. `BookingConfirmed`
3. `TicketIssued`

Les offsets augmentent dans l'ordre de leur enregistrement.

**Question 3 : Les messages sans clé sont-ils répartis dans les partitions ?**

Les messages envoyés sans clé ne bénéficient pas d'une garantie de répartition particulière. Leur emplacement dépend du comportement du producteur Kafka.

**Question 4 : Les messages de clés différentes sont-ils lus dans l'ordre où ils ont été envoyés ?**

Pas nécessairement. Les messages peuvent être enregistrés dans différentes partitions. Kafka garantit l'ordre à l'intérieur d'une partition, mais pas un ordre global entre plusieurs partitions.

## 4. Conclusion

Cet exercice m'a permis de comprendre l'importance des clés dans Kafka.

Une même clé permet de regrouper les messages d'une réservation dans la même partition et de conserver leur ordre de traitement.

Les offsets permettent d'identifier la position de chaque message dans sa partition.

Cette fonctionnalité est importante dans TicketFlow pour garantir l'ordre des événements associés à une même réservation.

---

# Exercice 4 — Groupes de consommateurs et rééquilibrage

## 1. Création du groupe de consommateurs

Deux consommateurs ont été lancés dans le même groupe `notification-service`, sur le topic `ticketflow.payment.succeeded`.

**Commande exécutée dans les terminaux T1 et T2 :**

```bash
./kafka-console-consumer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --group notification-service --from-beginning --property print.partition=true --property print.key=true
```

Les deux consommateurs appartiennent au même groupe et se partagent les partitions du topic.

## 2. Vérification de la répartition des partitions

**Commande exécutée dans le terminal T3 :**

```bash
./kafka-consumer-groups.sh --bootstrap-server $BS --describe --group notification-service
```

Le groupe possède trois partitions. Avec deux consommateurs actifs, la répartition observée était de deux partitions pour un consommateur et une partition pour l'autre.

Le lag était égal à 0 lorsque les messages avaient été consommés.

**Interprétation des colonnes :**

- `PARTITION` : numéro de la partition.
- `CURRENT-OFFSET` : position validée par le groupe.
- `LOG-END-OFFSET` : position de fin de la partition.
- `LAG` : nombre de messages restant à consommer.
- `CONSUMER-ID` : identifiant du consommateur affecté à la partition.

## 3. Envoi de messages avec des clés

Un producteur a été lancé avec la commande suivante :

```bash
./kafka-console-producer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --property parse.key=true --property key.separator=:
```

Les messages envoyés étaient :

```text
booking-20:{"event":"PaymentSucceeded"}
booking-21:{"event":"PaymentSucceeded"}
booking-22:{"event":"PaymentSucceeded"}
booking-20:{"event":"BookingConfirmed"}
booking-21:{"event":"BookingConfirmed"}
booking-22:{"event":"BookingConfirmed"}
```

**Résultat observé :**

- Les messages de `booking-20` et `booking-22` ont été reçus par T1, dans la partition 2.
- Les messages de `booking-21` ont été reçus par T2, dans la partition 0.

Chaque partition est affectée à un seul consommateur du groupe. Un même message n'est donc pas distribué simultanément aux deux consommateurs du même groupe.

## 4. Test du rééquilibrage (Rebalance)

Le consommateur T2 a été arrêté avec `Ctrl+C`.

Après quelques secondes, la commande suivante a été exécutée :

```bash
./kafka-consumer-groups.sh --bootstrap-server $BS --describe --group notification-service
```

Le consommateur restant a récupéré toutes les partitions du topic.

Après la publication de nouveaux messages, ceux-ci ont été reçus par T1.

Lorsque T2 a été relancé, Kafka a effectué un nouveau rééquilibrage pour répartir les partitions entre les deux consommateurs.

## 5. Test du retard (Lag)

Les deux consommateurs ont été arrêtés. Des messages ont ensuite été envoyés depuis le producteur.

La commande suivante a permis de vérifier le retard du groupe :

```bash
./kafka-consumer-groups.sh --bootstrap-server $BS --describe --group notification-service
```

**Résultat observé :**

Le groupe n'avait plus de membres actifs et les messages non consommés étaient indiqués par le lag.

Lors de cette manipulation, une commande saisie par erreur dans le terminal du producteur a également été enregistrée comme message. Le nombre de messages en attente était donc supérieur aux cinq messages prévus initialement.

Après le redémarrage d'un consommateur du groupe sans `--from-beginning`, les messages en attente ont été relus à partir du dernier offset validé.

Le lag est ensuite revenu à 0.

## 6. Création d'un deuxième groupe : analytics

Un consommateur appartenant au groupe `analytics` a été lancé :

```bash
./kafka-console-consumer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --group analytics --from-beginning --property print.partition=true
```

Ce groupe a pu lire les messages depuis le début du topic, indépendamment du groupe `notification-service`.

Pour afficher les groupes existants :

```bash
./kafka-consumer-groups.sh --bootstrap-server $BS --list
```

**Interprétation :**

Chaque groupe possède ses propres offsets. Ainsi, plusieurs groupes peuvent lire le même topic indépendamment les uns des autres.

## 7. Réinitialisation des offsets

Pour rejouer l'historique, les consommateurs ont été arrêtés avant la réinitialisation.

**Simulation sans modification :**

```bash
./kafka-consumer-groups.sh --bootstrap-server $BS --group notification-service --topic ticketflow.payment.succeeded --reset-offsets --to-earliest --dry-run
```

**Application de la réinitialisation :**

```bash
./kafka-consumer-groups.sh --bootstrap-server $BS --group notification-service --topic ticketflow.payment.succeeded --reset-offsets --to-earliest --execute
```

Après cette opération, le lag a augmenté car le groupe devait relire les anciens messages.

Un consommateur a ensuite été relancé et a relu l'historique.

Lors de notre vérification, le groupe `notification-service` affichait ensuite les offsets suivants :

```text
Partition 0 : CURRENT-OFFSET = 8, LOG-END-OFFSET = 8, LAG = 0
Partition 1 : CURRENT-OFFSET = 7, LOG-END-OFFSET = 7, LAG = 0
Partition 2 : CURRENT-OFFSET = 8, LOG-END-OFFSET = 8, LAG = 0
```

Le groupe n'avait plus de membre actif et le lag était égal à 0.

## 8. Test de quatre consommateurs

Le TP demande également de lancer quatre consommateurs dans le groupe `notification-service`.

**Commande à exécuter dans chaque terminal :**

```bash
./kafka-console-consumer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --group notification-service
```

**Principe attendu :**

Le topic possède trois partitions. Un groupe ne peut donc utiliser simultanément que trois consommateurs au maximum pour lire ces partitions.

Avec quatre consommateurs, un consommateur restera sans partition.

*Résultat de cette dernière vérification à compléter après l'exécution des quatre consommateurs.*

## 9. Conclusion

Cet exercice m'a permis de comprendre le fonctionnement des groupes de consommateurs, le partage des partitions et le rééquilibrage automatique.

J'ai également appris à surveiller le lag, à créer plusieurs groupes indépendants et à réinitialiser les offsets pour rejouer les messages.

---

# Exercice 5 — Rétention et configuration d'un topic

## 1. Consultation de la configuration du topic

La commande suivante permet de consulter la configuration du topic `ticketflow.payment.succeeded` :

```bash
./kafka-configs.sh --bootstrap-server $BS --entity-type topics --entity-name ticketflow.payment.succeeded --describe
```

Cette commande affiche les paramètres de configuration du topic.

## 2. Modification de la durée de rétention

Pour réduire la durée de rétention à une minute, la commande suivante est utilisée :

```bash
./kafka-configs.sh --bootstrap-server $BS --entity-type topics --entity-name ticketflow.payment.succeeded --alter --add-config retention.ms=60000
```

La valeur `60000` correspond à 60 000 millisecondes, soit une minute.

## 3. Vérification de la configuration

**Commande exécutée :**

```bash
./kafka-configs.sh --bootstrap-server $BS --entity-type topics --entity-name ticketflow.payment.succeeded --describe
```

**Résultat attendu :**

La configuration dynamique doit afficher :

```text
retention.ms=60000
```

Par défaut, la durée de rétention est de sept jours, soit :

```text
retention.ms=604800000
```

## 4. Rétablissement de la configuration par défaut

Pour supprimer la configuration personnalisée et revenir à la valeur par défaut :

```bash
./kafka-configs.sh --bootstrap-server $BS --entity-type topics --entity-name ticketflow.payment.succeeded --alter --delete-config retention.ms
```

## 5. Interprétation

La rétention détermine la durée pendant laquelle Kafka conserve les messages dans un topic.

La modification de `retention.ms` permet de personnaliser cette durée.

La suppression des messages n'est cependant pas immédiate après une minute. Kafka supprime les anciens segments fermés selon son mécanisme de nettoyage.

## 6. Optionnel — Création d'un topic compacté

Un topic compacté conserve le dernier état connu pour chaque clé.

**Commande de création :**

```bash
./kafka-topics.sh --bootstrap-server $BS --create --topic ticketflow.booking.status --partitions 1 --replication-factor 1 --config cleanup.policy=compact
```

Des changements d'état peuvent ensuite être publiés avec la clé `booking-30` :

```text
booking-30:PENDING
booking-30:CONFIRMED
booking-30:CANCELLED
```

Pour lire les messages :

```bash
./kafka-console-consumer.sh --bootstrap-server $BS --topic ticketflow.booking.status --from-beginning --property print.key=true
```

Les trois messages peuvent apparaître lors de la lecture initiale, car la compaction ne s'applique pas immédiatement. Elle intervient selon le mécanisme de nettoyage des segments.

## 7. Conclusion

Cet exercice permet de comprendre la configuration de la rétention et la politique de compaction dans Kafka.

La rétention contrôle la durée de conservation des messages, tandis que la compaction permet de conserver le dernier état associé à chaque clé.

---

# Exercice 6 — Les listeners et le réseau Docker

## 1. Présentation des listeners

Un listener est une adresse réseau utilisée par les clients pour se connecter au broker Kafka.

Dans notre environnement, les commandes internes utilisent l'adresse :

```text
kafka:19092
```

Cette adresse est utilisée par les conteneurs qui communiquent sur le réseau Docker.

Depuis la machine hôte, le broker est accessible avec :

```text
localhost:9092
```

## 2. Vérification depuis un autre conteneur Docker

La commande suivante doit être exécutée dans PowerShell ou CMD Windows, et non dans le conteneur Kafka :

```bash
docker run --rm --network kafka-tp apache/kafka:4.0.0 /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --list
```

Cette commande lance un conteneur temporaire sur le réseau `kafka-tp` et lui demande d'afficher les topics du broker.

**Résultat attendu :**

La liste des topics Kafka créés précédemment doit apparaître.

## 3. Différence entre l'adresse interne et l'adresse externe

Les adresses réseau doivent être adaptées à l'emplacement du client :

- Depuis un conteneur du réseau Docker : `kafka:19092`.
- Depuis la machine hôte : `localhost:9092`.

Un conteneur Docker ne doit pas utiliser automatiquement `localhost` pour joindre un autre conteneur, car `localhost` désigne le conteneur lui-même.

## 4. Interprétation

Les listeners permettent au broker Kafka de communiquer avec des clients situés dans différents environnements.

Une mauvaise adresse peut provoquer une erreur de connexion ou un timeout.

Il est donc important d'utiliser l'adresse correspondant au réseau du client.

## 5. Conclusion

Cet exercice m'a permis de comprendre la différence entre une adresse interne Docker et une adresse accessible depuis la machine hôte.

J'ai également compris l'importance de la configuration des listeners pour assurer la communication entre Kafka et les applications clientes.

---

# Exercice 7 — Simulation du fonctionnement de TicketFlow

## 1. Objectif

Cette simulation permet de représenter le fonctionnement du service `notification-service` dans l'architecture microservices TicketFlow.

Le service consomme les événements publiés dans le topic Kafka `ticketflow.payment.succeeded`.

Deux consommateurs appartenant au même groupe sont utilisés pour traiter les événements.

## 2. Lancement des consommateurs

Deux consommateurs ont été lancés dans deux terminaux différents, avec le même groupe `notification-service`.

**Commande exécutée dans T1 et T2 :**

```bash
./kafka-console-consumer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --group notification-service --from-beginning --property print.partition=true --property print.key=true
```

Les consommateurs se partagent les partitions du topic.

## 3. Publication des événements TicketFlow

Un producteur a été lancé dans un troisième terminal :

```bash
./kafka-console-producer.sh --bootstrap-server $BS --topic ticketflow.payment.succeeded --property parse.key=true --property key.separator=:
```

Les événements suivants ont été envoyés :

```text
booking-40:{"event":"PaymentSucceeded"}
booking-41:{"event":"PaymentSucceeded"}
booking-42:{"event":"PaymentSucceeded"}
booking-40:{"event":"BookingConfirmed"}
booking-41:{"event":"BookingConfirmed"}
booking-42:{"event":"BookingConfirmed"}
booking-40:{"event":"TicketIssued"}
booking-41:{"event":"TicketIssued"}
booking-42:{"event":"TicketIssued"}
```

Chaque clé représente une réservation.

Les messages ayant la même clé sont envoyés dans la même partition, ce qui permet de conserver leur ordre.

## 4. Arrêt d'un consommateur

Le consommateur T1 ou T2 est arrêté avec `Ctrl+C`.

Kafka détecte le départ du consommateur et effectue un rééquilibrage du groupe.

Les partitions sont alors réaffectées au consommateur restant.

Les nouveaux messages peuvent continuer à être consommés par ce dernier.

## 5. Vérification du groupe

La commande suivante permet de vérifier l'état du groupe :

```bash
./kafka-consumer-groups.sh --bootstrap-server $BS --group notification-service --describe
```

Elle permet de consulter les partitions attribuées, les offsets et le lag.

## 6. Interprétation

Cette simulation représente le fonctionnement d'un service de notification dans une architecture microservices.

Lorsqu'un paiement est effectué, un événement est publié dans Kafka.

Le service `notification-service` consomme cet événement et peut ensuite déclencher les traitements nécessaires.

Kafka permet de répartir les messages entre plusieurs consommateurs, de gérer les départs et les arrivées des consommateurs et de reprendre la lecture à partir des offsets validés.

## 7. Conclusion

Cette simulation m'a permis de comprendre comment Kafka facilite la communication asynchrone entre les microservices.

Dans TicketFlow, le service de paiement peut publier des événements sans attendre que le service de notification termine son traitement.

Le service de notification peut consommer ces événements indépendamment, tout en bénéficiant du mécanisme de groupes de consommateurs et de reprise après interruption.

---

# Conclusion générale du TP

Ce TP m'a permis de découvrir et de manipuler Apache Kafka en ligne de commande dans un environnement Docker.

J'ai appris à :

- Créer et décrire des topics.
- Envoyer et recevoir des messages avec un Producer et un Consumer.
- Utiliser les clés, les partitions et les offsets.
- Comprendre le fonctionnement des groupes de consommateurs.
- Observer le rééquilibrage automatique des partitions.
- Surveiller le lag et réinitialiser les offsets.
- Configurer la rétention et découvrir la compaction.
- Comprendre les listeners et la communication réseau avec Docker.
- Simuler la communication entre les microservices de TicketFlow.

Ce TP m'a permis de mieux comprendre le rôle de Kafka dans une architecture microservices, notamment pour assurer une communication asynchrone, fiable et découplée entre les
