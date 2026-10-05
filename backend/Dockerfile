FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY . .
RUN chmod +x mvnw && ./mvnw -B -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/hardware-store-0.0.1-SNAPSHOT.jar /app/app.jar
RUN mkdir -p /app/uploads && chown 10001:10001 /app/uploads
USER 10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
