#!/usr/bin/env bash
# Runs a shell command in a lab VM through qemu-guest-agent and prints its output and exit code.
# usage: exec.sh <profile> [--long] [--user] '<command>'   (--user runs it as tester in the desktop session)
# --long: for minutes-long commands. The agent buffers captured output in memory and qemu-ga aborted
# after ~20 min of `flatpak install`, so the command runs detached with its output in a guest log, and
# only a short status check goes through the agent every 10 s; the log's tail is printed at the end.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lab.sh"
dom=$(lab_domain "$1"); shift
as_user=0 long=0
if [ "${1:-}" = --long ]; then long=1; shift; fi
if [ "${1:-}" = --user ]; then as_user=1; shift; fi
cmd=$1
if [ "$as_user" = 1 ]; then
    cmd="runuser -u tester -- env XDG_RUNTIME_DIR=/run/user/\$(id -u tester) DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/\$(id -u tester)/bus bash -lc $(printf %q "$cmd")"
fi
python3 -I - "$dom" "$cmd" "$long" <<'PY'
import base64, json, subprocess, sys, time
dom, cmd, long = sys.argv[1], sys.argv[2], sys.argv[3] == '1'
def agent(payload):
    out = subprocess.run(['virsh', 'qemu-agent-command', dom, json.dumps(payload)], check=True, capture_output=True, text=True, timeout=60).stdout
    return json.loads(out)['return']
def run(command, poll):
    pid = agent({'execute': 'guest-exec', 'arguments': {'path': '/bin/bash', 'arg': ['-c', command], 'capture-output': True}})['pid']
    while True:
        st = agent({'execute': 'guest-exec-status', 'arguments': {'pid': pid}})
        if st.get('exited'):
            return st
        time.sleep(poll)
if long:
    job = f'/var/tmp/lab-exec-{int(time.time() * 1000)}'
    quoted = "'" + cmd.replace("'", "'\\''") + "'"
    run(f"(setsid sh -c {quoted} > {job}.log 2>&1 < /dev/null; echo $? > {job}.rc) > /dev/null 2>&1 &", 0.5)
    while True:
        time.sleep(10)
        try:
            st = run(f'cat {job}.rc 2>/dev/null', 0.5)
        except (subprocess.TimeoutExpired, subprocess.CalledProcessError):
            continue
        rc = base64.b64decode(st.get('out-data', '')).decode().strip()
        if rc:
            break
    st = run(f'tail -n 40 {job}.log; exit {rc}', 0.5)
else:
    st = run(cmd, 0.5)
for key, stream in (('out-data', sys.stdout), ('err-data', sys.stderr)):
    if key in st:
        stream.write(base64.b64decode(st[key]).decode(errors='replace'))
sys.exit(st.get('exitcode', 1))
PY
