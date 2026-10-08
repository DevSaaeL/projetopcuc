FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app/backend
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
COPY frontend /app/frontend
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend/target/helpdesk-ti-1.0.0.jar ./app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
