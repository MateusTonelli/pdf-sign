#!/bin/sh
set -e
mkdir -p /data/storage
if [ "$(id -u)" = "0" ]; then
  chown -R appuser:appuser /data/storage || true
  exec gosu appuser java -jar /app/app.jar
fi
exec java -jar /app/app.jar
