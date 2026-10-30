#!/bin/sh
app=/app/lib/argosy-desktop/lib/app
cfg=$app/argosy-desktop.cfg
classpath=$(sed -n "s|^app\.classpath=\\\$APPDIR|$app|p" "$cfg" | paste -sd:)
main=$(sed -n 's/^app\.mainclass=//p' "$cfg")
options=$(sed -n "s|^java-options=||p" "$cfg" | sed "s|\\\$APPDIR|$app|g")
exec /app/jre/bin/java $options -cp "$classpath" "$main" "$@"
