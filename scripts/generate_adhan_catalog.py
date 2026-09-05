#!/usr/bin/env python3
"""يولّد app/src/main/assets/adhan_catalog.json من صفحة الأذان في موقع إسلام ويب:
https://audio.islamweb.net/audio/index.php?page=AudioGroup&Gtype=1
كل عنصر: id، اسم المؤذن، النوع (أذان/أذان الفجر)، المكان، رابط mp3 المباشر.
"""
import html
import json
import os
import re
import urllib.request

URL = "https://audio.islamweb.net/audio/index.php?page=AudioGroup&Gtype=1"


def main():
    req = urllib.request.Request(URL, headers={"User-Agent": "Mozilla/5.0"})
    page = urllib.request.urlopen(req, timeout=60).read().decode("utf-8", "ignore")
    entries = []
    for block in re.split(r'<div class="rwayabar[^"]*">', page)[1:]:
        m = re.search(r"<h1><a[^>]*>(.*?)</a>", block, flags=re.S)
        reader = html.unescape(re.sub(r"<[^>]+>", "", m.group(1))).strip() if m else "غير معروف"
        for it in re.finditer(r'data-mp3="([^"]+)"[^>]*/>.*?audioid=(\d+)">(.*?)</a>', block, flags=re.S):
            label = html.unescape(re.sub(r"<[^>]+>", "", it.group(3))).strip()
            parts = [p.strip() for p in label.split(",")]
            entries.append({
                "id": int(it.group(2)),
                "reader": reader,
                "kind": parts[0],
                "place": " - ".join(parts[1:]),
                "url": it.group(1),
            })
    out = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                       "app", "src", "main", "assets", "adhan_catalog.json")
    with open(out, "w", encoding="utf-8") as f:
        json.dump(entries, f, ensure_ascii=False, separators=(",", ":"))
    print(f"wrote {len(entries)} entries -> {out}")


if __name__ == "__main__":
    main()
