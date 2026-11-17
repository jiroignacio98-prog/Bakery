FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV PORT=3000
COPY --from=build /workspace/target/sweet-bel-erina-1.0.0.jar /app/app.jar
EXPOSE 3000
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75 -jar /app/app.jar --server.port=${PORT:-3000}"]
