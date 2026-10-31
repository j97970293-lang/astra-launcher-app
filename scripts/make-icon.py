#!/usr/bin/env python3
"""make-icon.py — régénère l'icône du lanceur : mipmaps PNG + vecteur adaptatif.

    python3 scripts/make-icon.py            # écrit app/src/main/res/{mipmap-*,drawable}

L'icône est un astre à quatre branches, frontière de |x|^p + |y|^p = a^p avec
p = 0,70 (p < 1 : branches concaves), tracé en `astra_accent` sur le fond
`astra_background`. Le même tracé alimente les deux sorties, pour que le
lanceur affiche exactement la même forme avant et après l'API 26 :

  * `drawable/ic_launcher_foreground.xml` — icône adaptative, canevas 108 dp,
    rayon 0,32 (34,6 dp) : la pointe reste dans le cercle de sûreté de 36 dp ;
  * `mipmap-*/ic_launcher*.png` — anciens lanceurs, qui masquent aussi le
    carré ; le motif y garde la même marge.

Les fichiers sont réécrits à chaque exécution, jamais modifiés à la main.
"""

from __future__ import annotations

import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

POWER = 0.70            # p < 1 : branches concaves
RADIUS = 0.32           # fraction du côté, pointe de l'astre
FOREGROUND = "#8EA7FF"  # astra_accent
BACKGROUND_RGB = (11, 16, 32)    # astra_background
FOREGROUND_RGB = (142, 167, 255)
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
VIEWPORT = 108          # canevas d'une icône adaptative
SUPERSAMPLE = 4
VECTOR_STEP = 4         # degrés entre deux points du tracé vectoriel


def star(radius: float, centre: float, steps: int) -> list[tuple[float, float]]:
    """Le contour de l'astre : une pointe sur chaque axe, centre en (c, c)."""
    exponent = 2.0 / POWER
    quadrant = []
    for i in range(steps // 4 + 1):
        t = (math.pi / 2) * i / (steps // 4)
        quadrant.append((radius * math.cos(t) ** exponent,
                         radius * math.sin(t) ** exponent))
    points = []
    for sign_x, sign_y in ((1, 1), (-1, 1), (-1, -1), (1, -1)):
        for x, y in quadrant:
            points.append((centre + sign_x * x, centre + sign_y * y))
    return points


def number(value: float) -> str:
    return ("%.2f" % value).rstrip("0").rstrip(".")


def write_vector() -> str:
    steps = 360 // VECTOR_STEP
    points = star(VIEWPORT * RADIUS, VIEWPORT / 2.0, steps)
    path = "M%s,%s" % (number(points[0][0]), number(points[0][1]))
    path += "".join("L%s,%s" % (number(x), number(y)) for x, y in points[1:])
    path += "Z"
    content = """<?xml version="1.0" encoding="utf-8"?>
<!-- Généré par scripts/make-icon.py — ne pas éditer à la main. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="%s"
        android:pathData="%s" />
</vector>
""" % (FOREGROUND, path)
    target = os.path.join(RES, "drawable", "ic_launcher_foreground.xml")
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, "w", encoding="utf-8") as fh:
        fh.write(content)
    return os.path.relpath(target, ROOT)


def write_pngs() -> list[str]:
    written = []
    for density, size in SIZES.items():
        scale = SUPERSAMPLE
        big = size * scale
        image = Image.new("RGBA", (big, big), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        draw.rounded_rectangle((0, 0, big - 1, big - 1),
                               radius=int(big * 0.22), fill=BACKGROUND_RGB)
        draw.polygon(star(big * RADIUS, big / 2.0, 720), fill=FOREGROUND_RGB)
        icon = image.resize((size, size), Image.LANCZOS)
        folder = os.path.join(RES, "mipmap-%s" % density)
        os.makedirs(folder, exist_ok=True)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            path = os.path.join(folder, name)
            icon.save(path, "PNG", optimize=True)
            written.append(os.path.relpath(path, ROOT))
    return written


def main() -> int:
    print("écrit %s" % write_vector())
    for path in write_pngs():
        print("écrit %s" % path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
