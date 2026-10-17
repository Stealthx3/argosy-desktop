#!/usr/bin/env bash
# Compares two save-proof.sh runs: the normalized captures of every step, and the negotiate counts.
# Exit 0 when every normalized file is identical.
#
# usage: scripts/desktop/testbed/proof-diff.sh <run-a> <run-b>
set -uo pipefail

A="$1"
B="$2"
status=0
for step in $(cd "$A" && find . -mindepth 1 -maxdepth 1 -type d | sort); do
    printf '%-24s %s | %s\n' "${step#./}" \
        "$(grep -ohE 'upload=[0-9]+ download=[0-9]+ conflict=[0-9]+ no_op=[0-9]+' "$A/$step/plan-reconcile.txt" | tr '\n' ';')" \
        "$(grep -ohE 'upload=[0-9]+ download=[0-9]+ conflict=[0-9]+ no_op=[0-9]+' "$B/$step/plan-reconcile.txt" 2>/dev/null | tr '\n' ';')"
    for f in $(cd "$A/$step" && ls ./*.normalized.* | grep -v state-tables); do
        if ! diff -u "$A/$step/$f" "$B/$step/$f" > /dev/null 2>&1; then
            echo "  DIFFERS: $step/${f#./}"
            diff -u "$A/$step/$f" "$B/$step/$f" | sed -n '3,40p' | sed 's/^/    /'
            status=1
        fi
    done
done
[ "$status" = 0 ] && echo "all normalized captures identical"
exit $status
