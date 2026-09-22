#!/usr/bin/env python3
"""Compose Play Store phone screenshots from real game assets."""
from __future__ import annotations

import math
import os
from dataclasses import dataclass
from typing import List, Tuple

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "store", "screenshots")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
RES = os.path.join(ROOT, "app", "src", "main", "res")

W, H = 1920, 1080  # 16:9 phone, landscape — how the game actually runs
TILE = 16
T_EMPTY, T_SOLID, T_ONEWAY = 0, 1, 2
VIEW_H = 240.0
SCALE = H / VIEW_H
VIEW_W = W / SCALE
DENSITY = 2.75  # ~xxhdpi on a 1080p phone in landscape


def dp(n: float) -> int:
    return int(round(n * DENSITY))

FG = (239, 230, 216, 255)
MUTED = (196, 184, 165, 255)
GOLD = (212, 165, 116, 255)
CARVED = (230, 211, 176, 255)
CARVED_GOLD = (240, 215, 160, 255)
BG = (18, 14, 12, 255)
HP = (196, 90, 72, 255)
HP_EMPTY = (42, 33, 24, 255)


def load(path: str) -> Image.Image:
    return Image.open(path).convert("RGBA")


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    name = "cinzel_bold.ttf" if bold else "cinzel_regular.ttf"
    return ImageFont.truetype(os.path.join(RES, "font", name), size)


def crop9(im: Image.Image) -> Image.Image:
    return im.crop((1, 1, im.width - 1, im.height - 1)).convert("RGBA")


def paste_scaled(dst: Image.Image, src: Image.Image, box: Tuple[int, int, int, int]) -> None:
    x0, y0, x1, y1 = box
    piece = src.resize((max(1, x1 - x0), max(1, y1 - y0)), Image.Resampling.NEAREST)
    dst.alpha_composite(piece, (x0, y0))


def sheet_cell(img: Image.Image, frame: int) -> Image.Image:
    cols, rows = 2, 2
    i = frame % 4
    cw, ch = img.width // cols, img.height // rows
    cc, rr = i % cols, i // cols
    cell = img.crop((cc * cw, rr * ch, cc * cw + cw, rr * ch + ch))
    return cell


def blit_sheet(world: Image.Image, img: Image.Image, frame: int, x: float, y: float, dw: float, dh: float, flip: bool) -> None:
    cell = sheet_cell(img, frame)
    if flip:
        cell = cell.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
    piece = cell.resize((max(1, int(dw)), max(1, int(dh))), Image.Resampling.NEAREST)
    px, py = int(round(x)), int(round(y))
    if px < 0 or py < 0:
        return
    world.alpha_composite(piece, (px, py))


@dataclass
class Level:
    id: int
    name: str
    cols: int
    rows: int
    tiles: bytearray
    spawn: Tuple[float, float]
    flag: Tuple[float, float]
    enemies: list
    pickups: list
    torches: list

    def tile(self, c: int, r: int) -> int:
        if c < 0 or c >= self.cols or r < 0:
            return T_SOLID
        if r >= self.rows:
            return T_EMPTY
        return self.tiles[r * self.cols + c]


def fill(t, cols, rows, c0, r0, c1, r1, v):
    for r in range(r0, r1 + 1):
        for c in range(c0, c1 + 1):
            if 0 <= c < cols and 0 <= r < rows:
                t[r * cols + c] = v


def fill_earth(t, cols, rows):
    for c in range(cols):
        lowest = -1
        for r in range(2, rows):
            if t[r * cols + c] == T_SOLID:
                lowest = r
        if lowest >= 2:
            for r in range(lowest + 1, rows):
                t[r * cols + c] = T_SOLID


def shell(t, cols, rows):
    fill(t, cols, rows, 0, 0, cols - 1, 1, T_SOLID)
    fill(t, cols, rows, 0, 1, 1, rows - 1, T_SOLID)
    fill(t, cols, rows, cols - 2, 1, cols - 1, rows - 1, T_SOLID)


def ground(t, cols, rows, c0, r, c1):
    fill(t, cols, rows, c0, r, c1, r, T_SOLID)


def ledge(t, cols, rows, c0, r, c1):
    fill(t, cols, rows, c0, r, c1, r, T_ONEWAY)


