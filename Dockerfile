################################################################################################################ builder

FROM maven:3-jdk-8 as builder
WORKDIR /build

# this block will make docker cache the dependencies for subsequent builds of the same image.
COPY pom.xml .
COPY subsonic-rest-api/pom.xml ./subsonic-rest-api/pom.xml
COPY integration-test/pom.xml ./integration-test/pom.xml
COPY airsonic-sonos-api/pom.xml ./airsonic-sonos-api/pom.xml
COPY airsonic-main/pom.xml ./airsonic-main/pom.xml
RUN mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

COPY . .
RUN mvn package

################################################################################################################ runtime

FROM adoptopenjdk/openjdk8-openj9:alpine  as runtime
LABEL description="Airsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/airsonic/airsonic"

ENV AIRSONIC_PORT=4040
ENV AIRSONIC_DIR=/airsonic
ENV CONTEXT_PATH=/
ENV UPNP_PORT=4041
ENV JVM_HEAP=256m

WORKDIR $AIRSONIC_DIR
RUN apk --no-cache add \
    ffmpeg \
    lame \
    bash \
    libressl \
    fontconfig \
    ttf-dejavu \
    ca-certificates \
    tini
COPY install/docker/run.sh /usr/local/bin/run.sh
RUN chmod +x /usr/local/bin/run.sh
COPY --from=builder  /build/airsonic-main/target/airsonic.war airsonic.war

EXPOSE $AIRSONIC_PORT
EXPOSE $UPNP_PORT
EXPOSE 1900/udp

VOLUME $AIRSONIC_DIR/data
VOLUME $AIRSONIC_DIR/music
VOLUME $AIRSONIC_DIR/playlists
VOLUME $AIRSONIC_DIR/podcasts

HEALTHCHECK --interval=15s --timeout=3s CMD wget -q http://localhost:"$AIRSONIC_PORT""$CONTEXT_PATH"rest/ping -O /dev/null || exit 1
ENTRYPOINT ["tini", "--", "run.sh"]
