#!/usr/bin/env bash
# usage: deep-settings-walk.sh android|desktop <outdir>; opens Theme → Backdrop/Fonts, Interface → Controller Grip/Home Screen, Audio → Sounds/Music, Storage → Games/Caches (p4n). The last About step drifts; do not rely on it.
cd /mnt/FileDrive/Personal/Projects/Git/argosy-desktop
exec scripts/desktop/check/pad-walk.sh "$1" "$2" shot:home START wait:2 DOWN*8 wait:1 A wait:3 \
 A wait:3 DOWN*5 wait:1 A wait:3 shot:backdrop B wait:2 DOWN wait:1 A wait:3 shot:fonts B wait:2 B wait:2 \
 DOWN wait:1 A wait:3 DOWN*5 wait:1 A wait:3 shot:grip B wait:2 DOWN wait:1 A wait:3 shot:homescreen B wait:2 B wait:2 \
 DOWN*2 wait:1 A wait:3 A wait:3 shot:sounds B wait:2 DOWN wait:1 A wait:3 shot:music B wait:2 B wait:2 \
 DOWN*8 wait:1 A wait:3 A wait:3 shot:stgames B wait:2 DOWN*2 wait:1 A wait:3 shot:stcaches B wait:2 B wait:2 \
 DOWN*8 wait:1 A wait:3 DOWN*12 wait:1 shot:about-bottom B wait:2
