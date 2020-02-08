FROM alpine:3.9

LABEL description="Airsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/airsonic/airsonic"

ENV AIRSONIC_PORT=4040 AIRSONIC_DIR=/airsonic CONTEXT_PATH=/ UPNP_PORT=4041 JVM_HEAP=256m

WORKDIR $AIRSONIC_DIR

RUN apk --no-cache add \
    ffmpeg \
    lame \
    bash \
    libressl \
    fontconfig \
    ttf-dejavu \
    ca-certificates \
    tini \
    curl \
    openjdk8-jre

COPY run.sh /usr/local/bin/run.sh

RUN chmod +x /usr/local/bin/run.sh

COPY target/dependency/airsonic-main.war airsonic.war

EXPOSE $AIRSONIC_PORT

# Default DLNA/UPnP ports
EXPOSE $UPNP_PORT
EXPOSE 1900/udp

VOLUME $AIRSONIC_DIR/data $AIRSONIC_DIR/music $AIRSONIC_DIR/playlists $AIRSONIC_DIR/podcasts

HEALTHCHECK --interval=15s --timeout=3s CMD wget -q http://localhost:"$AIRSONIC_PORT""$CONTEXT_PATH"rest/ping -O /dev/null || exit 1

ENTRYPOINT ["tini", "--", "run.sh"]
