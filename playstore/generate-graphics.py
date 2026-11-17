#!/usr/bin/env python3
"""Regenerate RoomReady Play Store graphics from brand tokens.

Outputs (all 24-bit, no alpha, per Play specs):
  icon-512.png        512x512   Play app icon (full-bleed, masking applied by Play)
  feature-graphic.png 1024x500  Play feature graphic
  banner-1200x630.png 1200x630  Social / repo banner

Run: python3 playstore/generate-graphics.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

INK = (15, 23, 42)
INK_DEEP = (9, 17, 36)
SLATE = (51, 65, 85)
MINT_START = (0, 255, 148)
MINT_END = (0, 238, 218)
MINT_SOLID = (0, 241, 206)
WHITE = (255, 255, 255)
GRAY = (148, 163, 184)
WARM = (250, 248, 244)

OUT = os.path.dirname(os.path.abspath(__file__))
SUPERSAMPLE = 4


def font(bold, size):
    candidates = (
        ["/Library/Fonts/Arial Bold.ttf", "/Library/Fonts/Arial.ttf"]
        if bold
        else ["/Library/Fonts/Arial.ttf"]
    )
    for path in candidates:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            pass
    raise SystemExit("no usable font found (tried %s)" % ", ".join(candidates))


def lerp(a, b, t):
    return tuple(int(x + (y - x) * t) for x, y in zip(a, b))


def draw_door(img, ox, oy, s):
    """Ready Door mark in a 108-unit viewport scaled by s at offset (ox, oy)."""
    # Mint glow behind the door: stacked translucent discs, composited once.
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx, cy, r = ox + 58 * s, oy + 54 * s, 46 * s
    for i in range(14, 0, -1):
        rr = r * i / 14
        gd.ellipse([cx - rr, cy - rr, cx + rr, cy + rr],
                   fill=MINT_START + (int(30 * (1 - i / 15)),))
    img.paste(Image.alpha_composite(img.convert("RGBA"), glow).convert("RGB"))

    draw = ImageDraw.Draw(img)

    def p(x, y):
        return (ox + x * s, oy + y * s)

    # Doorway frame.
    x0, y0 = p(36, 28)
    x1, y1 = p(69, 80)
    draw.rounded_rectangle([x0, y0, x1, y1], radius=3 * s, fill=INK_DEEP,
                           outline=SLATE, width=max(1, int(1.6 * s)))
    # Light spill in the opening.
    draw.polygon([p(53, 29), p(68, 29), p(68, 79), p(53, 79)], fill=(0, 60, 52))
    # White slab, slightly ajar.
    slab = [p(34.5, 29), p(59, 34), p(59, 74), p(34.5, 79)]
    draw.polygon(slab, fill=WHITE)
    draw.line(slab + [slab[0]], fill=INK, width=max(1, int(2.4 * s)), joint="curve")
    # Latch.
    lx, ly = p(56.3, 54)
    lr = 1.8 * s
    draw.ellipse([lx - lr, ly - lr, lx + lr, ly + lr], fill=INK)
    # Check: ink halo under, mint gradient over (two-tone segments).
    a, b, c = p(47.5, 54), p(56.5, 63), p(75.5, 41.5)
    draw.line([a, b, c], fill=INK, width=int(8.8 * s), joint="curve")
    mid1 = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
    mid2 = ((b[0] + c[0]) / 2, (b[1] + c[1]) / 2)
    w = max(1, int(5.0 * s))
    draw.line([a, mid1], fill=MINT_START, width=w, joint="curve")
    draw.line([mid1, b, mid2], fill=MINT_SOLID, width=w, joint="curve")
    draw.line([mid2, c], fill=MINT_END, width=w, joint="curve")
    for pt in (a, b, c, mid1, mid2):
        draw.ellipse([pt[0] - w / 2, pt[1] - w / 2, pt[0] + w / 2, pt[1] + w / 2],
                     fill=MINT_SOLID)


def finish(img, size, name):
    img = img.resize(size, Image.LANCZOS).convert("RGB")
    path = os.path.join(OUT, name)
    img.save(path)
    print("wrote", path, img.size)


def make_icon():
    s = SUPERSAMPLE
    img = Image.new("RGB", (512 * s, 512 * s), INK)
    # Full-bleed glow wash.
    wash = Image.new("RGBA", img.size, (0, 0, 0, 0))
    wd = ImageDraw.Draw(wash)
    wd.ellipse([60 * s, 60 * s, 452 * s, 452 * s], fill=MINT_END + (14,))
    img.paste(Image.alpha_composite(img.convert("RGBA"), wash).convert("RGB"))
    # Door mark: viewport 108 mapped edge-to-edge (Play applies masking).
    draw_door(img, 0, 0, 512 * s / 108)
    finish(img, (512, 512), "icon-512.png")


def fit_font(draw, text, bold, max_width, start_size):
    size = start_size
    while size > 8:
        f = font(bold, size)
        box = draw.textbbox((0, 0), text, font=f)
        if box[2] - box[0] <= max_width:
            return f
        size -= 2
    return font(bold, 8)


def lockup(draw, x, max_w, y_top, sub_text, sub_color=GRAY):
    """Wordmark, mint rule, sub line stacked. Returns the y below the block."""
    wf = fit_font(draw, "RoomReady", True, max_w, 400)
    draw.text((x, y_top), "RoomReady", font=wf, fill=WHITE)
    asc, _ = wf.getmetrics()
    size = wf.size
    top = y_top + asc + size // 10
    rule_h = max(4, size // 16)
    rule_w = max_w // 4
    draw.rounded_rectangle([x, top, x + rule_w, top + rule_h],
                           radius=rule_h // 2, fill=MINT_SOLID)
    sf = fit_font(draw, sub_text, True, max_w, size // 3)
    sbox = draw.textbbox((0, 0), sub_text, font=sf)
    ty = top + rule_h + size // 10 - sbox[1]
    draw.text((x, ty), sub_text, font=sf, fill=sub_color)
    return ty + (sbox[3] - sbox[1]) + size // 8


def make_feature():
    w, h, s = 1024, 500, SUPERSAMPLE
    img = Image.new("RGB", (w * s, h * s), INK)
    unit = h * s / 108
    draw_door(img, 30 * s, (h * s - 108 * unit) / 2, unit)
    draw = ImageDraw.Draw(img)
    lockup(draw, int(545 * s), int((1024 - 545 - 50) * s), int(150 * s),
           "EVERY ROOM READY.")
    finish(img, (w, h), "feature-graphic.png")


def make_banner():
    w, h, s = 1200, 630, SUPERSAMPLE
    img = Image.new("RGB", (w * s, h * s), INK)
    unit = h * s / 128
    draw_door(img, 60 * s, (h * s - 108 * unit) / 2, unit)
    draw = ImageDraw.Draw(img)
    tx = int(660 * s)
    maxw = int((1200 - 660 - 50) * s)
    bottom = lockup(draw, tx, maxw, int(185 * s),
                    "OFFLINE-FIRST RESORT HOUSEKEEPING")
    ff = fit_font(draw, "Dirty  →  Cleaning  →  Cleaned  →  Ready", False, maxw, 34 * s)
    draw.text((tx, bottom), "Dirty  →  Cleaning  →  Cleaned  →  Ready",
              font=ff, fill=WARM)
    finish(img, (w, h), "banner-1200x630.png")


if __name__ == "__main__":
    make_icon()
    make_feature()
    make_banner()
