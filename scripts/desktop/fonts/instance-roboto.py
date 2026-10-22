#!/usr/bin/env python3
"""Cut Android's variable Roboto into the static weights Argosy Desktop bundles.

Source: /system/fonts/Roboto-Regular.ttf from the API 35 check AVD (Roboto 3.005, Apache 2.0),
pulled with `adb -s emulator-5556 pull /system/fonts/Roboto-Regular.ttf`. Android's fonts.xml maps
sans-serif to that file at wght 100..900, wdth 100, ital 0 and 1; this writes the same 18 instances.

Usage: instance-roboto.py <Roboto-Regular.ttf> <out-dir>
"""
import sys
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

NAMES = {100: "Thin", 200: "ExtraLight", 300: "Light", 400: "Regular", 500: "Medium",
         600: "SemiBold", 700: "Bold", 800: "ExtraBold", 900: "Black"}


def main(src: str, out: str) -> None:
    Path(out).mkdir(parents=True, exist_ok=True)
    for ital in (0, 1):
        for weight, name in NAMES.items():
            font = instancer.instantiateVariableFont(
                TTFont(src), {"wght": weight, "wdth": 100, "ital": ital}, updateFontNames=False)
            label = name + ("Italic" if ital else "")
            font.save(f"{out}/Roboto-{'Italic' if label == 'RegularItalic' else label}.ttf")


if __name__ == "__main__":
    main(*sys.argv[1:3])
