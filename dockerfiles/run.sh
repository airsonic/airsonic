#!/bin/sh

mkdir -p $AIRSONIC_DIR/data/transcode
ln -s /usr/bin/ffmpeg $AIRSONIC_DIR/data/transcode/ffmpeg
ln -s /usr/bin/lame $AIRSONIC_DIR/data/transcode/lame

chown -R $UID:$GID $AIRSONIC_DIR/data $AIRSONIC_DIR/playlists $AIRSONIC_DIR/podcasts

exec su-exec $UID:$GID tini -- \
     java -Xmx256m \
     -Dserver.host=0.0.0.0 \
     -Dserver.port=$AIRSONIC_PORT \
     -Dserver.contextPath=/ \
     -Dairsonic.home=$AIRSONIC_DIR/data \
     -Dairsonic.defaultMusicFolder=$AIRSONIC_DIR/musics \
     -Dairsonic.defaultPodcastFolder=$AIRSONIC_DIR/podcasts \
     -Dairsonic.defaultPlaylistFolder=$AIRSONIC_DIR/playlists \
     -Djava.awt.headless=true \
     -jar airsonic.war
