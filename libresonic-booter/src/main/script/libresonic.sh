#!/bin/sh

###################################################################################
# Shell script for starting Libresonic.  See http://libresonic.org.
#
# Author: Sindre Mehus
###################################################################################

LIBRESONIC_HOME=/var/libresonic
LIBRESONIC_HOST=0.0.0.0
LIBRESONIC_PORT=4040
LIBRESONIC_HTTPS_PORT=0
LIBRESONIC_CONTEXT_PATH=/
LIBRESONIC_MAX_MEMORY=150
LIBRESONIC_PIDFILE=
LIBRESONIC_DEFAULT_MUSIC_FOLDER=/var/music
LIBRESONIC_DEFAULT_PODCAST_FOLDER=/var/music/Podcast
LIBRESONIC_DEFAULT_PLAYLIST_FOLDER=/var/playlists

quiet=0

usage() {
    echo "Usage: libresonic.sh [options]"
    echo "  --help               This small usage guide."
    echo "  --home=DIR           The directory where Libresonic will create files."
    echo "                       Make sure it is writable. Default: /var/libresonic"
    echo "  --host=HOST          The host name or IP address on which to bind Libresonic."
    echo "                       Only relevant if you have multiple network interfaces and want"
    echo "                       to make Libresonic available on only one of them. The default value"
    echo "                       will bind Libresonic to all available network interfaces. Default: 0.0.0.0"
    echo "  --port=PORT          The port on which Libresonic will listen for"
    echo "                       incoming HTTP traffic. Default: 4040"
    echo "  --https-port=PORT    The port on which Libresonic will listen for"
    echo "                       incoming HTTPS traffic. Default: 0 (disabled)"
    echo "  --context-path=PATH  The context path, i.e., the last part of the Libresonic"
    echo "                       URL. Typically '/' or '/libresonic'. Default '/'"
    echo "  --max-memory=MB      The memory limit (max Java heap size) in megabytes."
    echo "                       Default: 100"
    echo "  --pidfile=PIDFILE    Write PID to this file. Default not created."
    echo "  --quiet              Don't print anything to standard out. Default false."
    echo "  --default-music-folder=DIR    Configure Libresonic to use this folder for music.  This option "
    echo "                                only has effect the first time Libresonic is started. Default '/var/music'"
    echo "  --default-podcast-folder=DIR  Configure Libresonic to use this folder for Podcasts.  This option "
    echo "                                only has effect the first time Libresonic is started. Default '/var/music/Podcast'"
    echo "  --default-playlist-folder=DIR Configure Libresonic to use this folder for playlists.  This option "
    echo "                                only has effect the first time Libresonic is started. Default '/var/playlists'"
    exit 1
}

# Parse arguments.
while [ $# -ge 1 ]; do
    case $1 in
        --help)
            usage
            ;;
        --home=?*)
            LIBRESONIC_HOME=${1#--home=}
            ;;
        --host=?*)
            LIBRESONIC_HOST=${1#--host=}
            ;;
        --port=?*)
            LIBRESONIC_PORT=${1#--port=}
            ;;
        --https-port=?*)
            LIBRESONIC_HTTPS_PORT=${1#--https-port=}
            ;;
        --context-path=?*)
            LIBRESONIC_CONTEXT_PATH=${1#--context-path=}
            ;;
        --max-memory=?*)
            LIBRESONIC_MAX_MEMORY=${1#--max-memory=}
            ;;
        --pidfile=?*)
            LIBRESONIC_PIDFILE=${1#--pidfile=}
            ;;
        --quiet)
            quiet=1
            ;;
        --default-music-folder=?*)
            LIBRESONIC_DEFAULT_MUSIC_FOLDER=${1#--default-music-folder=}
            ;;
        --default-podcast-folder=?*)
            LIBRESONIC_DEFAULT_PODCAST_FOLDER=${1#--default-podcast-folder=}
            ;;
        --default-playlist-folder=?*)
            LIBRESONIC_DEFAULT_PLAYLIST_FOLDER=${1#--default-playlist-folder=}
            ;;
        *)
            usage
            ;;
    esac
    shift
done

# Use JAVA_HOME if set, otherwise assume java is in the path.
JAVA=java
if [ -e "${JAVA_HOME}" ]
    then
    JAVA=${JAVA_HOME}/bin/java
fi

# Create Libresonic home directory.
mkdir -p ${LIBRESONIC_HOME}
LOG=${LIBRESONIC_HOME}/libresonic_sh.log
rm -f ${LOG}

cd $(dirname $0)
if [ -L $0 ] && ([ -e /bin/readlink ] || [ -e /usr/bin/readlink ]); then
    cd $(dirname $(readlink $0))
fi

${JAVA} -Xmx${LIBRESONIC_MAX_MEMORY}m \
  -Dlibresonic.home=${LIBRESONIC_HOME} \
  -Dlibresonic.host=${LIBRESONIC_HOST} \
  -Dlibresonic.port=${LIBRESONIC_PORT} \
  -Dlibresonic.httpsPort=${LIBRESONIC_HTTPS_PORT} \
  -Dlibresonic.contextPath=${LIBRESONIC_CONTEXT_PATH} \
  -Dlibresonic.defaultMusicFolder=${LIBRESONIC_DEFAULT_MUSIC_FOLDER} \
  -Dlibresonic.defaultPodcastFolder=${LIBRESONIC_DEFAULT_PODCAST_FOLDER} \
  -Dlibresonic.defaultPlaylistFolder=${LIBRESONIC_DEFAULT_PLAYLIST_FOLDER} \
  -Djava.awt.headless=true \
  -verbose:gc \
  -jar libresonic-booter-jar-with-dependencies.jar > ${LOG} 2>&1 &

# Write pid to pidfile if it is defined.
if [ $LIBRESONIC_PIDFILE ]; then
    echo $! > ${LIBRESONIC_PIDFILE}
fi

if [ $quiet = 0 ]; then
    echo Started Libresonic [PID $!, ${LOG}]
fi

