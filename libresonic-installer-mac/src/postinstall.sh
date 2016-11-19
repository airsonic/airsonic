#!/bin/bash

LIBRESONIC_HOME="/Library/Application Support/Libresonic"

chmod oug+rwx "$LIBRESONIC_HOME"
chown root:admin "$LIBRESONIC_HOME"

chmod oug+rx "$LIBRESONIC_HOME/transcode"
chown root:admin "$LIBRESONIC_HOME/transcode"

rm -rf "$LIBRESONIC_HOME/jetty"

echo Libresonic installation done
