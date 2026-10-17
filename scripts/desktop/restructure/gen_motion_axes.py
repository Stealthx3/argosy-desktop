#!/usr/bin/env python3
"""
Generates AndroidMotionAxes.kt: every AXIS_* and ACTION_* constant of android.view.MotionEvent, read from the
platform android.jar with javap, so shared code and the desktop input layer use AOSP's values.

usage: gen_motion_axes.py <android.jar> <output.kt>
"""
import re
import subprocess
import sys

jar, out = sys.argv[1], sys.argv[2]
listing = subprocess.run(
    ["javap", "-constants", "-cp", jar, "android.view.MotionEvent"],
    check=True, capture_output=True, text=True,
).stdout
axes = re.findall(r"public static final int (AXIS_\w+|ACTION_\w+) = (-?\d+);", listing)
lines = [
    "package com.nendo.argosy.core.input",
    "",
    "object AndroidMotionAxes {",
    *(f"    const val {name}: Int = {value}" for name, value in axes),
    "}",
    "",
]
with open(out, "w") as f:
    f.write("\n".join(lines))
print(f"{len(axes)} axes -> {out}")
