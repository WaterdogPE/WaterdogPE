# This Dockerfile uses Docker Multi-Stage Builds
# See https://docs.docker.com/engine/userguide/eng-image/multistage-build/
# Dockerfile snipets used from https://github.com/CloudburstMC/Nukkit
# Requires Docker v17.05

# Use OpenJDK JDK image for intermiediate build
FROM openjdk:11-jdk-slim AS build

# Install packages required for build
RUN apt-get -y update 
RUN apt-get install -y build-essential
RUN apt-get install -y git
RUN mkdir -p /usr/share/man/man1
RUN apt-get install -y maven

# Build from source and create artifact
WORKDIR /build

COPY mvn* pom.xml /build/
COPY src /build/src
COPY .git /build/.git
COPY .mvn /build/.mvn

RUN git submodule update --init 
RUN mvn clean package

# Use OpenJDK JRE image for runtime
FROM openjdk:11-jre-slim AS run
LABEL maintainer="OoP1nk <oop1nk@skulkstudio.com>" authors="WaterdogTeam"

# Copy artifact from build image
COPY --from=build /build/target/waterdogpe-*.jar /app/waterdogpe.jar

# Create minecraft user
RUN useradd --user-group \
    --no-create-home \
    --home-dir /data \
    --shell /usr/sbin/nologin \
    minecraft

# Ports
EXPOSE 19132

RUN mkdir /data && /home/minecraft
RUN chown -R minecraft:minecraft /app /data /home/minecraft

# User and group to run as
USER minecraft:minecraft

# Set runtime workfir
WORKDIR /data

# Run app
ENTRYPOINT [ "java" ]
CMD ["-jar", "/app/waterdogpe.jar"]