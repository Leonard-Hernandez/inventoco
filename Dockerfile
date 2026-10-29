FROM amazoncorretto:25-alpine AS builder

WORKDIR /app/inventoco

COPY gradle ./gradle
COPY gradlew .
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY ./src ./src

RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S inventoco && adduser -S -G inventoco -H -s /sbin/nologin inventoco

WORKDIR /app

COPY --from=builder --chown=inventoco:inventoco /app/inventoco/build/libs/app.jar .

USER inventoco

EXPOSE 8095

ENTRYPOINT [ "java", "-jar", "app.jar" ]