def build_level(n: int) -> Level:
    if n == 1:
        cols, rows = 132, 16
        t = bytearray(cols * rows)
        shell(t, cols, rows)
        fill(t, cols, rows, 0, 13, 24, 14, T_SOLID)
        fill(t, cols, rows, 16, 12, 24, 12, T_SOLID)
        ledge(t, cols, rows, 8, 9, 14)
        fill(t, cols, rows, 25, 13, 46, 14, T_SOLID)
        fill(t, cols, rows, 28, 12, 32, 12, T_SOLID)
        ledge(t, cols, rows, 30, 10, 38)
        fill(t, cols, rows, 47, 13, 53, 14, T_SOLID)
        fill(t, cols, rows, 61, 13, 68, 14, T_SOLID)
        ledge(t, cols, rows, 53, 9, 61)
        ledge(t, cols, rows, 49, 6, 55)
        fill(t, cols, rows, 69, 13, 90, 14, T_SOLID)
        fill(t, cols, rows, 69, 12, 74, 12, T_SOLID)
        ledge(t, cols, rows, 74, 11, 80)
        ledge(t, cols, rows, 80, 8, 87)
        ledge(t, cols, rows, 85, 5, 90)
        fill(t, cols, rows, 91, 13, 97, 14, T_SOLID)
        fill(t, cols, rows, 105, 13, 112, 14, T_SOLID)
        ledge(t, cols, rows, 97, 10, 105)
        ledge(t, cols, rows, 93, 7, 99)
        ledge(t, cols, rows, 102, 6, 108)
        fill(t, cols, rows, 113, 13, cols - 1, 14, T_SOLID)
        fill(t, cols, rows, 126, 12, cols - 1, 12, T_SOLID)
        fill_earth(t, cols, rows)
        return Level(1, "Ember Halls", cols, rows, t, (3 * TILE + 3, 13 * TILE - 14), (129 * TILE + 2, 12 * TILE - 26),
                     [("slime", 34, 13), ("slime", 76, 11), ("slime", 108, 13), ("bat", 56, 4), ("bat", 84, 3), ("dragon", 120, 13)],
                     [("coin", 12, 13), ("coin", 52, 9), ("coin", 86, 5), ("heal", 114, 13), ("mana", 127, 12)],
                     [12, 36, 55, 76, 102, 124])
    if n == 2:
        cols, rows = 148, 26
        t = bytearray(cols * rows)
        shell(t, cols, rows)
        ground(t, cols, rows, 2, 16, 22)
        fill(t, cols, rows, 16, 15, 22, 15, T_SOLID)
        ledge(t, cols, rows, 8, 12, 15)
        ground(t, cols, rows, 23, 22, 39)
        ground(t, cols, rows, 46, 22, 62)
        ledge(t, cols, rows, 39, 18, 47)
        ledge(t, cols, rows, 32, 7, 58)
        ground(t, cols, rows, 63, 20, 84)
        ground(t, cols, rows, 96, 23, 118)
        ground(t, cols, rows, 132, 12, cols - 1)
        fill(t, cols, rows, 142, 11, cols - 1, 11, T_SOLID)
        fill_earth(t, cols, rows)
        return Level(2, "Forked Dark", cols, rows, t, (4 * TILE + 3, 16 * TILE - 14), (145 * TILE + 2, 11 * TILE - 26),
                     [("slime", 18, 16), ("slime", 30, 22), ("dragon", 138, 12), ("bat", 40, 5)],
                     [("coin", 20, 16), ("heal", 133, 12), ("mana", 144, 11)],
                     [10, 28, 50, 140])
    cols, rows = 160, 28
    t = bytearray(cols * rows)
    shell(t, cols, rows)
    ground(t, cols, rows, 2, 24, 16)
    ledge(t, cols, rows, 22, 6, 30)
    ground(t, cols, rows, 58, 23, 78)
    ground(t, cols, rows, 80, 20, 96)
    ground(t, cols, rows, 102, 22, 118)
    ground(t, cols, rows, 136, 18, cols - 1)
    fill(t, cols, rows, 154, 17, cols - 1, 17, T_SOLID)
    fill_earth(t, cols, rows)
    return Level(3, "Night's Crown", cols, rows, t, (4 * TILE + 3, 24 * TILE - 14), (157 * TILE + 2, 17 * TILE - 26),
                 [("slime", 10, 24), ("slime", 66, 23), ("dragon", 148, 18), ("bat", 40, 3)],
                 [("coin", 10, 24), ("heal", 145, 18), ("mana", 156, 17)],
                 [8, 66, 150])


