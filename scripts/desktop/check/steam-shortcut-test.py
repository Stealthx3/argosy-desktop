#!/usr/bin/env python3
"""
Steam Shortcut Standard §8 step 1: add, re-add, refresh, status and remove against a fake Steam root
that already holds another game, checked with a VDF reader written here, independent of the helper.

usage: python3 -I scripts/desktop/check/steam-shortcut-test.py
"""
import json
import os
import struct
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
HELPER = os.path.join(ROOT, "packaging/steam/add_to_steam.py")
CONFIG = os.path.join(ROOT, "packaging/steam/steam-shortcut.json")
ART = ["p.png", ".png", "_hero.png", "_logo.png", "_icon.png"]


def read_map(buf, i):
    out = {}
    while True:
        kind = buf[i]
        i += 1
        if kind == 0x08:
            return out, i
        end = buf.index(b"\0", i)
        key = buf[i:end].decode()
        i = end + 1
        if kind == 0x00:
            out[key], i = read_map(buf, i)
        elif kind == 0x01:
            end = buf.index(b"\0", i)
            out[key] = buf[i:end].decode()
            i = end + 1
        elif kind == 0x02:
            out[key] = struct.unpack_from("<i", buf, i)[0]
            i += 4
        else:
            raise ValueError(f"type {kind:#x} at {i}")


def load(path):
    data, _ = read_map(open(path, "rb").read() + b"\x08", 0)
    return data


def write_map(d):
    out = b""
    for k, v in d.items():
        if isinstance(v, dict):
            out += b"\x00" + k.encode() + b"\0" + write_map(v)
        elif isinstance(v, int):
            out += b"\x02" + k.encode() + b"\0" + struct.pack("<i", v)
        else:
            out += b"\x01" + k.encode() + b"\0" + v.encode() + b"\0"
    return out + b"\x08"


def helper(op, root, *extra):
    r = subprocess.run([sys.executable, "-I", HELPER, op, "--config", CONFIG, "--steam-root", root, *extra],
                       capture_output=True, text=True)
    return r.returncode, json.loads(r.stdout) if r.stdout.strip().startswith("{") else r.stdout


def check(name, ok):
    print(("PASS " if ok else "FAIL ") + name)
    return ok


def main():
    results = []
    with tempfile.TemporaryDirectory() as tmp:
        root = os.path.join(tmp, "Steam")
        cfg = os.path.join(root, "userdata/12345/config")
        os.makedirs(cfg)
        vdf = os.path.join(cfg, "shortcuts.vdf")
        other = {"appid": -1234567, "AppName": "Other Game", "Exe": "\"/usr/bin/other\"", "StartDir": "\"/usr/bin/\"",
                 "LastPlayTime": 1700000000, "tags": {"0": "Favorites"}}
        open(vdf, "wb").write(write_map({"shortcuts": {"0": other}}))

        code, out = helper("add", root)
        results.append(check("add without --yes is refused or finds Steam closed", code in (0, 3)))
        code, out = helper("add", root, "--yes")
        results.append(check("add exits 0", code == 0))
        d = load(vdf)["shortcuts"]
        ours = [e for e in d.values() if e.get("FlatpakAppID") == "io.github.stealthx3.icebreaker-client"]
        results.append(check("one IceBreaker entry", len(ours) == 1))
        results.append(check("other game unchanged", d["0"] == other))
        e = ours[0]
        results.append(check("name, exe and launch options", e["AppName"] == "IceBreaker" and e["Exe"] == "\"/usr/bin/flatpak\""
                             and e["LaunchOptions"].startswith("run io.github.stealthx3.icebreaker-client")))
        results.append(check("IceSuite tag", "IceSuite" in e["tags"].values()))
        sid = e["appid"] & 0xFFFFFFFF
        grid = os.path.join(cfg, "grid")
        results.append(check("five art files", all(os.path.isfile(os.path.join(grid, f"{sid}{s}")) for s in ART)))
        results.append(check("icon field points at the grid icon", e["icon"].endswith(f"{sid}_icon.png")))

        helper("add", root, "--yes")
        d = load(vdf)["shortcuts"]
        results.append(check("re-add keeps one entry and its id",
                             [x["appid"] for x in d.values() if x.get("FlatpakAppID")] == [e["appid"]]))

        os.remove(os.path.join(grid, f"{sid}_hero.png"))
        before = open(vdf, "rb").read()
        code, out = helper("refresh", root)
        results.append(check("refresh restores art without touching shortcuts.vdf",
                             code == 0 and os.path.isfile(os.path.join(grid, f"{sid}_hero.png")) and open(vdf, "rb").read() == before))

        code, out = helper("status", root)
        results.append(check("status reports present", code == 0 and out["results"][0]["present"] is True))

        code, out = helper("remove", root, "--yes")
        d = load(vdf)["shortcuts"]
        results.append(check("remove leaves only the other game", code == 0 and list(d.values()) == [other]))
        results.append(check("remove deletes our art", not any(os.path.exists(os.path.join(grid, f"{sid}{s}")) for s in ART)))
        results.append(check("backup kept", os.path.isfile(vdf + ".icesuite-backup")))
    print(f"{sum(results)}/{len(results)} passed")
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
