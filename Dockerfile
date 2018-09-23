FROM gradle:4.10.1-jdk8-alpine as build
USER root

WORKDIR /build
COPY build.gradle /build/
COPY settings.gradle /build/
RUN mkdir /cache

RUN gradle classes --no-daemon -g /cache

COPY src /build/src

RUN gradle build jar --no-daemon --parallel -g /cache
RUN cp /build/build/libs/*.jar /build/app.jar



FROM openjdk:8-jre-alpine
WORKDIR /
COPY --from=build /build/app.jar /app.jar
RUN mkdir /data

ENTRYPOINT [ "java", "-jar", "/app.jar" ]