class A:
    def __init__(self):
        s = os.path.join(ASSETS, "sprites")
        self.knight_idle = load(os.path.join(s, "knight/idle.png"))
        self.knight_run = load(os.path.join(s, "knight/run.png"))
        self.knight_jump = load(os.path.join(s, "knight/jump.png"))
        self.knight_attack = load(os.path.join(s, "knight/attack.png"))
        self.slime = load(os.path.join(s, "slime/idle.png"))
        self.bat = load(os.path.join(s, "bat/idle.png"))
        self.dragon_idle = load(os.path.join(s, "dragon/idle.png"))
        self.dragon_attack = load(os.path.join(s, "dragon/attack.png"))
        self.flag = load(os.path.join(s, "flag/idle.png"))
        self.coin = load(os.path.join(s, "pickups/coin.png"))
        self.heal = load(os.path.join(s, "pickups/heal.png"))
        self.mana = load(os.path.join(s, "pickups/mana.png"))
        self.fireball = load(os.path.join(s, "fx/fireball.png"))
        self.slash = load(os.path.join(s, "fx/slash.png"))
        self.stone = load(os.path.join(s, "tiles/stone.jpg")).convert("RGBA")
        self.rock = load(os.path.join(s, "tiles/rock.jpg")).convert("RGBA")
        self.bg = load(os.path.join(ASSETS, "map/cave-far-bg.jpg")).convert("RGBA")
        self.title = load(os.path.join(RES, "drawable-nodpi/title_night.jpg")).convert("RGBA")
        self.slab = crop9(load(os.path.join(RES, "drawable/btn_slab_stone.9.png")))
        self.gold = crop9(load(os.path.join(RES, "drawable/btn_slab_gold.9.png")))
        self.icon = load(os.path.join(ROOT, "icon-512.png"))


def sample_tile(tex: Image.Image, c: int, r: int) -> Image.Image:
    tw = max(1, tex.width - TILE)
    th = max(1, tex.height - TILE)
    sx = (c * TILE) % tw
    sy = (r * TILE) % th
    return tex.crop((sx, sy, sx + TILE, sy + TILE))


def camera(px: float, py: float, world_w: float, world_h: float) -> Tuple[float, float]:
    look = 36
    tx = px + 5 - VIEW_W * 0.42 + look
    ty = py + 7 - VIEW_H * 0.58
    tx = max(0, min(tx, max(0, world_w - VIEW_W)))
    ty = max(0, min(ty, max(0, world_h - VIEW_H)))
    return tx, ty


