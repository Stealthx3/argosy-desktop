"""Mints a fresh client token on the testbed RomM and starts pairing it; writes the pairing code to
build/desktop-testbed/pair-code.txt (mode 600) and prints only its expiry, never the code."""
import os
import sys
from pathlib import Path
import time

sys.path.insert(0, os.path.dirname(__file__))
romm_setup = __import__("romm-setup")

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../.."))
CREDENTIALS = Path(ROOT, "build/desktop-testbed/credentials.env")
OUT = os.path.join(ROOT, "build/desktop-testbed/pair-code.txt")


def main():
    creds = romm_setup.load(CREDENTIALS)
    base = creds["ROMM_BASE_URL"]
    session = romm_setup.ensure_admin(base, creds)
    session.get(f"{base}/api/heartbeat")
    session.headers["x-csrftoken"] = session.cookies.get("romm_csrftoken")
    name = f"argosy-desktop-{int(time.time())}"
    response = session.post(f"{base}/api/client-tokens", json={"name": name, "scopes": romm_setup.TOKEN_SCOPES})
    if not response.ok:
        sys.exit(f"minting client token failed: {response.status_code}")
    response = session.post(f"{base}/api/client-tokens/{response.json()['id']}/pair")
    if not response.ok:
        sys.exit(f"starting pairing failed: {response.status_code}")
    body = response.json()
    fd = os.open(OUT, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w") as out:
        out.write(body["code"])
    print(f"pairing code for {name} written; expires in {body['expires_in']} s")


if __name__ == "__main__":
    main()
