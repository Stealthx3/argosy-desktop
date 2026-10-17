"""Uploads deterministic server-side saves to the testbed through the RomM API. Idempotent: a save
whose slot and content already exist on the server is skipped.

Seeds (built-in emulator layout: core id as emulator, `<rom name>.srm`):
  Ledger Isle   autosave             passive cart, boot counter 0 (quitting it changes nothing)
  Battery Bay   autosave, checkpoint counter cart, counters 5 and 2 (two slots)
Save Point Harbor gets no server save, so its first session uploads a brand-new one.

RomM 5.1 stores each upload as `<name> [YYYY-MM-DD_HH-MM-SS].srm` and replaces an existing save with
the same stored name, so two uploads for one rom within a second collapse into one row; uploads are
spaced by a second.

usage: seed-saves.py <base-url> <credentials-file>
"""
import hashlib
import importlib.util
import pathlib
import sys
import time

import requests

HERE = pathlib.Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("make_library", HERE / "make-library.py")
library = importlib.util.module_from_spec(spec)
spec.loader.exec_module(library)

EMULATOR = "mgba"
SEEDS = [
    ("Ledger Isle", 0x03, "autosave", 0),
    ("Battery Bay", 0x01, "checkpoint", 2),
    ("Battery Bay", 0x01, "autosave", 5),
]


def main():
    base = sys.argv[1].rstrip("/")
    creds = dict(line.split("=", 1) for line in pathlib.Path(sys.argv[2]).read_text().splitlines() if "=" in line)
    auth = {"Authorization": f"Bearer {creds['ROMM_CLIENT_TOKEN']}"}
    roms = requests.get(f"{base}/api/roms", params={"limit": 500}, headers=auth).json()
    roms = roms["items"] if isinstance(roms, dict) else roms
    by_name = {r["fs_name_no_ext"]: r for r in roms}
    for title, title_id, slot, counter in SEEDS:
        rom = by_name.get(f"{title} (Homebrew)")
        if rom is None:
            sys.exit(f"{title} is not on the server; run up.sh --rescan")
        data = library.sram(title_id, counter)
        digests = {hashlib.md5(data).hexdigest(), hashlib.sha1(data).hexdigest(), hashlib.sha256(data).hexdigest()}
        existing = requests.get(f"{base}/api/saves", params={"rom_id": rom["id"]}, headers=auth).json()
        if any(s.get("slot") == slot and s.get("content_hash") in digests for s in existing):
            print(f"{title} [{slot}] already seeded")
            continue
        response = requests.post(
            f"{base}/api/saves",
            params={"rom_id": rom["id"], "emulator": EMULATOR, "slot": slot},
            files={"saveFile": (f"{title} (Homebrew).srm", data, "application/octet-stream")},
            headers=auth,
        )
        if not response.ok:
            sys.exit(f"upload {title} [{slot}] failed: {response.status_code} {response.text}")
        save = response.json()
        time.sleep(1.1)
        print(f"{title} [{slot}] seeded as save {save['id']} content_hash={save.get('content_hash')}")


main()
