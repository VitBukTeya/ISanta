FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew --no-daemon clean installDist

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/install/ISanta /app

CMD ["/app/bin/ISanta"]

VOLUME ["/app/state"]