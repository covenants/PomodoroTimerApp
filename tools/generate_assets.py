"""
Generate the Orbit Timer launcher icon, Play icon, feature graphic and store screenshots.

Run from the repository root:
    python tools/generate_assets.py

Requires Pillow. Outputs are deterministic and committed to the repo.
"""

from __future__ import annotations

import math
import os
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
PLAY = ROOT / "docs" / "play"
SCREENSHOTS = PLAY / "screenshots"
ANDROID = ROOT / "app" / "src" / "main" / "res"

TOP = (10, 21, 48)
BOTTOM = (17, 38, 78)
PRIMARY = (166, 189, 255)
ACCENT = (121, 232, 215)
TEXT = (246, 248, 255)
MUTED = (149, 166, 201)
FAINT = (100, 117, 155)

FONT_CANDIDATES = {
    "bold": ["C:/Windows/Fonts/segoeuib.ttf", "C:/Windows/Fonts/arialbd.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"],
    "semibold": ["C:/Windows/Fonts/seguisb.ttf", "C:/Windows/Fonts/arialbd.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"],
    "regular": ["C:/Windows/Fonts/segoeui.ttf", "C:/Windows/Fonts/arial.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"],
}


def font(kind: str, size: int) -> ImageFont.FreeTypeFont:
    for candidate in FONT_CANDIDATES[kind]:
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default()


def vertical_gradient(width: int, height: int, top=TOP, bottom=BOTTOM) -> Image.Image:
    image = Image.new("RGBA", (width, height))
    draw = ImageDraw.Draw(image)
    for y in range(height):
        t = y / max(1, height - 1)
        color = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        draw.line([(0, y), (width, y)], fill=color + (255,))
    return image


