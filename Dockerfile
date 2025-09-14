# Étape 1 : Build avec Maven
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /app

# Copier uniquement pom.xml et télécharger les dépendances
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Copier le code source et builder le projet
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# Étape 2 : Exécution du JAR
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copier le JAR généré depuis l’étape build
COPY --from=build /app/target/club-taekwondo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar", "--server.port=${PORT}"]
