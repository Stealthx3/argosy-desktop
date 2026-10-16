"""Idempotent first-run setup for the disposable RomM testbed: admin user, library scan, client token.

Reads and updates a credentials env file (git-ignored). Run with the testbed venv's python.

usage: romm-setup.py <base-url> <credentials-file> [--rescan]
"""
import pathlib
import secrets
import sys
import time

import requests
import socketio

TOKEN_SCOPES = [
    "me.read", "me.write", "roms.read", "roms.user.read", "roms.user.write",
    "platforms.read", "assets.read", "assets.write", "devices.read", "devices.write",
    "collections.read", "collections.write", "firmware.read",
]


def load(path):
    values = {}
    if path.exists():
        for line in path.read_text().splitlines():
            if "=" in line and not line.startswith("#"):
                key, value = line.split("=", 1)
                values[key] = value
    return values


def save(path, values):
    path.write_text("".join(f"{k}={v}\n" for k, v in values.items()))
    path.chmod(0o600)


def csrf_session(base):
    session = requests.Session()
    session.get(f"{base}/api/heartbeat").raise_for_status()
    token = session.cookies.get("romm_csrftoken")
    session.headers.update({"x-csrftoken": token, "Origin": base, "Referer": f"{base}/"})
    return session


def ensure_admin(base, creds):
    session = csrf_session(base)
    if session.post(f"{base}/api/login", auth=(creds["ROMM_USER"], creds["ROMM_PASSWORD"])).ok:
        return session
    response = session.post(f"{base}/api/users", json={
        "username": creds["ROMM_USER"], "password": creds["ROMM_PASSWORD"],
        "email": f"{creds['ROMM_USER']}@local.invalid", "role": "admin",
    })
    if not response.ok:
        sys.exit(f"creating admin failed: {response.status_code} {response.text}")
    session = csrf_session(base)
    session.post(f"{base}/api/login", auth=(creds["ROMM_USER"], creds["ROMM_PASSWORD"])).raise_for_status()
    print("admin user created")
    return session


def scan(base, session):
    client = socketio.Client()
    result = {}

    @client.on("scan:scanning_platform")
    def on_platform(data):
        print("scanning", (data or {}).get("name", data) if isinstance(data, dict) else data)

    @client.on("scan:done")
    def on_done(data):
        result["done"] = data

    @client.on("scan:done_ko")
    def on_ko(data):
        result["ko"] = data

    cookie = "; ".join(f"{c.name}={c.value}" for c in session.cookies)
    client.connect(base, socketio_path="/ws/socket.io/", headers={"Cookie": cookie}, wait_timeout=15)
    client.emit("scan", {
        "platforms": [], "platform_fs_slugs": [], "type": "quick",
        "apis": [], "launchbox_remote_enabled": False, "playmatch_enabled": False,
    })
    deadline = time.time() + 600
    while not result and time.time() < deadline:
        time.sleep(1)
    client.disconnect()
    if "done" not in result:
        sys.exit(f"scan did not finish: {result or 'timeout'}")
    print("scan done", result["done"])


def ensure_token(base, session, creds):
    token = creds.get("ROMM_CLIENT_TOKEN")
    if token and requests.get(f"{base}/api/users/me", headers={"Authorization": f"Bearer {token}"}).ok:
        return
    session.get(f"{base}/api/heartbeat")
    session.headers["x-csrftoken"] = session.cookies.get("romm_csrftoken")
    response = session.post(f"{base}/api/client-tokens", json={"name": "argosy-emulator", "scopes": TOKEN_SCOPES})
    if not response.ok:
        sys.exit(f"minting client token failed: {response.status_code} {response.text}")
    creds["ROMM_CLIENT_TOKEN"] = response.json()["raw_token"]
    print("client token minted")


def main():
    base = sys.argv[1].rstrip("/")
    path = pathlib.Path(sys.argv[2])
    creds = load(path)
    creds.setdefault("ROMM_USER", "testbed")
    creds.setdefault("ROMM_PASSWORD", secrets.token_hex(12))
    creds["ROMM_BASE_URL"] = base
    creds["ROMM_EMULATOR_URL"] = base.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")
    save(path, creds)
    session = ensure_admin(base, creds)
    roms = session.get(f"{base}/api/roms", params={"limit": 1}).json()
    total = roms.get("total", 0) if isinstance(roms, dict) else len(roms)
    if total == 0 or "--rescan" in sys.argv:
        scan(base, session)
    else:
        print(f"library already scanned ({total} roms)")
    ensure_token(base, session, creds)
    save(path, creds)


main()
