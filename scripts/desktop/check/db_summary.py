#!/usr/bin/env python3
import sqlite3, sys, json, hashlib
c = sqlite3.connect(sys.argv[1])
out = {"user_version": c.execute("PRAGMA user_version").fetchone()[0],
       "identity_hash": c.execute("SELECT identity_hash FROM room_master_table").fetchone()[0]}
tables = [r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
out["tables"] = {t: c.execute(f'SELECT COUNT(*) FROM "{t}"').fetchone()[0] for t in tables}
out["checksum"] = {t: hashlib.sha1(repr(c.execute(f"SELECT * FROM \"{t}\" ORDER BY 1").fetchall()).encode()).hexdigest() for t in tables if t not in ("sqlite_sequence", "android_metadata")}
print(json.dumps(out, sort_keys=True))
