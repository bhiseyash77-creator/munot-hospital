# Multi-stage build. NOTE: not tested in the generation environment - verify with `docker build .`
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 munot && mkdir -p /app/uploads && chown munot /app/uploads
COPY --from=build /app/target/hospital-1.0.0.jar app.jar
USER munot
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
