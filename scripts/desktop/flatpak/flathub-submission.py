#!/usr/bin/env python3
"""
Writes the Flathub submission files for one release: <out>/io.github.stealthx3.icebreaker-client.yml and
<out>/generated/gradle-sources.json, from desktopApp/flatpak/io.github.stealthx3.icebreaker-client.flathub.yml
with its local git and dir sources replaced by GitHub URLs pinned to the release tag and commit.

usage: python3 -I scripts/desktop/flatpak/flathub-submission.py <tag> <commit> <out-dir>
"""
import shutil
import subprocess
import sys
from pathlib import Path

APP_ID = "io.github.stealthx3.icebreaker-client"
APP_URL = "https://github.com/Stealthx3/icebreaker-client.git"
SIGIL_URL = "https://github.com/rommapp/argosy-sigil.git"
SRC = Path(f"desktopApp/flatpak/{APP_ID}.flathub.yml")

tag, commit, out = sys.argv[1], sys.argv[2], Path(sys.argv[3])
sigil_commit = subprocess.run(["git", "rev-parse", f"{commit}:sigil"], capture_output=True, text=True, check=True).stdout.strip()
text = SRC.read_text(encoding="utf-8")


def swap(old: str, new: str) -> None:
    global text
    if text.count(old) != 1:
        sys.exit(f"expected one occurrence of:\n{old}")
    text = text.replace(old, new)


swap("""      - type: git
        path: ../../sigil
        branch: HEAD
        dest: sigil
""", f"""      - type: git
        url: {SIGIL_URL}
        commit: {sigil_commit}
        dest: sigil
""")
swap("""    build-commands:
      - install -Dm755 add_to_steam.py ${FLATPAK_DEST}/bin/${FLATPAK_ID}-steam
      - install -Dm644 -t ${FLATPAK_DEST}/share/${FLATPAK_ID}/steam steam-shortcut.json
          capsule-600x900.png header-920x430.png hero-3840x1240.png logo-1280x720.png icon-256.png
    sources:
      - type: dir
        path: ../../packaging/steam
""", f"""    subdir: packaging/steam
    build-commands:
      - install -Dm755 add_to_steam.py ${{FLATPAK_DEST}}/bin/${{FLATPAK_ID}}-steam
      - install -Dm644 -t ${{FLATPAK_DEST}}/share/${{FLATPAK_ID}}/steam steam-shortcut.json
          capsule-600x900.png header-920x430.png hero-3840x1240.png logo-1280x720.png icon-256.png
    sources:
      - type: git
        url: {APP_URL}
        tag: {tag}
        commit: {commit}
        disable-submodules: true
""")
swap("""      - type: git
        path: ../..
        branch: HEAD
""", f"""      - type: git
        url: {APP_URL}
        tag: {tag}
        commit: {commit}
""")
text = "\n".join(l for l in text.splitlines() if not l.startswith("# For Flathub, the two git sources")) + "\n"

(out / "generated").mkdir(parents=True, exist_ok=True)
(out / f"{APP_ID}.yml").write_text(text, encoding="utf-8")
shutil.copyfile("desktopApp/flatpak/generated/gradle-sources.json", out / "generated" / "gradle-sources.json")
print(f"wrote {out}/{APP_ID}.yml (app {tag} {commit[:12]}, sigil {sigil_commit[:12]})")
