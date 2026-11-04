#!/bin/sh
app=/app/lib/argosy-desktop/lib/app
cfg=$app/argosy-desktop.cfg
classpath=$(sed -n "s|^app\.classpath=\\\$APPDIR|$app|p" "$cfg" | paste -sd:)
main=$(sed -n 's/^app\.mainclass=//p' "$cfg")
options=$(sed -n "s|^java-options=||p" "$cfg" | sed "s|\\\$APPDIR|$app|g")
trap 'kill -TERM "$pid" 2>/dev/null' TERM INT
while :; do
    /app/jre/bin/java $options -cp "$classpath" "$main" "$@" &
    pid=$!
    wait "$pid"
    status=$?
    while kill -0 "$pid" 2>/dev/null; do
        wait "$pid"
        status=$?
    done
    [ "$status" = 75 ] || exit "$status"
done
