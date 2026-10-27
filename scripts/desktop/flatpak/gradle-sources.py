#!/usr/bin/env python3
"""
Turns the downloads of a fresh desktop-only Gradle build into flatpak-builder `file` sources.

Input: a log of the build's `--debug` lines ("Performing HTTP GET: <url>" and "Resource missing")
and the Gradle home the build filled. Every fetched Maven file becomes a source laid out under
`offline-repository/` with its repository path, so one local Maven repository replaces Google,
Maven Central and the plugin portal for an `--offline` build. Each checksum is taken from the file
Gradle cached; a file the cache does not hold is downloaded once to hash it.

usage: gradle-sources.py <capture.log> <gradle-home> <out.json>
"""
import hashlib
import json
import re
import sys
import urllib.request
from pathlib import Path

REPOSITORIES = (
    "https://repo.maven.apache.org/maven2/",
    "https://dl.google.com/dl/android/maven2/",
    "https://plugins.gradle.org/m2/",
)
GET = re.compile(r"Performing HTTP GET: (\S+)")
MISSING = re.compile(r"Resource missing\. \[HTTP GET: (\S+)\]")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def cached(home: Path, relative: str) -> Path | None:
    parts = relative.split("/")
    if len(parts) < 4:
        return None
    group, module, version, name = ".".join(parts[:-3]), parts[-3], parts[-2], parts[-1]
    folder = home / "caches/modules-2/files-2.1" / group / module / version
    return next(folder.glob(f"*/{name}"), None)


def main(log: Path, home: Path, out: Path) -> None:
    text = log.read_text()
    missing = set(MISSING.findall(text))
    urls = sorted({url for url in GET.findall(text) if url not in missing})
    sources, skipped = [], []
    for url in urls:
        base = next((repo for repo in REPOSITORIES if url.startswith(repo)), None)
        if base is None or url.endswith("maven-metadata.xml"):
            skipped.append(url)
            continue
        relative = url[len(base):]
        local = cached(home, relative)
        if local is not None:
            digest = sha256(local)
        else:
            with urllib.request.urlopen(url) as response:
                digest = hashlib.sha256(response.read()).hexdigest()
        folder, name = relative.rsplit("/", 1)
        sources.append({
            "type": "file",
            "url": url,
            "sha256": digest,
            "dest": f"offline-repository/{folder}",
            "dest-filename": name,
        })
    out.write_text(json.dumps(sources, indent=2) + "\n")
    print(f"{len(sources)} sources, {len(skipped)} skipped")
    for url in skipped:
        print(f"  skipped {url}")


if __name__ == "__main__":
    main(Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]))
