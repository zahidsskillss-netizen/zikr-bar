"""Renders the tasbih counter bodies (round discs and finger-ring counters) as PNGs.

The count digits are NOT drawn here; the app draws them live (7-segment style).
usage: python3 render_counters.py OUT_DIR
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.spatial import Delaunay

OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
os.makedirs(OUT, exist_ok=True)
K = 2  # supersampling
rng = np.random.default_rng(7)


# ------------------------------------------------------------ helpers
def noise(w, h, scale, seed):
    r = np.random.default_rng(seed)
    gw, gh = max(2, int(w / scale) + 2), max(2, int(h / scale) + 2)
    g = r.random((gh, gw)).astype(np.float32)
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
    return np.asarray(img).astype(np.float32) / 255.0


def fbm(w, h, seed, octaves=5, base=120):
    acc = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        acc += noise(w, h, base / (2 ** o), seed + o) * amp
        tot += amp
        amp *= 0.5
    return acc / tot


def colorize(t, stops):
    """t in 0..1 -> RGB via (pos, (r,g,b)) stops."""
    out = np.zeros(t.shape + (3,), np.float32)
    for (p0, c0), (p1, c1) in zip(stops, stops[1:]):
        m = (t >= p0) & (t <= p1)
        f = ((t - p0) / max(1e-6, p1 - p0))[..., None]
        out[m] = (np.array(c0) * (1 - f) + np.array(c1) * f)[m]
    out[t < stops[0][0]] = stops[0][1]
    out[t > stops[-1][0]] = stops[-1][1]
    return out


def to_img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


# ------------------------------------------------------------ textures (W x H RGBA)
def leather(w, h, base=(206, 118, 48)):
    cells = fbm(w, h, 11, 3, 18)
    grain = fbm(w, h, 12, 4, 6)
    t = 0.55 * cells + 0.45 * grain
    t = (t - t.min()) / (t.max() - t.min())
    b = np.array(base, np.float32)
    arr = b[None, None] * (0.72 + 0.45 * t[..., None])
    pits = (cells < np.percentile(cells, 18))[..., None]
    arr = np.where(pits, arr * 0.78, arr)
    return to_img(arr)


def carbon(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    s = 7 * K
    cx, cy = (x // s) % 2, (y // s) % 2
    weave = np.where(cx == cy, (np.sin((x % s) / s * math.pi)), (np.sin((y % s) / s * math.pi)))
    v = 120 + 55 * weave + 18 * fbm(w, h, 3, 2, 40)
    arr = np.stack([v, v * 1.02, v * 1.05], -1)
    return to_img(arr)


def lowpoly(w, h, palette, n=70, seed=3, edges=None, edge_w=2):
    r = np.random.default_rng(seed)
    pts = np.vstack([r.random((n, 2)) * [w, h], [[0, 0], [w, 0], [0, h], [w, h], [w / 2, 0], [0, h / 2], [w, h / 2], [w / 2, h]]])
    tri = Delaunay(pts)
    img = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(img)
    for s in tri.simplices:
        p = [tuple(pts[i]) for i in s]
        cy = sum(q[1] for q in p) / 3 / h
        c = palette[int(r.integers(0, len(palette)))]
        k = 0.85 + 0.3 * (1 - cy) + r.uniform(-0.08, 0.08)
        d.polygon(p, fill=tuple(int(min(255, ch * k)) for ch in c) + (255,))
    if edges:
        for s in tri.simplices:
            p = [tuple(pts[i]) for i in s]
            d.line(p + [p[0]], fill=edges, width=edge_w)
    return img


def gold(w, h, dark=False):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    t = 0.5 + 0.5 * np.sin((x * 0.6 + y) / h * math.pi * 1.4)
    t = 0.75 * t + 0.25 * fbm(w, h, 5, 3, 30)
    stops = [(0, (122, 86, 22)), (0.45, (201, 162, 72)), (0.7, (240, 214, 140)), (1, (168, 124, 40))]
    if dark:
        stops = [(0, (40, 30, 14)), (0.5, (120, 92, 40)), (1, (70, 52, 20))]
    return to_img(colorize(t, stops))


def marble(w, h):
    f = fbm(w, h, 21, 6, 90)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    veins = np.abs(np.sin((x * 0.7 + y * 0.4) / w * 9 + f * 9))
    t = np.clip(veins ** 0.25, 0, 1)
    arr = colorize(t, [(0, (120, 128, 140)), (0.35, (200, 204, 212)), (1, (246, 247, 250))])
    return to_img(arr)


def glossy(w, h, top, bottom):
    y = np.linspace(0, 1, h, dtype=np.float32)[:, None]
    arr = np.array(top, np.float32) * (1 - y[..., None]) + np.array(bottom, np.float32) * y[..., None]
    arr = np.repeat(arr, w, axis=1)
    arr += (fbm(w, h, 31, 2, 60)[..., None] - 0.5) * 12
    return to_img(arr)


TEX = {
    "leather": lambda w, h: leather(w, h),
    "carbon": carbon,
    "green": lambda w, h: lowpoly(w, h, [(46, 140, 40), (70, 180, 50), (30, 110, 34), (110, 200, 70), (20, 90, 30)], 90, 5),
    "blackgold": lambda w, h: lowpoly(w, h, [(22, 22, 24), (34, 33, 36), (16, 16, 18), (28, 27, 30)], 46, 9, (190, 150, 70, 255), 2 * K),
    "gold": lambda w, h: gold(w, h),
    "black": lambda w, h: glossy(w, h, (52, 56, 62), (8, 9, 11)),
    "navy": lambda w, h: glossy(w, h, (40, 52, 74), (10, 14, 24)),
    "marble": marble,
}


def masked(tex, mask):
    out = Image.new("RGBA", tex.size, (0, 0, 0, 0))
    out.paste(tex, (0, 0), mask)
    return out


def bevel(mask, strength=0.55):
    """Light top-left / dark bottom-right edge shading inside a mask."""
    m = np.asarray(mask).astype(np.float32) / 255
    blur = np.asarray(mask.filter(ImageFilter.GaussianBlur(10 * K))).astype(np.float32) / 255
    gy, gx = np.gradient(blur)
    light = -(gx + gy)
    light = light / (np.abs(light).max() + 1e-6)
    edge = np.clip(1 - blur, 0, 1) * m
    return light * edge * strength, m


def shade(img, mask, strength=0.55, gloss=True):
    arr = np.asarray(img).astype(np.float32)
    l, m = bevel(mask, strength)
    arr[..., :3] = np.clip(arr[..., :3] * (1 + l[..., None]) + 255 * np.maximum(l, 0)[..., None] * 0.5, 0, 255)
    if gloss:
        h, w = m.shape
        y, x = np.mgrid[0:h, 0:w].astype(np.float32)
        g = np.clip(1 - np.hypot((x - w * 0.35) / (w * 0.5), (y - h * 0.22) / (h * 0.32)), 0, 1) ** 2 * 0.22
        arr[..., :3] = arr[..., :3] * (1 - g[..., None]) + 255 * g[..., None]
    arr[..., 3] = m * 255
    return Image.fromarray(arr.astype(np.uint8), "RGBA")


def with_shadow(img, pad, blur=14, opacity=0.45, dy=10):
    W, H = img.size
    canvas = Image.new("RGBA", (W + 2 * pad, H + 2 * pad), (0, 0, 0, 0))
    a = img.getchannel("A")
    sh = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    sh.paste((0, 0, 0, int(255 * opacity)), (pad, pad + dy * K), a)
    sh = sh.filter(ImageFilter.GaussianBlur(blur * K))
    canvas.alpha_composite(sh)
    canvas.alpha_composite(img, (pad, pad))
    return canvas


# ------------------------------------------------------------ round disc
def round_disc(tex_name, rim=None, cross=False):
    D = 360 * K
    tex = TEX[tex_name](D, D)
    m = Image.new("L", (D, D), 0)
    ImageDraw.Draw(m).ellipse([0, 0, D - 1, D - 1], fill=255)
    disc = shade(masked(tex, m), m, 0.6)
    d = ImageDraw.Draw(disc)
    if rim:
        d.ellipse([3 * K, 3 * K, D - 3 * K, D - 3 * K], outline=rim, width=7 * K)
    if cross:
        c = D / 2
        for a in (0, 90, 180, 270):
            r0, r1 = D * 0.33, D * 0.47
            x0, y0 = c + r0 * math.cos(math.radians(a)), c + r0 * math.sin(math.radians(a))
            x1, y1 = c + r1 * math.cos(math.radians(a)), c + r1 * math.sin(math.radians(a))
            d.line([x0, y0, x1, y1], fill=(40, 120, 200, 200), width=3 * K)
            d.ellipse([x1 - 6 * K, y1 - 6 * K, x1 + 6 * K, y1 + 6 * K], fill=(40, 120, 200, 220))
    # soft inner ring so digits read well
    ring = Image.new("RGBA", (D, D), (0, 0, 0, 0))
    ImageDraw.Draw(ring).ellipse([D * 0.17, D * 0.17, D * 0.83, D * 0.83], fill=(0, 0, 0, 46))
    disc.alpha_composite(ring.filter(ImageFilter.GaussianBlur(18 * K)))
    return with_shadow(disc, 20 * K)


# ------------------------------------------------------------ finger-ring counter
def ring_body(tex_name, button, screen_rim=(18, 18, 20)):
    W, H = 300 * K, 380 * K
    tex = TEX[tex_name](W, H)
    m = Image.new("L", (W, H), 0)
    md = ImageDraw.Draw(m)
    md.rounded_rectangle([0, 0, W - 1, H * 0.62], radius=W * 0.34, fill=255)
    md.ellipse([0, H * 0.3, W - 1, H - 1], fill=255)
    md.rectangle([0, H * 0.3, W - 1, H * 0.62], fill=255)
    m = m.filter(ImageFilter.GaussianBlur(1.2 * K))
    body = shade(masked(tex, m), m, 0.7)
    d = ImageDraw.Draw(body)
    # LCD window
    sx0, sy0, sx1, sy1 = W * 0.17, H * 0.13, W * 0.83, H * 0.36
    d.rounded_rectangle([sx0, sy0, sx1, sy1], radius=16 * K, fill=screen_rim + (255,))
    inset = 8 * K
    lcd = Image.new("RGBA", body.size, (0, 0, 0, 0))
    ld = ImageDraw.Draw(lcd)
    ld.rounded_rectangle([sx0 + inset, sy0 + inset, sx1 - inset, sy1 - inset], radius=8 * K, fill=(186, 196, 182, 255))
    body.alpha_composite(lcd)
    d = ImageDraw.Draw(body)
    # tiny side buttons
    for cx in (W * 0.1, W * 0.9):
        cy = H * 0.445
        d.ellipse([cx - 9 * K, cy - 9 * K, cx + 9 * K, cy + 9 * K], fill=(30, 30, 32, 255))
        d.ellipse([cx - 6 * K, cy - 7 * K, cx + 6 * K, cy + 5 * K], fill=(90, 92, 96, 255))
    # big press button: chrome ring + coloured centre
    bx, by, br = W / 2, H * 0.7, W * 0.27
    chrome = Image.new("RGBA", body.size, (0, 0, 0, 0))
    cd = ImageDraw.Draw(chrome)
    cd.ellipse([bx - br - 6 * K, by - br - 6 * K, bx + br + 6 * K, by + br + 6 * K], fill=(20, 20, 22, 200))
    body.alpha_composite(chrome.filter(ImageFilter.GaussianBlur(4 * K)))
    d = ImageDraw.Draw(body)
    yy, xx = np.mgrid[0:2 * int(br), 0:2 * int(br)].astype(np.float32)
    rr = np.hypot(xx - br, yy - br) / br
    ang = np.arctan2(yy - br, xx - br)
    met = 150 + 90 * np.cos(ang * 2 + 0.6) * (rr > 0.72)
    cen = np.array(button, np.float32)
    shadeC = np.clip(1.25 - np.hypot(xx - br * 0.7, yy - br * 0.6) / br, 0.55, 1.25)
    arr = np.where((rr > 0.74)[..., None], np.stack([met] * 3, -1), cen[None, None] * shadeC[..., None])
    hl = np.clip(1 - np.hypot((xx - br * 0.72) / (br * 0.5), (yy - br * 0.55) / (br * 0.28)), 0, 1) ** 2
    arr = arr * (1 - hl[..., None] * 0.7) + 255 * hl[..., None] * 0.7
    a = (rr <= 1).astype(np.float32) * 255
    btn = Image.fromarray(np.dstack([np.clip(arr, 0, 255), a]).astype(np.uint8), "RGBA")
    body.alpha_composite(btn, (int(bx - br), int(by - br)))
    return with_shadow(body, 20 * K)


# Only white, gold and black — the three finger-ring counters used in the app.
RING = [
    ("tasbih_white", lambda: ring_body("marble", (24, 24, 28))),
    ("tasbih_gold", lambda: ring_body("gold", (30, 22, 12))),
    ("tasbih_black", lambda: ring_body("blackgold", (200, 160, 62))),
]
ROUND = []

for name, fn in ROUND + RING:
    img = fn()
    # kept at 2x so it stays sharp on high-density phones
    img.save(os.path.join(OUT, name + ".png"), optimize=True)
    print(name, img.size)
