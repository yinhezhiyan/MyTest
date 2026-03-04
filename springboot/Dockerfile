FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY springboot/pom.xml springboot/pom.xml
COPY springboot/src springboot/src
RUN mvn -f springboot/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/springboot/target/*.jar /app/app.jar
COPY data /app/data
COPY files /app/files
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
