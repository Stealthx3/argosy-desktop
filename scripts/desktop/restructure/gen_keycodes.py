#!/usr/bin/env python3
"""
Generates AndroidKeyCodes.kt: every KEYCODE_*, ACTION_*, META_* and FLAG_* constant of android.view.KeyEvent, read from the
platform android.jar with javap, so shared code and the desktop input layer use AOSP's values.

usage: gen_keycodes.py <android.jar> <output.kt>
"""
import re
import subprocess
import sys

jar, out = sys.argv[1], sys.argv[2]
listing = subprocess.run(
    ["javap", "-constants", "-cp", jar, "android.view.KeyEvent"],
    check=True, capture_output=True, text=True,
).stdout
codes = re.findall(r"public static final int (KEYCODE_\w+|ACTION_\w+|META_\w+|FLAG_\w+) = (-?\d+);", listing)
lines = [
    "package com.nendo.argosy.core.input",
    "",
    "object AndroidKeyCodes {",
    *(f"    const val {name}: Int = {value}" for name, value in codes),
    "}",
    "",
]
with open(out, "w") as f:
    f.write("\n".join(lines))
print(f"{len(codes)} key codes -> {out}")
