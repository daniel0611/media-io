FROM gradle:4.10.1-jdk8-alpine as build
USER root

WORKDIR /build
COPY build.gradle /build/
COPY settings.gradle /build/
RUN mkdir /cache

# Create a Scala file to download all compile dependencies
RUN mkdir /build/src/ && mkdir /build/src/main && \
    mkdir /build/src/main/java && echo "object Main{}" > /build/src/main/java/Main.scala

# download deps to be cached with the Docker cache
RUN gradle classes compileScala --no-daemon -g /cache --debug

COPY src /build/src

# actual compiling
RUN gradle build jar --no-daemon --parallel -g /cache
RUN cp /build/build/libs/*.jar /build/app.jar



FROM openjdk:8-jre-alpine
WORKDIR /
COPY --from=build /build/app.jar /app.jar
COPY ./src/main/webapp /src/main/webapp
RUN mkdir /data

ENTRYPOINT [ "java", "-jar", "/app.jar" ]
