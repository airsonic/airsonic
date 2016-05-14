#!/bin/bash

SUBSONIC_HOME="/Library/Application Support/Libresonic"

# Backup database.

if [ -e "$SUBSONIC_HOME/db" ]; then
  rm -rf "$SUBSONIC_HOME/db.backup"
  cp -R "$SUBSONIC_HOME/db" "$SUBSONIC_HOME/db.backup"
fi

