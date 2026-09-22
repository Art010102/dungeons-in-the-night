package com.dungeonsnight.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    val engine = Engine(1)
    var running = false
    private var thread: Thread? = null
    private var assets: Assets? = null
    var listener: Listener? = null

    var camX = 0f
    var camY = 0f
    private var camReady = false
    var trauma = 0f
    private var lastHp = -1
    private var lastMana = -1
    private var lastCoins = -1
    private var lastPhase: Phase? = null
    private var lastPaused = false
    private val pix = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = false }
    private val fill = Paint()
    private val src = Rect()
    private val dst = RectF()

    interface Listener {
        fun onHud(hp: Int, mana: Int, coins: Int, phase: Phase, paused: Boolean, levelName: String)
        fun onWin()
        fun onDead()
        fun onEvent(ev: GameEvent)
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        setZOrderOnTop(false)
    }

    fun loadAssets() {
        if (assets == null) assets = Assets(context)
    }

    fun startLevel(id: Int) {
        engine.reset(id)
        camReady = false
        trauma = 0f
    }

    fun resetCamera() { camReady = false }

    override fun surfaceCreated(holder: SurfaceHolder) {
        loadAssets()
        running = true
        thread = Thread(this, "dungeons-loop").also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try { thread?.join(400) } catch (_: InterruptedException) {}
        thread = null
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = min(0.1f, (now - last) / 1_000_000_000f)
            last = now
            engine.step(dt)
            val events = engine.drainEvents()
            for (ev in events) {
                post { listener?.onEvent(ev) }
                if (ev is GameEvent.Win) post { listener?.onWin() }
                if (ev is GameEvent.GameOver) post { listener?.onDead() }
            }
            trauma = max(0f, trauma - dt * 1.8f)
            val hp = engine.player.hp
            val mana = engine.player.mana
            val coins = engine.coins
            val phase = engine.phase
            val paused = engine.paused
            val name = engine.level.name
            if (hp != lastHp || mana != lastMana || coins != lastCoins || phase != lastPhase || paused != lastPaused) {
                lastHp = hp; lastMana = mana; lastCoins = coins; lastPhase = phase; lastPaused = paused
                post { listener?.onHud(hp, mana, coins, phase, paused, name) }
            }
            val canvas = try { holder.lockCanvas() } catch (_: Exception) { null }
            if (canvas != null) {
                try { drawWorld(canvas, dt) } finally { holder.unlockCanvasAndPost(canvas) }
            } else {
                try { Thread.sleep(8) } catch (_: InterruptedException) {}
            }
        }
    }

    private fun drawWorld(canvas: Canvas, dt: Float) {
        val a = assets ?: return
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        canvas.drawColor(Color.parseColor("#120E0C"))
        val viewH = 240f
        val scale = h / viewH
        var viewW = w / scale
        if (viewW < 300f) {
            val s = w / 300f
            viewW = 300f
            val viewH2 = h / s
            drawScaled(canvas, a, dt, w, h, viewW, viewH2, s)
            return
        }
        drawScaled(canvas, a, dt, w, h, viewW, viewH, scale)
    }

    private fun drawScaled(
        canvas: Canvas, a: Assets, dt: Float,
        screenW: Float, screenH: Float, viewW: Float, viewH: Float, scale: Float,
    ) {
        val p = engine.player
        val look = p.facing * 36f + p.vx * 0.18f
        val tx = (p.x + p.w / 2f - viewW * 0.42f + look).coerceIn(0f, max(0f, engine.worldW - viewW))
        val ty = (p.y + p.h / 2f - viewH * 0.58f).coerceIn(0f, max(0f, engine.worldH - viewH))
        if (!camReady) {
            camX = tx; camY = ty; camReady = true
        } else {
            camX = lerpCam(camX, tx, dt)
            camY = lerpCam(camY, ty, dt)
        }
        val shake = trauma * trauma
        val sx = if (shake > 0f) (Math.random().toFloat() * 2f - 1f) * 5f * shake else 0f
        val sy = if (shake > 0f) (Math.random().toFloat() * 2f - 1f) * 4f * shake else 0f

        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(-camX + sx, -camY + sy)

        drawBackdrop(canvas, a, viewW, viewH)
        drawTiles(canvas, a, viewW, viewH)
        drawTorches(canvas)
        drawPickups(canvas, a)
        drawFlag(canvas, a)
        drawEnemies(canvas, a)
        drawProjectiles(canvas, a)
        drawPlayer(canvas, a)
        drawSlash(canvas, a)
        drawFloaters(canvas)
        canvas.restore()
        // vignette
        fill.shader = android.graphics.RadialGradient(
            screenW / 2f, screenH * 0.55f, screenW * 0.72f,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#80080605")),
            floatArrayOf(0.35f, 1f),
            android.graphics.Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, screenW, screenH, fill)
        fill.shader = null
    }

    private fun drawBackdrop(canvas: Canvas, a: Assets, viewW: Float, viewH: Float) {
        val img = a.bg
        val destH = 288f
        val destW = destH * img.width / img.height
        val px = camX * 0.18f
        var x = camX - (((px % destW) + destW) % destW)
        while (x < camX + viewW + 4f) {
            dst.set(x, camY - 12f, x + destW, camY - 12f + destH)
            canvas.drawBitmap(img, null, dst, pix)
            x += destW - 1f
        }
        fill.color = Color.argb(0x47, 0x12, 0x0C, 0x06)
        canvas.drawRect(camX - 4f, camY - 4f, camX + viewW + 4f, camY + viewH + 4f, fill)
    }

    private fun sample(canvas: Canvas, bmp: Bitmap, c: Int, r: Int, x: Float, y: Float) {
        val tw = max(1, bmp.width - TILE.toInt())
        val th = max(1, bmp.height - TILE.toInt())
        val sx = (c * TILE.toInt()) % tw
        val sy = (r * TILE.toInt()) % th
        src.set(sx, sy, sx + TILE.toInt(), sy + TILE.toInt())
        dst.set(x, y, x + TILE, y + TILE)
        canvas.drawBitmap(bmp, src, dst, pix)
    }

    private fun drawTiles(canvas: Canvas, a: Assets, viewW: Float, viewH: Float) {
        val lv = engine.level
        val c0 = max(0, floor(camX / TILE).toInt() - 1)
        val c1 = min(lv.cols - 1, floor((camX + viewW) / TILE).toInt() + 1)
        val r0 = max(0, floor(camY / TILE).toInt() - 1)
        val r1 = min(lv.rows - 1, floor((camY + viewH) / TILE).toInt() + 1)
        for (r in r0..r1) for (c in c0..c1) {
            val t = lv.tileAt(c, r)
            val x = c * TILE
            val y = r * TILE
            if (t == T_SOLID) {
                val shell = r < 2 || c < 2 || c >= lv.cols - 2
                val above = lv.tileAt(c, r - 1)
                sample(canvas, if (shell) a.rock else a.stone, c, r, x, y)
                if (!shell && above != T_SOLID) {
                    fill.color = Color.argb(40, 235, 220, 200)
                    canvas.drawRect(x, y, x + TILE, y + 2f, fill)
                    fill.color = Color.argb(70, 0, 0, 0)
                    canvas.drawRect(x, y + TILE - 2f, x + TILE, y + TILE, fill)
                }
            } else if (t == T_ONEWAY) {
                sample(canvas, a.stone, c, r, x, y)
                fill.color = Color.argb(56, 235, 220, 200)
                canvas.drawRect(x, y, x + TILE, y + 2f, fill)
                fill.color = Color.argb(140, 0, 0, 0)
                canvas.drawRect(x, y + 5f, x + TILE, y + TILE, fill)
            }
        }
    }

    private fun drawTorches(canvas: Canvas) {
        val now = System.currentTimeMillis() / 1000.0
        for (t in engine.level.torches) {
            val flicker = 0.72 + sin(now * 5 + t.x) * 0.08 + sin(now * 8 + t.y) * 0.06
            fill.shader = android.graphics.RadialGradient(
                t.x, t.y, 46f,
                intArrayOf(
                    Color.argb((55 * flicker).roundToInt().coerceIn(0, 255), 232, 170, 90),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 1f),
                android.graphics.Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(t.x, t.y, 46f, fill)
            fill.shader = null
            fill.color = Color.argb((210 * flicker).roundToInt().coerceIn(0, 80), 255, 210, 130)
            canvas.drawRect(t.x - 1f, t.y - 3f, t.x + 1f, t.y + 2f, fill)
        }
    }

    private fun drawSheet(canvas: Canvas, bmp: Bitmap, frame: Int, dx: Float, dy: Float, dw: Float, dh: Float, flip: Boolean) {
        val cols = 2
        val rows = 2
        val i = ((frame % 4) + 4) % 4
        val cw = bmp.width / cols
        val ch = bmp.height / rows
        val cc = i % cols
        val rr = i / cols
        src.set(cc * cw, rr * ch, cc * cw + cw, rr * ch + ch)
        canvas.save()
        if (flip) {
            canvas.translate(dx + dw / 2f, dy + dh / 2f)
            canvas.scale(-1f, 1f)
            canvas.translate(-dw / 2f, -dh / 2f)
            dst.set(0f, 0f, dw, dh)
            canvas.drawBitmap(bmp, src, dst, pix)
        } else {
            dst.set(dx, dy, dx + dw, dy + dh)
            canvas.drawBitmap(bmp, src, dst, pix)
        }
        canvas.restore()
    }

    private fun drawFlag(canvas: Canvas, a: Assets) {
        val f = engine.level
        val frame = ((System.currentTimeMillis() / 180) % 4).toInt()
        drawSheet(canvas, a.flag, frame, f.flagX - 6f, f.flagY - 4f, 28f, 32f, false)
    }

    private fun drawPlayer(canvas: Canvas, a: Assets) {
        val p = engine.player
        if (p.invuln > 0f && (p.invuln * 16).toInt() % 2 == 0) pix.alpha = 115 else pix.alpha = 255
        val (bmp, frame) = playerFrame(p, a)
        drawSheet(canvas, bmp, frame, p.x + p.w / 2f - 14f, p.y + p.h - 26f, 28f, 28f, p.facing < 0)
        pix.alpha = 255
    }

    private fun playerFrame(p: Player, a: Assets): Pair<Bitmap, Int> {
        if (p.anim == "attack") {
            val t = 1f - p.attackT / ATTACK_TIME
            return a.knightAttack to min(3, floor(t * 4f).toInt())
        }
        if (p.anim == "jump") {
            val f = when {
                p.vy < -60f -> 0
                p.vy < -10f -> 1
                p.vy < 40f -> 2
                else -> 3
            }
            return a.knightJump to f
        }
        if (p.anim == "run") return a.knightRun to ((p.animT * 10).toInt() % 4)
        return a.knightIdle to ((p.animT * 6).toInt() % 4)
    }

    private fun drawEnemies(canvas: Canvas, a: Assets) {
        for (e in engine.enemies) {
            if (e.gone) continue
            val fade = if (e.dying) max(0f, 1f - e.animT / 0.55f) else 1f
            pix.alpha = (255 * fade).toInt()
            val lunge = if (e.anim == "attack") e.facing * 3f else 0f
            val bmp: Bitmap
            val frame: Int
            if (e.kind == "slime") {
                bmp = if (e.dying) a.slimeDeath else a.slimeIdle
                frame = if (e.dying) min(3, floor(e.animT * 8f).toInt()) else (e.animT * 6).toInt() % 4
                val squash = if (e.anim == "attack") 0.82f else 1f
                val dh = 18f * squash
                drawSheet(canvas, bmp, frame, e.x + e.w / 2f - 11f + lunge, e.y + e.h - dh + 1f, 22f, dh, e.facing < 0)
            } else if (e.kind == "dragon") {
                bmp = when {
                    e.dying -> a.dragonIdle
                    e.anim == "attack" -> a.dragonAttack
                    e.anim == "run" || abs(e.vx) > 8f -> a.dragonRun
                    else -> a.dragonIdle
                }
                frame = when {
                    e.dying -> min(3, floor(e.animT * 6f).toInt())
                    e.anim == "attack" -> min(3, floor(e.animT * 10f).toInt())
                    else -> (e.animT * 8).toInt() % 4
                }
                val dw = DRAGON_DRAW_W
                val dh = DRAGON_DRAW_H
                val lungeD = if (e.anim == "attack") e.facing * 8f else 0f
                drawSheet(
                    canvas, bmp, frame,
                    e.x + e.w / 2f - dw / 2f + lungeD,
                    e.y + e.h - dh + 2f,
                    dw, dh, e.facing < 0,
                )
            } else {
                bmp = if (e.dying) a.batDeath else a.batIdle
                frame = if (e.dying) min(3, floor(e.animT * 8f).toInt()) else (e.animT * 8).toInt() % 4
                drawSheet(canvas, bmp, frame, e.x + e.w / 2f - 11f + lunge, e.y + e.h / 2f - 9f, 22f, 18f, e.facing < 0)
            }
            pix.alpha = 255
            if (!e.dying) {
                val maxHp = if (e.kind == "dragon") DRAGON_HP else ENEMY_HP
                if (e.hp < maxHp) {
                    val bar = if (e.kind == "dragon") 45f else 10f
                    val hx = e.x + e.w / 2f - bar / 2f
                    val hy = if (e.kind == "dragon") e.y + e.h - DRAGON_DRAW_H - 6f else e.y - 4f
                    fill.color = Color.parseColor("#5A221C")
                    canvas.drawRect(hx, hy, hx + bar, hy + 1.5f, fill)
                    fill.color = Color.parseColor("#C45A48")
                    canvas.drawRect(hx, hy, hx + bar * e.hp / maxHp, hy + 1.5f, fill)
                }
            }
        }
    }

    private fun drawPickups(canvas: Canvas, a: Assets) {
        val now = System.currentTimeMillis()
        for (item in engine.pickups) {
            if (item.taken) continue
            val bob = sin(item.bobT.toDouble()).toFloat() * 1.6f
            if (item.kind == "coin") {
                val frame = ((now / 140) % 4).toInt()
                drawSheet(canvas, a.coin, frame, item.x - 2f, item.y + bob - 2f, 14f, 14f, false)
            } else {
                val img = if (item.kind == "heal") a.heal else a.mana
                dst.set(item.x - 1f, item.y + bob - 2f, item.x + 13f, item.y + bob + 14f)
                canvas.drawBitmap(img, null, dst, pix)
            }
        }
    }

    private fun drawProjectiles(canvas: Canvas, a: Assets) {
        for (b in engine.projectiles) {
            val frame = (((1.4f - b.life) * 10).toInt() % 4)
            drawSheet(canvas, a.fireball, frame, b.x - 4f, b.y - 6f, 18f, 16f, b.vx < 0f)
        }
    }

    private fun drawFloaters(canvas: Canvas) {
        val text = Paint(Paint.ANTI_ALIAS_FLAG)
        text.color = Color.parseColor("#EFE6D8")
        text.textSize = 8f
        text.textAlign = Paint.Align.CENTER
        for (f in engine.floaters) {
            text.alpha = (255f * min(1f, f.t * 1.4f)).toInt().coerceIn(0, 255)
            canvas.drawText(f.text, f.x, f.y, text)
        }
    }

    private fun drawSlash(canvas: Canvas, a: Assets) {
        val p = engine.player
        if (p.attackT <= 0f) return
        val t = 1f - p.attackT / ATTACK_TIME
        val frame = min(3, floor(t * 4f).toInt())
        val dx = if (p.facing > 0) p.x + p.w - 2f else p.x - 20f
        pix.alpha = 230
        drawSheet(canvas, a.slash, frame, dx, p.y, 22f, 18f, p.facing < 0)
        pix.alpha = 255
    }
}

