FROM gradle:4.10.1-jdk8-alpine as build
USER root

WORKDIR /build
COPY . /build

RUN gradle build jar --no-daemon
RUN cp /build/build/libs/*.jar  /build/app.jar

FROM openjdk:8-jre-alpine

WORKDIR /
COPY --from=build /build/app.jar /app.jar
COPY ./src/main/webapp /src/main/webapp
RUN mkdir /data

ENTRYPOINT [ "java", "-jar", "/app.jar" ]
