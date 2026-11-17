#!/usr/bin/env python3
"""Frame raw device screenshots with the ink caption band.

Reads playstore/screenshots/raw-N-*.png, writes framed-N-*.png at
1080x1920 (9:16, 24-bit, no alpha) ready for Play Console upload.

Run: python3 playstore/screenshots/frame-shot.py
"""
import glob
import os

from PIL import Image, ImageDraw, ImageFont

INK = (15, 23, 42)
MINT = (0, 241, 206)
WHITE = (255, 255, 255)
W, H = 1080, 1920
BAND = 320

CAPTIONS = {
    1: ("Every room ready, at a glance", False),
    2: ("Works without internet", False),
    3: ("Your whole floor, live", False),
    4: ("Eight steps. Zero misses.", False),
    5: ("Inspect and pass in seconds", False),
    6: ("সম্পূর্ণ বাংলায়", True),
}

OUT = os.path.dirname(os.path.abspath(__file__))


def font_for(bengali, size):
    paths = (
        ["/System/Library/Fonts/KohinoorBangla.ttc",
         "/System/Library/Fonts/Bangla Sangam MN.ttc"]
        if bengali
        else ["/Library/Fonts/Arial Bold.ttf", "/Library/Fonts/Arial.ttf"]
    )
    for path in paths:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            pass
    raise SystemExit("no usable font for caption")


def frame(raw_path, number, caption, bengali):
    shot = Image.open(raw_path).convert("RGB")
    # Cover-fit into the area below the band.
    target_w, target_h = W, H - BAND
    scale = max(target_w / shot.width, target_h / shot.height)
    shot = shot.resize((int(shot.width * scale + 0.5), int(shot.height * scale + 0.5)),
                       Image.LANCZOS)
    left = (shot.width - target_w) // 2
    top = 0  # keep the status bar / top content; crop the bottom
    shot = shot.crop((left, top, left + target_w, top + target_h))

    canvas = Image.new("RGB", (W, H), INK)
    canvas.paste(shot, (0, BAND))
    draw = ImageDraw.Draw(canvas)

    size = 64
    while size > 20:
        f = font_for(bengali, size)
        box = draw.textbbox((0, 0), caption, font=f)
        if box[2] - box[0] <= W - 120:
            break
        size -= 2
    box = draw.textbbox((0, 0), caption, font=f)
    tw, th = box[2] - box[0], box[3] - box[1]
    # Rule above, caption vertically centered in the remaining band.
    rule_y = BAND // 2 - th // 2 - 34
    draw.rounded_rectangle([(W - 120) // 2, rule_y, (W + 120) // 2, rule_y + 8],
                           radius=4, fill=MINT)
    draw.text(((W - tw) // 2 - box[0], BAND // 2 - th // 2 - box[1] + 22),
              caption, font=f, fill=WHITE)

    name = "framed-%d-%s.png" % (number, os.path.basename(raw_path)[6:-4])
    out = os.path.join(OUT, name)
    canvas.save(out)
    print("wrote", out)


def main():
    raws = sorted(glob.glob(os.path.join(OUT, "raw-*-*.png")))
    if not raws:
        raise SystemExit("no raw screenshots — run capture-screenshots.sh first")
    for raw in raws:
        number = int(os.path.basename(raw).split("-")[1])
        caption, bengali = CAPTIONS[number]
        frame(raw, number, caption, bengali)


if __name__ == "__main__":
    main()
