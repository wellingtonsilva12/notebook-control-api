# Etapa 1: build com Maven + JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Etapa 2: imagem final, só com o JRE (mais leve)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/api.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
