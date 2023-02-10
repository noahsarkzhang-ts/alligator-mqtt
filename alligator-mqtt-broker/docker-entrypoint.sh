#!/bin/sh

set -ex;

exec /usr/bin/java \
  $JAVA_OPTS \
  -Djava.security.egd=file:/dev/./urandom \
  -Djava.net.preferIPv4Stack=true \
  -Djava.io.tmpdir="/home/java-app/tmp" \
  -jar \
  /home/java-app/lib/app.jar \
  "$@"