def add_glow(image: Image.Image, center: tuple[int, int], radius: int, color, alpha: int) -> Image.Image:
    layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    cx, cy = center
    draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(max(1, radius // 3)))
    return Image.alpha_composite(image, layer)


def add_stars(image: Image.Image, scale: float = 1.0) -> Image.Image:
    layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    w, h = image.size
    stars = [
        (0.09, 0.12, 2.0), (0.21, 0.07, 1.2), (0.34, 0.17, 1.6),
        (0.51, 0.08, 1.1), (0.68, 0.14, 1.8), (0.81, 0.05, 1.2),
        (0.93, 0.19, 2.1), (0.14, 0.29, 1.1), (0.88, 0.34, 1.4),
        (0.27, 0.42, 1.0), (0.73, 0.39, 1.0), (0.44, 0.30, 1.3),
    ]
    for x, y, r in stars:
        rr = r * scale
        draw.ellipse([w * x - rr, h * y - rr, w * x + rr, h * y + rr], fill=(220, 230, 255, 120))
    return Image.alpha_composite(image, layer)


def star_polygon(cx: float, cy: float, radius: float, inner: float) -> list[tuple[float, float]]:
    points = []
    for i in range(8):
        angle = -math.pi / 2 + i * math.pi / 4
        r = radius if i % 2 == 0 else inner
        points.append((cx + math.cos(angle) * r, cy + math.sin(angle) * r))
    return points


def draw_ring(image: Image.Image, size: int) -> Image.Image:
    draw = ImageDraw.Draw(image)
    c = size / 2
    radius = size * 0.292
    stroke = max(2, int(size * 0.030))
    bbox = [c - radius, c - radius, c + radius, c + radius]

    glow = Image.new("RGBA", image.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse(bbox, outline=PRIMARY + (46,), width=int(stroke * 2.4))
    glow = glow.filter(ImageFilter.GaussianBlur(max(1, int(size * 0.018))))
    image = Image.alpha_composite(image, glow)

    draw = ImageDraw.Draw(image)
    draw.ellipse(bbox, outline=PRIMARY + (255,), width=stroke)

    # Mint progress arc from 12 o'clock to roughly 4:30.
    arc_layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    ad = ImageDraw.Draw(arc_layer)
    ad.arc(bbox, start=-90, end=45, fill=ACCENT + (255,), width=int(stroke * 1.15))
    for angle in (-90, 45):
        rad = math.radians(angle)
        x = c + radius * math.cos(rad)
        y = c + radius * math.sin(rad)
        r = stroke * 0.58
        ad.ellipse([x - r, y - r, x + r, y + r], fill=ACCENT + (255,))
    image = Image.alpha_composite(image, arc_layer)

    end_rad = math.radians(45)
    end = (c + radius * math.cos(end_rad), c + radius * math.sin(end_rad))
    image = add_glow(image, (int(end[0]), int(end[1])), int(stroke * 1.8), ACCENT, 70)
    draw = ImageDraw.Draw(image)
    dot = stroke * 0.66
    draw.ellipse([end[0] - dot, end[1] - dot, end[0] + dot, end[1] + dot], fill=ACCENT + (255,))

    # Central planet.
    center_r = size * 0.055
    image = add_glow(image, (int(c), int(c)), int(center_r * 2.4), (210, 222, 255), 55)
    draw = ImageDraw.Draw(image)
    draw.ellipse([c - center_r, c - center_r, c + center_r, c + center_r], fill=TEXT + (255,))

    # A small four-point star at the upper right.
    star_c = (size * 0.735, size * 0.245)
    image = add_glow(image, (int(star_c[0]), int(star_c[1])), int(size * 0.035), TEXT, 70)
    draw = ImageDraw.Draw(image)
    draw.polygon(star_polygon(star_c[0], star_c[1], size * 0.028, size * 0.009), fill=TEXT + (245,))
    return image


def full_icon(size: int = 1024) -> Image.Image:
    supersample = 2
    s = size * supersample
    image = vertical_gradient(s, s)
    image = add_glow(image, (int(s * 0.78), int(s * 0.18)), int(s * 0.40), ACCENT, 38)
    image = add_glow(image, (int(s * 0.18), int(s * 0.22)), int(s * 0.32), PRIMARY, 22)
    image = add_stars(image, scale=supersample)
    image = draw_ring(image, s)
    return image.resize((size, size), Image.LANCZOS)


def adaptive_background(size: int = 432) -> Image.Image:
    image = vertical_gradient(size, size)
    image = add_glow(image, (int(size * 0.78), int(size * 0.18)), int(size * 0.42), ACCENT, 42)
    image = add_glow(image, (int(size * 0.18), int(size * 0.22)), int(size * 0.32), PRIMARY, 22)
    image = add_stars(image, scale=1.0)
    return image


def adaptive_foreground(size: int = 432) -> Image.Image:
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    return draw_ring(image, size)


def rounded_icon(image: Image.Image, size: int, radius_ratio: float) -> Image.Image:
    result = image.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * radius_ratio), fill=255)
    result.putalpha(mask)
    return result


def circular_icon(image: Image.Image, size: int) -> Image.Image:
    result = image.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    result.putalpha(mask)
    return result


def wrap_text(draw: ImageDraw.ImageDraw, text: str, fnt: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        trial = (current + " " + word).strip()
        if draw.textlength(trial, font=fnt) <= max_width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def store_screenshot(raw: Path, headline: str, subline: str, out: Path) -> None:
    width, height = 1080, 1920
    image = vertical_gradient(width, height)
    image = add_glow(image, (int(width * 0.82), int(height * 0.08)), 480, PRIMARY, 26)
    image = add_glow(image, (int(width * 0.12), int(height * 0.22)), 360, ACCENT, 20)
    image = add_stars(image, scale=2.0)

    draw = ImageDraw.Draw(image)
    eyebrow_font = font("semibold", 24)
    headline_font = font("bold", 64)
    subline_font = font("regular", 29)

    draw.text((72, 78), "ORBIT TIMER", font=eyebrow_font, fill=ACCENT + (255,))

    lines = wrap_text(draw, headline, headline_font, width - 144)
    y = 128
    for line in lines:
        draw.text((72, y), line, font=headline_font, fill=TEXT + (255,))
        y += 74
    y += 8
    for line in wrap_text(draw, subline, subline_font, width - 144):
        draw.text((72, y), line, font=subline_font, fill=MUTED + (255,))
        y += 38

    phone_width = 660
    phone_path = raw
    with Image.open(phone_path) as raw_image:
        phone = raw_image.convert("RGB")
        phone = phone.resize((phone_width, int(phone_width * phone.height / phone.width)), Image.LANCZOS)

    x = (width - phone.width) // 2
    y = 430
    shadow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle(
        [x - 18, y + 18, x + phone.width + 18, y + phone.height + 18],
        radius=50,
        fill=(0, 0, 0, 120),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(28))
    image = Image.alpha_composite(image, shadow)

    masked = Image.new("RGBA", phone.size, (0, 0, 0, 0))
    masked.paste(phone, (0, 0))
    mask = Image.new("L", phone.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, phone.width - 1, phone.height - 1], radius=42, fill=255)
    masked.putalpha(mask)
    image.alpha_composite(masked, (x, y))

    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle(
        [x, y, x + phone.width - 1, y + phone.height - 1],
        radius=42,
        outline=(42, 63, 98, 220),
        width=2,
    )
    image.convert("RGB").save(out, "PNG", optimize=True)


def feature_graphic(icon: Image.Image) -> Image.Image:
    width, height = 1024, 500
    image = vertical_gradient(width, height)
    image = add_glow(image, (820, 40), 420, PRIMARY, 30)
    image = add_glow(image, (120, 420), 300, ACCENT, 18)
    image = add_stars(image, scale=1.4)

    ring_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring_layer)
    rd.ellipse([690, -130, 1250, 430], outline=PRIMARY + (34,), width=22)
    rd.arc([690, -130, 1250, 430], start=-90, end=45, fill=ACCENT + (50,), width=24)
    image = Image.alpha_composite(image, ring_layer)

    icon_size = 250
    icon_rounded = rounded_icon(icon, icon_size, 0.22)
    image.alpha_composite(icon_rounded, (72, 125))

    draw = ImageDraw.Draw(image)
    draw.text((376, 142), "Orbit Timer", font=font("bold", 72), fill=TEXT + (255,))
    draw.text((382, 232), "Focus. Breathe. Repeat.", font=font("semibold", 34), fill=ACCENT + (255,))
    draw.text((382, 292), "Pomodoro · Countdown · Stopwatch", font=font("regular", 24), fill=MUTED + (255,))

    pill = "OFFLINE  ·  NO ACCOUNT"
    pill_font = font("semibold", 20)
    pill_width = int(draw.textlength(pill, font=pill_font)) + 44
    draw.rounded_rectangle([382, 354, 382 + pill_width, 402], radius=24, fill=ACCENT + (255,))
    draw.text((404, 367), pill, font=pill_font, fill=TOP + (255,))
    return image.convert("RGB")


