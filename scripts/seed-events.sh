#!/usr/bin/env bash
# Cree et publie des evenements de test via le Gateway (compte organisateur de demonstration).
API=${API:-http://localhost:8080/api/events}
KEYCLOAK=${KEYCLOAK:-http://localhost:8180}

TOKEN=$(curl -s -X POST "$KEYCLOAK/realms/ticketflow/protocol/openid-connect/token" \
  -d grant_type=password -d client_id=ticketflow-web \
  -d username=organizer1 -d password=organizer123 \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')

create_and_publish() {
  local out code body id
  out=$(curl -s -w $'\n%{http_code}' -X POST "$API" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" --data "$1")
  code=${out##*$'\n'}
  body=${out%$'\n'*}
  if [[ "$code" == "201" ]]; then
    id=$(echo "$body" | sed -E 's/.*"id":([0-9]+).*/\1/')
    curl -s -o /dev/null -X POST "$API/$id/publish" -H "Authorization: Bearer $TOKEN"
    echo "Evenement $id cree et publie"
  else
    echo "Echec (HTTP $code) : $body"
  fi
}

create_and_publish '{"title":"Conference Java","summary":"Java et Spring Boot","eventDate":"2027-07-25T10:00:00Z","city":"Casablanca","venue":"EMSI","category":"Formation","price":0,"capacity":200}'
create_and_publish '{"title":"Festival Jazz","summary":"Soiree jazz en plein air","eventDate":"2027-07-01T19:00:00Z","city":"Casablanca","venue":"Theatre Mohammed V","category":"Musique","price":150,"capacity":500}'
create_and_publish '{"title":"Concert Rock","summary":"Rock en plein air","eventDate":"2027-06-15T20:00:00Z","city":"Rabat","venue":"Stade Prince Moulay Abdellah","category":"Musique","price":45.99,"capacity":1000}'
create_and_publish '{"title":"Match de foot","summary":"Derby de la ville","eventDate":"2027-08-10T18:00:00Z","city":"Casablanca","venue":"Stade","category":"Sport","price":80,"capacity":3000}'
create_and_publish '{"title":"Atelier Angular","summary":"Introduction a Angular","eventDate":"2027-09-05T09:00:00Z","city":"Marrakech","venue":"Technopark","category":"Formation","price":20,"capacity":60}'
create_and_publish '{"title":"Marathon de la ville","summary":"Course de 10 km","eventDate":"2027-10-12T07:00:00Z","city":"Rabat","venue":"Centre ville","category":"Sport","price":30,"capacity":2000}'
create_and_publish '{"title":"Stand-up Comedy","summary":"Soiree humour","eventDate":"2027-11-20T21:00:00Z","city":"Tanger","venue":"Salle Cervantes","category":"Spectacle","price":100,"capacity":300}'
create_and_publish '{"title":"Salon de l emploi","summary":"Rencontres entreprises","eventDate":"2027-12-02T10:00:00Z","city":"Casablanca","venue":"Foire internationale","category":"Salon","price":0,"capacity":5000}'