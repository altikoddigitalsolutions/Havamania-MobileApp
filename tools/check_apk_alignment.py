"""Inspect ELF LOAD alignment of 64-bit libraries packaged in an APK.

Usage: python tools/check_apk_alignment.py path/to/app.apk
This complements zipalign; it does not replace testing on a 16 KB device.
"""
import json
import struct
import sys
import zipfile


def inspect(apk):
    results = []
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if not name.endswith(".so") or not name.startswith(("lib/arm64-v8a/", "lib/x86_64/")):
                continue
            data = archive.read(name)
            if data[:5] != b"\x7fELF\x02":
                raise ValueError(f"Invalid ELF64 library: {name}")
            endian = "<" if data[5] == 1 else ">"
            offset = struct.unpack_from(endian + "Q", data, 32)[0]
            size, count = struct.unpack_from(endian + "HH", data, 54)
            alignments = []
            for index in range(count):
                header = offset + index * size
                if struct.unpack_from(endian + "I", data, header)[0] == 1:
                    alignments.append(struct.unpack_from(endian + "Q", data, header + 48)[0])
            results.append({"library": name, "load_alignment": alignments,
                            "supports_16kb_alignment": bool(alignments) and min(alignments) >= 16384})
    return results


if __name__ == "__main__":
    results = inspect(sys.argv[1])
    print(json.dumps(results, indent=2))
    sys.exit(0 if all(item["supports_16kb_alignment"] for item in results) else 1)