def main() -> None:
    PLAY.mkdir(parents=True, exist_ok=True)
    SCREENSHOTS.mkdir(parents=True, exist_ok=True)
    for density, pixels in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        (ANDROID / f"mipmap-{density}").mkdir(parents=True, exist_ok=True)
    (ANDROID / "drawable-nodpi").mkdir(parents=True, exist_ok=True)

    icon = full_icon(1024)
    icon.save(PLAY / "icon-1024.png", "PNG", optimize=True)
    icon.resize((512, 512), Image.LANCZOS).save(PLAY / "icon-512.png", "PNG", optimize=True)

    for density, pixels in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        rounded_icon(icon, pixels, 0.22).save(ANDROID / f"mipmap-{density}" / "ic_launcher.png", "PNG", optimize=True)
        circular_icon(icon, pixels).save(ANDROID / f"mipmap-{density}" / "ic_launcher_round.png", "PNG", optimize=True)

    adaptive_background(432).save(ANDROID / "drawable-nodpi" / "ic_launcher_bg.png", "PNG", optimize=True)
    adaptive_foreground(432).save(ANDROID / "drawable-nodpi" / "ic_launcher_fg.png", "PNG", optimize=True)

    feature_graphic(icon).save(PLAY / "feature-graphic-1024x500.png", "PNG", optimize=True)

    shots = [
        ("01-home.png", "01-home.png", "Make time count.", "One tap. Focus, countdown or stopwatch."),
        ("02-active-focus.png", "02-active-focus.png", "Stay in your orbit.", "A calm focus screen that survives real life."),
        ("03-quick-timer.png", "03-quick-timer.png", "Any countdown.", "Presets, direct entry and +5 minutes."),
        ("02b-break-ready.png", "04-break-ready.png", "Breaks that wait.", "Start now, later, or skip — your choice."),
        ("05-tasks.png", "05-tasks.png", "One thing at a time.", "Optional tasks, linked to your focus."),
        ("06-insights.png", "06-insights.png", "Progress you can see.", "Week, month, year — all on your device."),
        ("08-constellations.png", "07-constellations.png", "Your orbit, your pace.", "A gentle visual that never punishes."),
        ("07-settings.png", "08-themes.png", "Make it yours.", "Three themes, reduced motion, high contrast."),
    ]
    for raw_name, out_name, headline, subline in shots:
        raw = ROOT / "docs" / "screenshots" / raw_name
        if not raw.exists():
            raise FileNotFoundError(f"Missing source screenshot: {raw}")
        store_screenshot(raw, headline, subline, SCREENSHOTS / out_name)

    print("Generated icon, feature graphic and store screenshots.")
    print(f"Play assets: {PLAY}")
    print(f"Adaptive layers: {ANDROID / 'drawable-nodpi'}")


if __name__ == "__main__":
    main()