class Assets(private val ctx: Context) {
    private fun load(path: String): Bitmap {
        return ctx.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)
                ?: throw IllegalStateException("missing $path")
        }
    }
    val knightIdle = load("sprites/knight/idle.png")
    val knightRun = load("sprites/knight/run.png")
    val knightJump = load("sprites/knight/jump.png")
    val knightAttack = load("sprites/knight/attack.png")
    val slimeIdle = load("sprites/slime/idle.png")
    val slimeDeath = load("sprites/slime/death.png")
    val batIdle = load("sprites/bat/idle.png")
    val batDeath = load("sprites/bat/death.png")
    val dragonIdle = load("sprites/dragon/idle.png")
    val dragonAttack = load("sprites/dragon/attack.png")
    val dragonRun = load("sprites/dragon/run.png")
    val flag = load("sprites/flag/idle.png")
    val slash = load("sprites/fx/slash.png")
    val fireball = load("sprites/fx/fireball.png")
    val heal = load("sprites/pickups/heal.png")
    val mana = load("sprites/pickups/mana.png")
    val coin = load("sprites/pickups/coin.png")
    val rock = load("sprites/tiles/rock.jpg")
    val grass = load("sprites/tiles/grass.jpg")
    val dirt = load("sprites/tiles/dirt.jpg")
    val stone = load("sprites/tiles/stone.jpg")
    val bg = load("map/cave-far-bg.jpg")
}
