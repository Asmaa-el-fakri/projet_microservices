# Build context = RACINE du dépôt (le module dépend du pom parent) :
#   docker build -f services/notification-service/Dockerfile -t ticketflow/notification-service .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -q -pl services/notification-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/services/notification-service/target/notification-service-*.jar app.jar
USER app
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
