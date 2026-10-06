#!/bin/sh
set -eu
if [ -n "${AIVEN_TRUSTSTORE_BASE64:-}" ]; then
    if [ -z "${AIVEN_TRUSTSTORE_PASSWORD:-}" ]; then
        echo 'Aiven truststore password is required.' >&2
        exit 1
    fi
    umask 077
    printf '%s' "$AIVEN_TRUSTSTORE_BASE64" | base64 -d > /tmp/aiven-truststore.p12
    unset AIVEN_TRUSTSTORE_BASE64
fi
exec java -jar /app/app.jar
