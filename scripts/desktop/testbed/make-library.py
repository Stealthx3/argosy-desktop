"""Writes a tiny RomM-shaped library of self-made ROMs (freely redistributable, no third-party code or art).

Each ROM only loops forever; it exists so RomM has files to scan and Argosy has games to list.
Layout is RomM structure A: <out>/roms/<fs_slug>/<file>.

The battery titles are Game Boy MBC1+RAM+BATTERY carts (type 0x03, 8 KiB RAM) so a core writes a
real SRAM file. Their RAM layout: $A001 = 0x5A once initialised, $A002 = a per-title id, $A000 = a
boot counter. "counter" carts add one to $A000 on every boot, so each play session changes the save
by exactly one byte, deterministically; "passive" carts initialise once and never write again, so
quitting one leaves its save unchanged.

They also carry the first four header-logo bytes (0xCE 0xED 0x66 0x66) at 0x104: mGBA, the built-in
emulator's default Game Boy core, identifies a Game Boy ROM by exactly those four bytes and refuses
the file otherwise. The rest of the logo is not included.

usage: python3 -I make-library.py <out-dir>
"""
import pathlib
import sys

GAMES = {
    "gb": ["Argosy Test Quest", "Harbor Lights", "Lantern Keeper", "Tidepool Run"],
    "gbc": ["Argosy Color Trial", "Coral Garden", "Signal Fire"],
    "gba": ["Argosy Advance Demo", "Driftwood Racer", "Star Chart"],
    "nes": ["Argosy Eight Bit", "Lighthouse Climb", "Rope and Anchor"],
}


def gb(title, color):
    rom = bytearray(32 * 1024)
    rom[0x100:0x104] = bytes([0x00, 0xC3, 0x50, 0x01])
    rom[0x134:0x143] = title.upper().encode()[:15].ljust(15, b"\0")
    rom[0x143] = 0x80 if color else 0x00
    rom[0x14A] = 0x01
    rom[0x150:0x152] = bytes([0x18, 0xFE])
    chk = 0
    for b in rom[0x134:0x14D]:
        chk = (chk - b - 1) & 0xFF
    rom[0x14D] = chk
    total = (sum(rom) - rom[0x14E] - rom[0x14F]) & 0xFFFF
    rom[0x14E:0x150] = total.to_bytes(2, "big")
    return bytes(rom)


BATTERY = [
    ("Battery Bay", "counter", 0x01),
    ("Save Point Harbor", "counter", 0x02),
    ("Ledger Isle", "passive", 0x03),
]


def gb_battery(title, mode, title_id):
    rom = bytearray(32 * 1024)
    rom[0x100:0x104] = bytes([0x00, 0xC3, 0x50, 0x01])
    rom[0x134:0x143] = title.upper().encode()[:15].ljust(15, b"\0")
    rom[0x104:0x108] = bytes([0xCE, 0xED, 0x66, 0x66])
    rom[0x147] = 0x03
    rom[0x149] = 0x02
    rom[0x14A] = 0x01
    init = bytes([
        0x3E, 0x5A, 0xEA, 0x01, 0xA0,
        0x3E, title_id, 0xEA, 0x02, 0xA0,
        0xAF, 0xEA, 0x00, 0xA0,
    ])
    code = bytes([
        0x3E, 0x0A, 0xEA, 0x00, 0x00,
        0xFA, 0x01, 0xA0,
        0xFE, 0x5A,
        0x28, len(init),
    ]) + init
    if mode == "counter":
        code += bytes([0x21, 0x00, 0xA0, 0x34])
    code += bytes([0x18, 0xFE])
    rom[0x150:0x150 + len(code)] = code
    chk = 0
    for b in rom[0x134:0x14D]:
        chk = (chk - b - 1) & 0xFF
    rom[0x14D] = chk
    total = (sum(rom) - rom[0x14E] - rom[0x14F]) & 0xFFFF
    rom[0x14E:0x150] = total.to_bytes(2, "big")
    return bytes(rom)


def sram(title_id, counter):
    """The 8 KiB SRAM a battery cart above has after `counter` boots (what a core writes on exit)."""
    ram = bytearray(8 * 1024)
    ram[0] = counter & 0xFF
    ram[1] = 0x5A
    ram[2] = title_id
    return bytes(ram)


def gba(title):
    rom = bytearray(256 * 1024)
    rom[0:4] = (0xEA00002E).to_bytes(4, "little")
    rom[0xA0:0xAC] = title.upper().encode()[:12].ljust(12, b"\0")
    rom[0xAC:0xB0] = b"AARG"
    rom[0xB0:0xB2] = b"00"
    rom[0xB2] = 0x96
    rom[0xC0:0xC4] = (0xEAFFFFFE).to_bytes(4, "little")
    rom[0xBD] = (-(sum(rom[0xA0:0xBD]) + 0x19)) & 0xFF
    return bytes(rom)


def nes(title):
    header = b"NES\x1a" + bytes([1, 1, 0, 0]) + bytes(8)
    prg = bytearray(16 * 1024)
    prg[0:3] = bytes([0x4C, 0x00, 0xC0])
    name = title.encode()[:32]
    prg[0x100:0x100 + len(name)] = name
    prg[0x3FFA:0x4000] = bytes([0x00, 0xC0, 0x00, 0xC0, 0x00, 0xC0])
    chr_rom = bytes(8 * 1024)
    return header + bytes(prg) + chr_rom


def main():
    out = pathlib.Path(sys.argv[1])
    for slug, titles in GAMES.items():
        folder = out / "roms" / slug
        folder.mkdir(parents=True, exist_ok=True)
        for title in titles:
            if slug in ("gb", "gbc"):
                data = gb(title, slug == "gbc")
            elif slug == "gba":
                data = gba(title)
            else:
                data = nes(title)
            (folder / f"{title} (Homebrew).{slug}").write_bytes(data)
    for title, mode, title_id in BATTERY:
        (out / "roms" / "gb" / f"{title} (Homebrew).gb").write_bytes(gb_battery(title, mode, title_id))
    print(out)


if __name__ == "__main__":
    main()
