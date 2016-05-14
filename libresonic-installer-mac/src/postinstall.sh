#!/bin/bash

SUBSONIC_HOME="/Library/Application Support/Libresonic"

chmod oug+rwx "$SUBSONIC_HOME"
chown root:admin "$SUBSONIC_HOME"

chmod oug+rx "$SUBSONIC_HOME/transcode"
chown root:admin "$SUBSONIC_HOME/transcode"

rm -rf "$SUBSONIC_HOME/jetty"

echo Libresonic installation done
