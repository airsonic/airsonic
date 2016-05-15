#!/bin/bash

LIBRESONIC_HOME="/Library/Application Support/Libresonic"

# Backup database.

if [ -e "$LIBRESONIC_HOME/db" ]; then
  rm -rf "$LIBRESONIC_HOME/db.backup"
  cp -R "$LIBRESONIC_HOME/db" "$LIBRESONIC_HOME/db.backup"
fi