def render_world(a: A, lv: Level, px: float, py: float, extras: dict) -> Image.Image:
    camx, camy = camera(px, py, lv.cols * TILE, lv.rows * TILE)
    world = Image.new("RGBA", (int(VIEW_W) + 8, int(VIEW_H) + 8), (18, 14, 12, 255))
    dest_h = 288
    dest_w = dest_h * a.bg.width / a.bg.height
    parallax = camx * 0.18
    x = camx - ((parallax % dest_w) + dest_w) % dest_w
    bg = a.bg.resize((int(dest_w), dest_h), Image.Resampling.BILINEAR)
    while x < camx + VIEW_W + 4:
        world.alpha_composite(bg, (int(x - camx), int(-12)))
        x += dest_w - 1
    tint = Image.new("RGBA", world.size, (18, 12, 6, 0x47))
    world.alpha_composite(tint)

    c0 = max(0, int(camx / TILE) - 1)
    c1 = min(lv.cols - 1, int((camx + VIEW_W) / TILE) + 1)
    r0 = max(0, int(camy / TILE) - 1)
    r1 = min(lv.rows - 1, int((camy + VIEW_H) / TILE) + 1)
    overlay = ImageDraw.Draw(world)
    for r in range(r0, r1 + 1):
        for c in range(c0, c1 + 1):
            t = lv.tile(c, r)
            x = c * TILE - camx
            y = r * TILE - camy
            if t == T_SOLID:
                shell_tile = r < 2 or c < 2 or c >= lv.cols - 2
                tex = a.rock if shell_tile else a.stone
                world.paste(sample_tile(tex, c, r), (int(x), int(y)))
                if not shell_tile and lv.tile(c, r - 1) != T_SOLID:
                    overlay.rectangle([x, y, x + TILE, y + 2], fill=(235, 220, 200, 40))
                    overlay.rectangle([x, y + TILE - 2, x + TILE, y + TILE], fill=(0, 0, 0, 70))
            elif t == T_ONEWAY:
                world.paste(sample_tile(a.stone, c, r), (int(x), int(y)))
                overlay.rectangle([x, y, x + TILE, y + 2], fill=(235, 220, 200, 56))
                overlay.rectangle([x, y + 5, x + TILE, y + TILE], fill=(0, 0, 0, 140))

    # torches
    for col in lv.torches:
        tx, ty = col * TILE + 8 - camx, 6 * TILE - camy
        glow = Image.new("RGBA", (92, 92), (0, 0, 0, 0))
        gd = ImageDraw.Draw(glow)
        gd.ellipse([0, 0, 91, 91], fill=(232, 170, 90, 40))
        world.alpha_composite(glow, (int(tx - 46), int(ty - 46)))

    def wx(x): return x - camx
    def wy(y): return y - camy

    for kind, c, floor in lv.pickups:
        ix, iy = c * TILE + 4, floor * TILE - 12
        if kind == "coin":
            blit_sheet(world, a.coin, 1, wx(ix - 2), wy(iy - 2), 14, 14, False)
        else:
            img = a.heal if kind == "heal" else a.mana
            piece = img.resize((14, 16), Image.Resampling.NEAREST)
            world.alpha_composite(piece, (int(wx(ix - 1)), int(wy(iy - 2))))

    fx, fy = lv.flag
    blit_sheet(world, a.flag, 1, wx(fx - 6), wy(fy - 4), 28, 32, False)

    for kind, c, floor in lv.enemies:
        if kind == "slime":
            ex, ey = c * TILE, floor * TILE - 10
            blit_sheet(world, a.slime, extras.get("slime_frame", 1), wx(ex + 6 - 11), wy(ey + 10 - 18), 22, 18, True)
        elif kind == "bat":
            ex, ey = c * TILE, floor * TILE
            blit_sheet(world, a.bat, 2, wx(ex + 6 - 11), wy(ey + 5 - 9), 22, 18, True)
        elif kind == "dragon":
            ex, ey = c * TILE - 20, floor * TILE - 88
            bmp = a.dragon_attack if extras.get("dragon_attack") else a.dragon_idle
            blit_sheet(world, bmp, extras.get("dragon_frame", 1), wx(ex + 35 - 87.5), wy(ey + 88 - 140), 175, 140, True)
            if extras.get("dragon_hp") is not None:
                bar, hp, maxhp = 45, extras["dragon_hp"], 10
                hx, hy = wx(ex + 35 - bar / 2), wy(ey + 88 - 140 - 6)
                overlay.rectangle([hx, hy, hx + bar, hy + 2], fill=(90, 34, 28, 255))
                overlay.rectangle([hx, hy, hx + bar * hp / maxhp, hy + 2], fill=HP)

    anim = extras.get("player_anim", "idle")
    fr = extras.get("player_frame", 1)
    bmp = {"idle": a.knight_idle, "run": a.knight_run, "jump": a.knight_jump, "attack": a.knight_attack}[anim]
    blit_sheet(world, bmp, fr, wx(px + 5 - 14), wy(py + 14 - 26), 28, 28, extras.get("flip", False))
    if extras.get("slash"):
        blit_sheet(world, a.slash, 2, wx(px + 10 - 2), wy(py), 22, 18, False)
    if extras.get("fireball"):
        bx, by = extras["fireball"]
        blit_sheet(world, a.fireball, 1, wx(bx), wy(by), 18, 16, False)
    if extras.get("floater"):
        d = ImageDraw.Draw(world)
        d.text((wx(extras["floater"][0]), wy(extras["floater"][1])), extras["floater"][2], font=font(10, True), fill=CARVED_GOLD)

    screen = world.resize((W, H), Image.Resampling.NEAREST)
    # vignette
    vig = Image.new("L", (W, H), 0)
    vd = ImageDraw.Draw(vig)
    for i in range(18):
        a_val = int(18 * (i / 18) ** 1.6)
        vd.rectangle([i * 12, i * 8, W - i * 12, H - i * 8], outline=a_val)
    vig = vig.filter(ImageFilter.GaussianBlur(28))
    dark = Image.new("RGBA", (W, H), (8, 6, 5, 0))
    dark.putalpha(vig.point(lambda p: int(p * 1.4)))
    screen.alpha_composite(dark)
    return screen


