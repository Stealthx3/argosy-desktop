#!/usr/bin/env bash
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lab.sh"
profile=$1 out=$2
mkdir -p "$out"
report=$out/report.txt
fail=0
: > "$report"

note() { echo "$*" | tee -a "$report"; }
check() {
    local name=$1; shift
    local text
    if text=$("$@" 2>&1); then note "PASS $name: $(echo "$text" | tail -n 1 | cut -c1-140)"
    else note "FAIL $name: $(echo "$text" | tail -n 1 | cut -c1-140)"; fail=1; fi
}
guest() { "$here/exec.sh" "$profile" --user "$1"; }
walk() { "$here/walk.sh" "$profile" "$out" "$@" >/dev/null 2>&1; }
mouse() { "$here/mouse.sh" "$profile" "$@"; }
app_pid() { "$here/app.sh" "$profile" status 2>/dev/null | tail -n 1 | cut -d' ' -f1; }

base='$HOME/.var/app/io.github.stealthx3.ArgosyDesktop'
sql() {
    guest "python3 -c \"import sqlite3,os;c=sqlite3.connect(os.path.expanduser('~/.var/app/io.github.stealthx3.ArgosyDesktop/data/argosy-desktop/alauncher.db'));$1\""
}
seed='
p=c.execute(\"select id from platforms where slug=\\\"nes\\\"\").fetchone()[0];
g=c.execute(\"select id from games limit 1\").fetchone()[0];
c.execute(\"update platforms set customRomPath=\\\"/home/tester/CustomNes\\\" where id=?\",(p,));
c.execute(\"insert into emulator_configs(platformId,packageName,displayName,isDefault) values(?,\\\"org.libretro.RetroArch\\\",\\\"RetroArch\\\",1)\",(p,));
c.execute(\"insert into emulator_configs(platformId,gameId,packageName,displayName,isDefault) values(?,?,\\\"org.libretro.RetroArch\\\",\\\"RetroArch\\\",0)\",(p,g));
c.execute(\"insert into emulator_save_config(emulatorId,savePathPattern,isAutoDetected,isUserOverride,isUserStateOverride,savesBesideRom) values(\\\"x\\\",\\\"/home/tester/s\\\",0,1,0,0)\");
c.execute(\"insert into emulator_launch_args(platformId,emulatorId,customExtras) values(?,\\\"x\\\",\\\"--foo\\\")\",(p,));
c.execute(\"insert into platform_libretro_settings(platformId,shader) values(?,\\\"crt\\\")\",(p,));
c.commit()'
counts='
q=lambda s:c.execute(s).fetchone()[0];
print(q(\"select count(*) from emulator_configs where gameId is null and platformId is not null\"),q(\"select count(*) from emulator_configs where gameId is not null\"),q(\"select count(*) from emulator_save_config\"),q(\"select count(*) from emulator_launch_args\"),q(\"select count(*) from platform_libretro_settings\"),q(\"select count(*) from platforms where customRomPath is not null\"),q(\"select count(*) from games\")>0)'

note "reset-test $profile $(date -Iseconds)"
"$here/app.sh" "$profile" kill >/dev/null 2>&1
sleep 2
guest "echo mine > \"\$HOME/Games/Argosy/nes/mine.nes\"; d=\$HOME/.var/app/org.libretro.RetroArch/config/retroarch/saves; mkdir -p \$d; echo save > \$d/keep.srm" >/dev/null

check start "$here/app.sh" "$profile" start
sleep 45
walk shot:h0-home PAD:DPAD_LEFT wait:3 PAD:DPAD_DOWN*7 wait:1 PAD:SOUTH wait:4 PAD:DPAD_DOWN wait:1 PAD:SOUTH wait:3 PAD:DPAD_DOWN wait:1 PAD:DPAD_RIGHT wait:3 shot:h1-profile
check profile-desktop guest "strings $base/data/argosy-desktop/datastore/settings.preferences_pb | grep -A1 display_profile | grep -x DESKTOP"
walk PAD:EAST wait:3 PAD:EAST wait:3 PAD:DPAD_LEFT wait:3 shot:h2-drawer
mouse move 450 465
sleep 1
walk shot:h3-drawer-hover
mouse move 1500 700
sleep 1
walk PAD:EAST wait:3
"$here/app.sh" "$profile" kill >/dev/null 2>&1
sleep 2

sql "$seed" >/dev/null
note "seeded (platform, per-game, save, args, libretro, romdir, games): $(sql "$counts" | tail -n 1)"
check start "$here/app.sh" "$profile" start
sleep 45
walk PAD:DPAD_LEFT wait:3 PAD:DPAD_DOWN*7 wait:1 PAD:SOUTH wait:4 PAD:DPAD_DOWN*9 wait:1 PAD:SOUTH wait:4 PAD:DPAD_DOWN*2 wait:1 shot:p1-row PAD:SOUTH wait:2 shot:p2-dialog PAD:DPAD_RIGHT wait:1 PAD:SOUTH wait:4 shot:p3-after
"$here/app.sh" "$profile" kill >/dev/null 2>&1
sleep 2
after=$(sql "$counts" | tail -n 1)
if [ "$after" = "0 1 0 0 0 0 True" ]; then note "PASS platform-reset: $after"; else note "FAIL platform-reset: $after"; fail=1; fi

check start "$here/app.sh" "$profile" start
sleep 45
before=$(app_pid)
walk PAD:DPAD_LEFT wait:3 PAD:DPAD_DOWN*7 wait:1 PAD:SOUTH wait:4 PAD:DPAD_UP*2 wait:2 shot:f1-row PAD:SOUTH wait:3 shot:f2-warning PAD:EAST wait:2 PAD:SOUTH wait:2 PAD:DPAD_RIGHT wait:1 PAD:SOUTH wait:3 shot:f3-confirm PAD:DPAD_RIGHT wait:1 PAD:SOUTH
restarted=
for i in $(seq 60); do
    sleep 2
    now=$(app_pid)
    case "$now" in ''|not) ;; "$before") ;; *) restarted=$i; break ;; esac
done
if [ -n "$restarted" ]; then note "PASS restart: new process after $((restarted * 2)) s"; else note "FAIL restart: no new process"; fail=1; fi
sleep 45
walk shot:f4-wizard
check rom-deleted guest "if ls \"\$HOME/Games/Argosy/nes\" | grep -q Eight; then echo still there; exit 1; fi; echo gone"
check user-rom-kept guest "cat \"\$HOME/Games/Argosy/nes/mine.nes\""
check emulator-save-kept guest "cat \$HOME/.var/app/org.libretro.RetroArch/config/retroarch/saves/keep.srm"
check wiped guest "cd $base/data; [ ! -e argosy-desktop-factory-reset ] || { echo marker left; exit 1; }; [ ! -e argosy-desktop/files/reset_records ] || { echo records left; exit 1; }; echo clean"
check no-games sql 'n=c.execute(\"select count(*) from games\").fetchone()[0];print(n);raise SystemExit(n)'
check sigterm "$here/app.sh" "$profile" term
note "result $([ $fail = 0 ] && echo PASS || echo FAIL) (screens: $out)"
exit $fail
