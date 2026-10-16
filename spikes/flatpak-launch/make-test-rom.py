"""Writes a 32 KiB Game Boy ROM that only loops: a self-made, freely redistributable test file.

No boot logo is included (the Nintendo logo bytes are not ours to ship); libretro cores skip the
boot ROM check by default. The header checksum is set so cores do not warn about it.

usage: python3 -I make-test-rom.py <out.gb>
"""
import sys

rom = bytearray(32 * 1024)
rom[0x100:0x104] = bytes([0x00, 0xC3, 0x50, 0x01])  # nop; jp $0150
rom[0x134:0x143] = b"ARGOSY SPIKE".ljust(15, b"\0")  # title
rom[0x147] = 0x00                                      # cartridge: ROM only
rom[0x148] = 0x00                                      # 32 KiB
rom[0x14A] = 0x01                                      # non-Japanese
rom[0x150:0x152] = bytes([0x18, 0xFE])                 # jr -2 (loop forever)
chk = 0
for b in rom[0x134:0x14D]:
    chk = (chk - b - 1) & 0xFF
rom[0x14D] = chk
total = (sum(rom) - rom[0x14E] - rom[0x14F]) & 0xFFFF
rom[0x14E:0x150] = total.to_bytes(2, "big")
open(sys.argv[1], "wb").write(rom)
