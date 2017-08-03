FROM alpine:3.6

LABEL description="Airsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/airsonic/airsonic"

ENV UID=1001 GID=1001 AIRSONIC_PORT=4040 AIRSONIC_DIR=/airsonic

WORKDIR $AIRSONIC_DIR

COPY dockerfiles/run.sh /usr/local/bin/run.sh
COPY airsonic-main/target/airsonic.war airsonic.war

RUN apk --no-cache add \
    ffmpeg \
    lame \
    su-exec \
    libressl \
    ca-certificates \
    tini \
    openjdk8-jre \
    && chmod +x /usr/local/bin/run.sh

EXPOSE $AIRSONIC_PORT

VOLUME $AIRSONIC_DIR/data $AIRSONIC_DIR/music $AIRSONIC_DIR/playlists $AIRSONIC_DIR/podcasts

ENTRYPOINT ["run.sh"]
