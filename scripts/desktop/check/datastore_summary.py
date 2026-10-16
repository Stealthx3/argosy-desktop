#!/usr/bin/env python3
"""
Prints the contents of DataStore preferences files (*.preferences_pb) as sorted JSON.

Decodes androidx.datastore's PreferenceMap protobuf: field 1 holds map entries of key (1) and
Value (2), where Value is one of boolean, float, integer, long, string, string set, double or
bytes.

usage: datastore_summary.py <datastore-dir>
"""
import json
import struct
import sys
from pathlib import Path


def varint(buf, i):
    shift = result = 0
    while True:
        b = buf[i]
        i += 1
        result |= (b & 0x7F) << shift
        if not b & 0x80:
            return result, i
        shift += 7


def fields(buf):
    i = 0
    while i < len(buf):
        tag, i = varint(buf, i)
        number, wire = tag >> 3, tag & 7
        if wire == 0:
            value, i = varint(buf, i)
        elif wire == 1:
            value, i = buf[i:i + 8], i + 8
        elif wire == 2:
            length, i = varint(buf, i)
            value, i = buf[i:i + length], i + length
        elif wire == 5:
            value, i = buf[i:i + 4], i + 4
        else:
            raise ValueError(f"wire type {wire}")
        yield number, wire, value


def decode_value(buf):
    for number, wire, raw in fields(buf):
        if number == 1:
            return bool(raw)
        if number == 2:
            return struct.unpack("<f", raw)[0]
        if number in (3, 4):
            return raw - (1 << 64) if raw >= 1 << 63 else raw
        if number == 5:
            return raw.decode("utf-8", "replace")
        if number == 6:
            return sorted(v.decode("utf-8", "replace") for n, _, v in fields(raw) if n == 1)
        if number == 7:
            return struct.unpack("<d", raw)[0]
        if number == 8:
            return raw.hex()
    return None


def decode_file(path):
    out = {}
    for number, _, entry in fields(path.read_bytes()):
        if number != 1:
            continue
        key = value = None
        for n, _, raw in fields(entry):
            if n == 1:
                key = raw.decode("utf-8", "replace")
            elif n == 2:
                value = decode_value(raw)
        out[key] = value
    return out


def main():
    root = Path(sys.argv[1])
    summary = {p.name: decode_file(p) for p in sorted(root.glob("*.preferences_pb"))}
    print(json.dumps(summary, sort_keys=True, indent=1))


if __name__ == "__main__":
    main()
