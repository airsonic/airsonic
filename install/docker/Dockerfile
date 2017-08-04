FROM alpine:3.6

LABEL description="Airsonic is a free, web-based media streamer, providing ubiquitious access to your music." \
      url="https://github.com/airsonic/airsonic"

ENV AIRSONIC_PORT=4040 AIRSONIC_DIR=/airsonic

WORKDIR $AIRSONIC_DIR

RUN apk --no-cache add \
    ffmpeg \
    lame \
    bash \
    libressl \
    ca-certificates \
    tini \
    openjdk8-jre

COPY run.sh /usr/local/bin/run.sh

RUN chmod +x /usr/local/bin/run.sh

COPY target/dependency/airsonic-main.war airsonic.war

EXPOSE $AIRSONIC_PORT

VOLUME $AIRSONIC_DIR/data $AIRSONIC_DIR/music $AIRSONIC_DIR/playlists $AIRSONIC_DIR/podcasts

ENTRYPOINT ["tini", "--", "run.sh"]