def circle(img: Image.Image, cx: int, cy: int, r: int, fill, outline, stroke: int) -> None:
    overlay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=fill, outline=outline, width=stroke)
    img.alpha_composite(overlay)


def tri(d, kind: str, cx: int, cy: int, s: int) -> None:
    c = (239, 230, 216, 255)
    if kind == "left":
        d.polygon([(cx - s, cy), (cx + int(s * 0.55), cy - s), (cx + int(s * 0.55), cy + s)], fill=c)
    elif kind == "right":
        d.polygon([(cx + s, cy), (cx - int(s * 0.55), cy - s), (cx - int(s * 0.55), cy + s)], fill=c)
    elif kind == "up":
        d.polygon([(cx, cy - s), (cx + s, cy + int(s * 0.55)), (cx - s, cy + int(s * 0.55))], fill=c)
    else:
        d.polygon([(cx, cy + s), (cx - s, cy - int(s * 0.55)), (cx + s, cy - int(s * 0.55))], fill=c)


def draw_controls(img: Image.Image) -> None:
    """Real overlay from activity_main.xml: triangle arrows, sword, flame."""
    m = dp(16)
    pad = dp(168)
    btn = dp(52)
    plate_x, plate_y = m, H - m - pad
    circle(img, plate_x + pad // 2, plate_y + pad // 2, pad // 2, (239, 230, 216, 13), (239, 230, 216, 34), max(2, dp(1)))
    cx, cy = plate_x + pad // 2, plate_y + pad // 2
    inset = dp(8) + btn // 2
    positions = {
        "up": (cx, plate_y + inset),
        "left": (plate_x + inset, cy),
        "right": (plate_x + pad - inset, cy),
        "down": (cx, plate_y + pad - inset),
    }
    d = ImageDraw.Draw(img)
    for kind, (x, y) in positions.items():
        circle(img, x, y, btn // 2, (239, 230, 216, 26), (239, 230, 216, 51), max(2, dp(2)))
        tri(d, kind, x, y, dp(9))
    me, mb = dp(20), dp(20)
    jump_s, atk_s, spl_s = dp(60), dp(72), dp(48)
    jump_cx = W - me - jump_s // 2
    jump_cy = H - mb - dp(28) - jump_s // 2
    atk_cx = jump_cx - dp(10) - jump_s // 2 - atk_s // 2
    atk_cy = H - mb - atk_s // 2
    spl_cx = atk_cx - dp(8) - atk_s // 2 - spl_s // 2
    spl_cy = H - mb - dp(36) - spl_s // 2
    circle(img, spl_cx, spl_cy, spl_s // 2, (239, 230, 216, 34), (239, 230, 216, 68), max(2, dp(2)))
    w = max(3, dp(2))
    d.arc([spl_cx - dp(10), spl_cy - dp(4), spl_cx + dp(4), spl_cy + dp(12)], start=200, end=20, fill=FG, width=w)
    d.arc([spl_cx - dp(2), spl_cy - dp(12), spl_cx + dp(12), spl_cy + dp(8)], start=240, end=80, fill=FG, width=w)
    circle(img, atk_cx, atk_cy, atk_s // 2, (239, 230, 216, 34), (239, 230, 216, 68), max(2, dp(2)))
    s = dp(13)
    d.line([(atk_cx - s, atk_cy + s), (atk_cx + s, atk_cy - s)], fill=FG, width=max(3, dp(2)))
    d.line([(atk_cx + int(s * 0.2), atk_cy - s), (atk_cx + s, atk_cy - s), (atk_cx + s, atk_cy - int(s * 0.2))], fill=FG, width=max(3, dp(2)))
    d.line([(atk_cx - s, atk_cy + int(s * 0.2)), (atk_cx - s, atk_cy + s), (atk_cx - int(s * 0.2), atk_cy + s)], fill=FG, width=max(3, dp(2)))
    circle(img, jump_cx, jump_cy, jump_s // 2, (239, 230, 216, 34), (239, 230, 216, 68), max(2, dp(2)))
    tri(d, "up", jump_cx, jump_cy, dp(11))


def draw_hud(img: Image.Image, name: str, hp: int = 10, mana: bool = True, coins: str = "1/3") -> None:
    d = ImageDraw.Draw(img)
    m = dp(16)
    d.text((m, m), name.upper(), font=font(dp(11), True), fill=(138, 122, 104, 255))
    x = m
    y = m + dp(18)
    for i in range(10):
        col = HP if i < hp else HP_EMPTY
        d.rectangle([x, y, x + dp(14), y + dp(8)], fill=col)
        x += dp(16)
    y2 = y + dp(14)
    d.rectangle([m, y2, m + dp(28), y2 + dp(8)], fill=(90, 140, 200, 220) if mana else HP_EMPTY)
    d.text((m + dp(34), y2 - dp(2)), coins, font=font(dp(12), True), fill=MUTED)
    ps = dp(44)
    d.rectangle([W - m - ps, m, W - m, m + ps], outline=(239, 230, 216, 80), width=max(2, dp(1)))
    d.text((W - m - ps // 2, m + ps // 2), "II", font=font(dp(14), True), fill=FG, anchor="mm")


def cover_menu(a: A) -> Image.Image:
    bg = a.title.resize((W, H), Image.Resampling.LANCZOS)
    # slightly lift the veil vs the in-game 30% so the cave reads
    veil = Image.new("RGBA", (W, H), (18, 14, 12, 70))
    img = bg.copy()
    img.alpha_composite(veil)
    return img


def draw_button(img: Image.Image, slab: Image.Image, y: int, text: str, gold=False) -> None:
    bw, bh = 620, 118 if gold else 108
    x = (W - bw) // 2
    paste_scaled(img, slab, (x, y, x + bw, y + bh))
    d = ImageDraw.Draw(img)
    col = CARVED_GOLD if gold else CARVED
    d.text((W // 2, y + bh // 2 - 2), text.upper(), font=font(28, True), fill=col, anchor="mm")


def menu(a: A) -> Image.Image:
    img = cover_menu(a)
    d = ImageDraw.Draw(img)
    d.text((W // 2, 118), "A TORCHLIT DESCENT", font=font(22, True), fill=GOLD, anchor="mm")
    d.text((W // 2, 210), "Dungeons", font=font(92, True), fill=FG, anchor="mm")
    d.text((W // 2, 300), "in the Night", font=font(64, True), fill=MUTED, anchor="mm")
    d.text((W // 2, 390), "Three halls. Slimes, bats, a red banner at the far end.", font=font(24), fill=FG, anchor="mm")
    draw_button(img, a.gold, 470, "Start Game", True)
    draw_button(img, a.slab, 610, "Select Level")
    draw_button(img, a.slab, 740, "Settings")
    d.text((W // 2, 900), "XP  240", font=font(28, True), fill=GOLD, anchor="mm")
    icon = a.icon.resize((96, 96), Image.Resampling.LANCZOS)
    img.alpha_composite(icon, (72, 72))
    return img


def levels(a: A) -> Image.Image:
    img = cover_menu(a)
    d = ImageDraw.Draw(img)
    d.text((W // 2, 160), "SELECT LEVEL", font=font(52, True), fill=FG, anchor="mm")
    draw_button(img, a.slab, 280, "Ember Halls  ·  Open")
    draw_button(img, a.slab, 420, "Forked Dark  ·  Open")
    draw_button(img, a.slab, 560, "Night's Crown  ·  Locked")
    draw_button(img, a.slab, 740, "Back")
    return img


def settings(a: A) -> Image.Image:
    img = cover_menu(a)
    d = ImageDraw.Draw(img)
    d.text((W // 2, 140), "SETTINGS", font=font(52, True), fill=FG, anchor="mm")
    d.text((W // 2, 250), "VOLUME", font=font(22, True), fill=GOLD, anchor="mm")
    bar_x, bar_y, bar_w = 420, 300, 1080
    d.rectangle([bar_x, bar_y, bar_x + bar_w, bar_y + 10], fill=(42, 33, 24, 255))
    d.rectangle([bar_x, bar_y, bar_x + int(bar_w * 0.8), bar_y + 10], fill=GOLD)
    d.ellipse([bar_x + int(bar_w * 0.8) - 16, bar_y - 12, bar_x + int(bar_w * 0.8) + 16, bar_y + 22], fill=CARVED_GOLD)
    d.text((W // 2, 360), "80%", font=font(26), fill=MUTED, anchor="mm")
    d.text((W // 2, 460), "LANGUAGE", font=font(22, True), fill=GOLD, anchor="mm")
    draw_button(img, a.slab, 500, "English")
    draw_button(img, a.slab, 680, "Back")
    return img


def panel(img: Image.Image, lines) -> None:
    d = ImageDraw.Draw(img)
    pw, ph = 820, 520
    x, y = (W - pw) // 2, (H - ph) // 2
    d.rounded_rectangle([x, y, x + pw, y + ph], radius=8, fill=(18, 14, 12, 230), outline=(212, 165, 116, 70), width=2)
    yy = y + 70
    for i, (text, size, col, bold) in enumerate(lines):
        d.text((W // 2, yy), text, font=font(size, bold), fill=col, anchor="mm")
        yy += size + 28


def to_rgb(img: Image.Image) -> Image.Image:
    bg = Image.new("RGB", img.size, (18, 14, 12))
    bg.paste(img.convert("RGB"), mask=img.split()[-1] if img.mode == "RGBA" else None)
    # Play wants no alpha; flatten
    return Image.new("RGB", img.size, (18, 14, 12)).convert("RGB").paste and None or img.convert("RGB")


def flatten(img: Image.Image) -> Image.Image:
    out = Image.new("RGB", (W, H), (18, 14, 12))
    out.paste(img.convert("RGB"), mask=img.split()[-1])
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    a = A()
    shots = []

    shots.append(("01-menu.png", flatten(menu(a))))
    shots.append(("02-select-level.png", flatten(levels(a))))
    shots.append(("03-settings.png", flatten(settings(a))))

    lv1 = build_level(1)
    play = render_world(a, lv1, 34 * TILE - 40, 13 * TILE - 14, {"player_anim": "run", "player_frame": 2, "slime_frame": 0})
    draw_hud(play, "Ember Halls", 10, True, "1/3")
    draw_controls(play)
    shots.append(("04-ember-halls.png", flatten(play)))

    combat = render_world(
        a, lv1, 34 * TILE - 18, 13 * TILE - 14,
        {"player_anim": "attack", "player_frame": 2, "slash": True, "slime_frame": 2,
         "fireball": (34 * TILE + 10, 13 * TILE - 22), "floater": (34 * TILE, 13 * TILE - 40, "HP restored")},
    )
    draw_hud(combat, "Ember Halls", 8, False, "2/3")
    draw_controls(combat)
    shots.append(("05-combat.png", flatten(combat)))

    air = render_world(
        a, lv1, 84 * TILE, 8 * TILE - 14,
        {"player_anim": "jump", "player_frame": 1, "slime_frame": 1},
    )
    draw_hud(air, "Ember Halls", 9, True, "2/3")
    draw_controls(air)
    shots.append(("06-ascent.png", flatten(air)))

    boss = render_world(
        a, lv1, 116 * TILE, 13 * TILE - 14,
        {"player_anim": "idle", "player_frame": 0, "dragon_attack": True, "dragon_frame": 2, "dragon_hp": 7, "flip": False},
    )
    draw_hud(boss, "Ember Halls", 6, True, "3/3")
    draw_controls(boss)
    shots.append(("07-dragon.png", flatten(boss)))

    win = render_world(a, lv1, 126 * TILE, 12 * TILE - 14, {"player_anim": "idle", "player_frame": 1, "dragon_hp": 0})
    veil = Image.new("RGBA", (W, H), (18, 14, 12, 200))
    win.alpha_composite(veil)
    panel(win, [
        ("THE BANNER IS YOURS", 22, GOLD, True),
        ("You Win", 64, FG, True),
        ("+100 XP", 36, FG, True),
        ("Next hall: Forked Dark. Continue?", 24, MUTED, False),
    ])
    draw_button(win, a.gold, 700, "Yes — next level", True)
    draw_button(win, a.slab, 830, "No — menu")
    shots.append(("08-victory.png", flatten(win)))

    art = os.path.join("/workspace/artifacts", "play-screenshots")
    os.makedirs(art, exist_ok=True)
    for name, im in shots:
        path = os.path.join(OUT, name)
        im.save(path, "PNG", optimize=True)
        im.save(os.path.join(art, name), "PNG", optimize=True)
        print(name, im.size, im.mode)

    print("done", OUT)


if __name__ == "__main__":
    main()
