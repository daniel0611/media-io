FROM openjdk:8-jdk as build

# sbt
ENV SBT_VERSION=1.2.6
RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion

# get dependencies
RUN mkdir /build
WORKDIR /build
COPY build.sbt /build
COPY project /build/project
RUN ls
RUN ls project
RUN sbt update

# compile
COPY . /build
RUN sbt assembly
RUN cp /build/target/scala-2.12/MovieDownloader.jar /build/app.jar


FROM openjdk:8-jre-alpine

WORKDIR /
COPY --from=build /build/app.jar /app.jar
COPY ./src/main/webapp /src/main/webapp
RUN mkdir /data

ENTRYPOINT [ "java", "-jar", "/app.jar" ]
