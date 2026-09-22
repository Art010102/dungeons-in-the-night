package com.dungeonsnight.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

const val PLAYER_MAX_HP = 10
const val PLAYER_MAX_MANA = 1
const val ENEMY_HP = 5
const val DRAGON_HP = 10
const val PLAYER_DMG = 1
const val ENEMY_DMG = 2
const val DRAGON_DMG = 3
const val SPELL_DMG = 8
const val WIN_XP = 100
const val STEP = 1f / 60f
const val ATTACK_TIME = 0.3f

const val PW = 10f
const val PH = 14f
const val EW = 12f
const val EH = 10f
const val DW = 70f
const val DH = 88f
const val DRAGON_DRAW_W = 175f
const val DRAGON_DRAW_H = 140f
private const val MOVE_SPEED = 110f
private const val ACCEL_GND = 980f
private const val ACCEL_AIR = 560f
private const val FRICTION = 1600f
private const val JUMP_VEL = -258f
private const val GRAV_UP = 500f
private const val GRAV_DOWN = 1080f
private const val GRAV_APEX = 240f
private const val APEX_ABS = 32f
private const val TERMINAL = 310f
private const val COYOTE = 0.1f
private const val BUFFER = 0.13f
private const val JUMP_CUT = 0.46f
private const val DROP_TIME = 0.16f
private const val ATTACK_ACTIVE0 = 0.06f
private const val ATTACK_ACTIVE1 = 0.2f
private const val INVULN = 0.72f
private const val HITSTOP = 0.045f

enum class Phase { PLAY, WIN, DEAD }

data class InputState(
    var moveX: Float = 0f,
    var jumpHeld: Boolean = false,
    var attackHeld: Boolean = false,
    var spellHeld: Boolean = false,
    var dropHeld: Boolean = false,
)

class Player {
    var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var w = PW; var h = PH
    var facing = 1
    var hp = PLAYER_MAX_HP
    var mana = PLAYER_MAX_MANA
    var grounded = true
    var anim = "idle"
    var animT = 0f
    var attackT = 0f
    var attackHit = false
    var invuln = 0f
}

class Enemy {
    var id = 0
    var kind = "slime"
    var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
    var w = EW; var h = EH
    var facing = -1
    var hp = ENEMY_HP
    var anim = "idle"
    var animT = 0f
    var attackT = 0f
    var attackCd = 0f
    var attackHit = false
    var aggro = false
    var dying = false
    var gone = false
    var bobT = 0f
    var grounded = true
}

class Pickup {
    var kind = "coin"
    var x = 0f; var y = 0f; var w = 10f; var h = 10f
    var taken = false
    var bobT = 0f
}

class Projectile {
    var x = 0f; var y = 0f; var vx = 0f
    var w = 10f; var h = 8f
    var life = 1.4f
    var hit = false
}

class Floater {
    var text = ""
    var x = 0f; var y = 0f; var t = 1f
}

sealed class GameEvent {
    object Jump : GameEvent()
    class Land(val x: Float, val y: Float) : GameEvent()
    object Swing : GameEvent()
    object Spell : GameEvent()
    class Hit(val x: Float, val y: Float) : GameEvent()
    class Hurt(val x: Float, val y: Float) : GameEvent()
    class EnemyDie(val x: Float, val y: Float) : GameEvent()
    class Heal(val x: Float, val y: Float) : GameEvent()
    class Mana(val x: Float, val y: Float) : GameEvent()
    class Coin(val x: Float, val y: Float) : GameEvent()
    object Win : GameEvent()
    object GameOver : GameEvent()
}

private fun aabb(ax: Float, ay: Float, aw: Float, ah: Float, bx: Float, by: Float, bw: Float, bh: Float) =
    ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

private fun gravity(vy: Float): Float = when {
    vy < 0f && abs(vy) < APEX_ABS -> GRAV_APEX
    vy < 0f -> GRAV_UP
    else -> GRAV_DOWN
}

class Engine(var levelId: Int = 1) {
    var level: LevelData = buildLevel(levelId)
    val player = Player()
    val enemies = mutableListOf<Enemy>()
    val pickups = mutableListOf<Pickup>()
    val projectiles = mutableListOf<Projectile>()
    val floaters = mutableListOf<Floater>()
    var coins = 0
    var phase = Phase.PLAY
    var paused = false
    var hitstop = 0f
    var coyote = 0f
    var jumpBuffer = 0f
    var dropT = 0f
    private var prevJump = false
    private var prevAttack = false
    private var prevSpell = false
    private var acc = 0f
    private val events = mutableListOf<GameEvent>()
    private var nextId = 1
    val input = InputState()
    val worldW get() = level.worldW
    val worldH get() = level.worldH

    init { reset(levelId) }

    fun reset(id: Int = levelId) {
        levelId = id
        level = buildLevel(id)
        nextId = 1
        player.x = level.spawnX; player.y = level.spawnY
        player.vx = 0f; player.vy = 0f
        player.facing = 1; player.hp = PLAYER_MAX_HP; player.mana = PLAYER_MAX_MANA
        player.grounded = true; player.anim = "idle"; player.animT = 0f
        player.attackT = 0f; player.attackHit = false; player.invuln = 0f
        enemies.clear()
        pickups.clear()
        projectiles.clear()
        floaters.clear()
        coins = 0
        for (s in level.enemies) {
            val e = Enemy()
            e.id = nextId++
            e.kind = s.kind
            e.x = s.x; e.y = s.y
            if (s.kind == "dragon") {
                e.w = DW; e.h = DH; e.hp = DRAGON_HP
            }
            e.grounded = s.kind != "bat"
            e.animT = Random.nextFloat() * 0.4f
            e.bobT = Random.nextFloat() * Math.PI.toFloat() * 2f
            enemies.add(e)
        }
        for (s in level.pickups) {
            val p = Pickup()
            p.kind = s.kind; p.x = s.x; p.y = s.y
            if (s.kind == "coin") { p.w = 10f; p.h = 10f } else { p.w = 12f; p.h = 14f }
            pickups.add(p)
        }
        phase = Phase.PLAY
        paused = false
        hitstop = 0f
        coyote = 0f
        jumpBuffer = 0f
        dropT = 0f
        prevJump = false
        prevAttack = false
        prevSpell = false
        acc = 0f
        events.clear()
        input.moveX = 0f; input.jumpHeld = false; input.attackHeld = false; input.spellHeld = false; input.dropHeld = false
    }

    fun togglePause() {
        if (phase == Phase.PLAY) paused = !paused
    }

    fun drainEvents(): List<GameEvent> {
        val out = events.toList()
        events.clear()
        return out
    }

    fun step(dt: Float) {
        if (phase != Phase.PLAY || paused) return
        if (hitstop > 0f) {
            hitstop = max(0f, hitstop - dt)
            return
        }
        acc += min(dt, 0.1f)
        if (acc > 0.25f) acc = 0.25f
        while (acc >= STEP) {
            tick(STEP)
            acc -= STEP
        }
    }

    private fun emit(ev: GameEvent) { events.add(ev) }

    private fun tick(dt: Float) {
        val p = player
        if (input.jumpHeld && !prevJump) jumpBuffer = BUFFER
        if (input.attackHeld && !prevAttack && p.attackT <= 0f) {
            p.attackT = ATTACK_TIME
            p.attackHit = false
            p.animT = 0f
            emit(GameEvent.Swing)
        }
        if (input.spellHeld && !prevSpell) castSpell()
        jumpBuffer = max(0f, jumpBuffer - dt)
        dropT = if (input.dropHeld) DROP_TIME else max(0f, dropT - dt)
        if (p.invuln > 0f) p.invuln -= dt
        if (p.attackT > 0f) p.attackT -= dt
        stepPlayer(dt)
        stepSword()
        stepEnemies(dt)
        stepProjectiles(dt)
        stepPickups()
        stepFloaters(dt)
        checkWin()
        checkFall()
        animatePlayer(dt)
        prevJump = input.jumpHeld
        prevAttack = input.attackHeld
        prevSpell = input.spellHeld
    }

    private fun stepPlayer(dt: Float) {
        val p = player
        val move = input.moveX.coerceIn(-1f, 1f)
        if (move != 0f) p.facing = if (move > 0f) 1 else -1
        val accel = if (p.grounded) ACCEL_GND else ACCEL_AIR
        val target = move * MOVE_SPEED * (if (p.attackT > 0f) 0.45f else 1f)
        if (move != 0f) {
            val dir = sign(target - p.vx).let { if (it == 0f) sign(move) else it }
            p.vx += dir * accel * dt
            if ((dir > 0f && p.vx > target) || (dir < 0f && p.vx < target)) p.vx = target
        } else if (p.grounded) {
            val fr = FRICTION * dt
            p.vx = if (abs(p.vx) <= fr) 0f else p.vx - sign(p.vx) * fr
        } else {
            p.vx *= 1f - 1.2f * dt
        }
        p.vx = p.vx.coerceIn(-MOVE_SPEED, MOVE_SPEED)
        p.vy += gravity(p.vy) * dt
        if (p.vy > TERMINAL) p.vy = TERMINAL
        if (!input.jumpHeld && prevJump && p.vy < 0f) p.vy *= JUMP_CUT
        coyote = if (p.grounded) COYOTE else max(0f, coyote - dt)
        if (jumpBuffer > 0f && coyote > 0f) {
            p.vy = JUMP_VEL
            p.grounded = false
            coyote = 0f
            jumpBuffer = 0f
            emit(GameEvent.Jump)
        }
        moveActor(p, p.vx * dt, 0f, false)
        val groundedBefore = p.grounded
        val prevBottom = p.y + p.h
        moveActor(p, 0f, p.vy * dt, true, prevBottom)
        if (p.grounded && !groundedBefore && p.vy >= 0f) emit(GameEvent.Land(p.x + p.w / 2f, p.y + p.h))
    }

    private fun moveActor(body: Any, dx: Float, dy: Float, doY: Boolean, prevBottom: Float = 0f) {
        val x: () -> Float
        val y: () -> Float
        val w: Float
        val h: Float
        val setX: (Float) -> Unit
        val setY: (Float) -> Unit
        val setVx: (Float) -> Unit
        val setVy: (Float) -> Unit
        val setG: (Boolean) -> Unit
        val getG: () -> Boolean
        if (body is Player) {
            x = { body.x }; y = { body.y }; w = body.w; h = body.h
            setX = { body.x = it }; setY = { body.y = it }
            setVx = { body.vx = it }; setVy = { body.vy = it }
            setG = { body.grounded = it }; getG = { body.grounded }
        } else {
            body as Enemy
            x = { body.x }; y = { body.y }; w = body.w; h = body.h
            setX = { body.x = it }; setY = { body.y = it }
            setVx = { body.vx = it }; setVy = { body.vy = it }
            setG = { body.grounded = it }; getG = { body.grounded }
        }
        val allowDrop = dropT > 0f
        if (!doY) {
            if (dx == 0f) return
            val steps = max(1, ceil(abs(dx) / (TILE * 0.45f)).toInt())
            val step = dx / steps
            for (i in 0 until steps) {
                setX(x() + step)
                if (solveSolidX(x(), y(), w, h, step > 0f, setX)) {
                    setVx(0f)
                    break
                }
            }
            if (x() < TILE) setX(TILE)
            if (x() + w > worldW - TILE) setX(worldW - TILE - w)
            return
        }
        setG(false)
        if (dy == 0f) {
            setG(probeGround(x(), y(), w, h))
            return
        }
        val steps = max(1, ceil(abs(dy) / (TILE * 0.45f)).toInt())
        val step = dy / steps
        for (i in 0 until steps) {
            setY(y() + step)
            val hit = solveSolidY(x(), y(), w, h, step > 0f, allowDrop, if (prevBottom == 0f) y() + h - step else prevBottom, setY)
            if (hit == 1) { setVy(0f); setG(true); break }
            if (hit == 2) { setVy(0f); break }
        }
        if (!getG()) setG(probeGround(x(), y(), w, h))
    }

    private fun solveSolidX(x: Float, y: Float, w: Float, h: Float, goingRight: Boolean, setX: (Float) -> Unit): Boolean {
        val c0 = floor(x / TILE).toInt()
        val c1 = floor((x + w - 0.001f) / TILE).toInt()
        val r0 = floor(y / TILE).toInt()
        val r1 = floor((y + h - 0.001f) / TILE).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            if (level.tileAt(c, r) != T_SOLID) continue
            setX(if (goingRight) c * TILE - w else (c + 1) * TILE)
            return true
        }
        return false
    }

    private fun solveSolidY(
        x: Float, y: Float, w: Float, h: Float,
        goingDown: Boolean, allowDrop: Boolean, prevBottom: Float, setY: (Float) -> Unit,
    ): Int {
        var result = 0
        val c0 = floor(x / TILE).toInt()
        val c1 = floor((x + w - 0.001f) / TILE).toInt()
        val r0 = floor(y / TILE).toInt()
        val r1 = floor((y + h - 0.001f) / TILE).toInt()
        for (r in r0..r1) for (c in c0..c1) {
            val t = level.tileAt(c, r)
            if (t == T_SOLID) {
                if (goingDown) { setY(r * TILE - h); result = 1 } else { setY((r + 1) * TILE); result = 2 }
            } else if (t == T_ONEWAY && goingDown && !allowDrop) {
                val top = r * TILE
                if (prevBottom <= top + 2f) { setY(top - h); result = 1 }
            }
        }
        return result
    }

    private fun probeGround(x: Float, y: Float, w: Float, h: Float): Boolean {
        val yy = y + h
        val c0 = floor(x / TILE).toInt()
        val c1 = floor((x + w - 0.001f) / TILE).toInt()
        val r = floor((yy + 0.6f) / TILE).toInt()
        for (c in c0..c1) {
            val t = level.tileAt(c, r)
            if (t == T_SOLID) return true
            if (t == T_ONEWAY && dropT <= 0f && yy <= r * TILE + 1.5f) return true
        }
        return false
    }

    private fun stepSword() {
        val p = player
        if (p.attackT <= 0f) return
        val elapsed = ATTACK_TIME - p.attackT
        val active = elapsed in ATTACK_ACTIVE0..ATTACK_ACTIVE1
        val bx = if (p.facing > 0) p.x + p.w - 2f else p.x - 18f
        val by = p.y + 1f
        if (!active || p.attackHit) return
        for (e in enemies) {
            if (e.gone || e.dying) continue
            if (!aabb(bx, by, 18f, 13f, e.x, e.y, e.w, e.h)) continue
            e.hp -= PLAYER_DMG
            p.attackHit = true
            e.vx += p.facing * 36f
            if (e.kind == "slime") e.vy = -28f
            hitstop = HITSTOP
            emit(GameEvent.Hit(e.x + e.w / 2f, e.y + e.h / 2f))
            if (e.hp <= 0) killEnemy(e)
            break
        }
    }

    private fun killEnemy(e: Enemy) {
        e.dying = true
        e.hp = 0
        e.anim = "death"
        e.animT = 0f
        e.vy = 30f
        emit(GameEvent.EnemyDie(e.x + e.w / 2f, e.y + e.h / 2f))
    }

    private fun stepEnemies(dt: Float) {
        val p = player
        for (e in enemies) {
            if (e.gone) continue
            e.animT += dt
            e.attackCd = max(0f, e.attackCd - dt)
            if (e.dying) {
                e.vy += GRAV_DOWN * dt
                e.y += e.vy * dt
                e.x += e.vx * dt * 0.4f
                if (e.animT > 0.55f || e.y > worldH + 24f) e.gone = true
                continue
            }
            val cx = e.x + e.w / 2f
            val cy = e.y + e.h / 2f
            val px = p.x + p.w / 2f
            val py = p.y + p.h / 2f
            val dx = px - cx
            val dy = py - cy
            val dist = hypot(dx, dy)
            val range = when (e.kind) {
                "bat" -> 108f
                "dragon" -> 240f
                else -> 86f
            }
            if (dist < range) e.aggro = true
            if (e.attackT > 0f) {
                e.attackT -= dt
                e.anim = "attack"
                val dur = if (e.kind == "dragon") 0.46f else 0.34f
                val elapsed = dur - e.attackT
                if (!e.attackHit && elapsed in 0.12f..0.22f) {
                    val pad = if (e.kind == "dragon") 20f else 2f
                    if (aabb(e.x - pad, e.y - pad, e.w + pad * 2f, e.h + pad * 2f, p.x, p.y, p.w, p.h)) {
                        e.attackHit = true
                        val dmg = if (e.kind == "dragon") DRAGON_DMG else ENEMY_DMG
                        hurtPlayer(if (dx == 0f) e.facing.toFloat() else sign(dx), dmg)
                    }
                }
                if (e.attackT <= 0f) e.anim = if (abs(e.vx) > 8f) "run" else "idle"
            }
            if (e.kind == "bat") {
                e.bobT += dt * 3.2f
                if (e.aggro) {
                    val spd = 52f
                    val nx = if (dist > 1f) dx / dist else 0f
                    val ny = if (dist > 1f) dy / dist else 0f
                    e.vx += (nx * spd - e.vx) * min(1f, 4f * dt)
                    e.vy += (ny * spd - e.vy) * min(1f, 4f * dt)
                } else {
                    e.vx *= 1f - 3f * dt
                    e.vy = sin(e.bobT) * 10f
                }
                if (e.vx != 0f) e.facing = if (e.vx > 0f) 1 else -1
                moveActor(e, e.vx * dt, 0f, false)
                moveActor(e, 0f, e.vy * dt, true, e.y + e.h)
                if (e.grounded) e.y -= 0.4f
            } else {
                if (e.aggro) {
                    val dir = if (dx == 0f) e.facing else sign(dx).toInt()
                    e.facing = if (dir > 0) 1 else -1
                    val ahead = e.x + if (dir > 0) e.w + 2f else -2f
                    val floor = level.tileAt(floor(ahead / TILE).toInt(), floor((e.y + e.h + 1f) / TILE).toInt())
                    val spd = if (e.kind == "dragon") 42f else 34f
                    e.vx = if (floor == T_SOLID || floor == T_ONEWAY) dir * spd else 0f
                } else {
                    e.vx *= 1f - 6f * dt
                }
                e.vy += gravity(e.vy) * dt
                if (e.vy > TERMINAL) e.vy = TERMINAL
                moveActor(e, e.vx * dt, 0f, false)
                moveActor(e, 0f, e.vy * dt, true, e.y + e.h)
            }
            val reach = if (e.kind == "dragon") 35f else 6f
            if (e.attackT <= 0f && e.attackCd <= 0f && e.aggro &&
                aabb(e.x - reach, e.y - reach, e.w + reach * 2f, e.h + reach * 2f, p.x, p.y, p.w, p.h)
            ) {
                e.attackT = if (e.kind == "dragon") 0.46f else 0.34f
                e.attackHit = false
                e.attackCd = if (e.kind == "dragon") 1.05f else 0.85f
                e.animT = 0f
                e.anim = "attack"
            }
        }
    }

    private fun hurtPlayer(fromDir: Float, dmg: Int = ENEMY_DMG) {
        val p = player
        if (p.invuln > 0f || phase != Phase.PLAY) return
        p.hp -= dmg
        p.invuln = INVULN
        p.vx = -fromDir * 52f
        p.vy = -48f
        p.grounded = false
        hitstop = HITSTOP
        emit(GameEvent.Hurt(p.x + p.w / 2f, p.y + p.h / 2f))
        if (p.hp <= 0) {
            p.hp = 0
            phase = Phase.DEAD
            emit(GameEvent.GameOver)
        }
    }

    private fun checkWin() {
        if (phase != Phase.PLAY) return
        val p = player
        val f = level
        if (aabb(p.x, p.y, p.w, p.h, f.flagX, f.flagY, 18f, 28f)) {
            phase = Phase.WIN
            emit(GameEvent.Win)
        }
    }

    private fun checkFall() {
        if (phase != Phase.PLAY) return
        if (player.y > worldH + 8f) {
            player.hp = 0
            phase = Phase.DEAD
            emit(GameEvent.GameOver)
        }
    }

    private fun animatePlayer(dt: Float) {
        val p = player
        val prev = p.anim
        p.anim = when {
            p.attackT > 0f -> "attack"
            !p.grounded -> "jump"
            abs(p.vx) > 12f -> "run"
            else -> "idle"
        }
        if (p.anim != prev) p.animT = 0f else p.animT += dt
    }

    private fun castSpell() {
        val p = player
        if (p.mana < 1 || phase != Phase.PLAY) return
        p.mana -= 1
        val dir = p.facing
        val b = Projectile()
        b.x = if (dir > 0) p.x + p.w else p.x - 10f
        b.y = p.y + 2f
        b.vx = dir * 210f
        projectiles.add(b)
        emit(GameEvent.Spell)
    }

    private fun stepProjectiles(dt: Float) {
        val keep = mutableListOf<Projectile>()
        for (b in projectiles) {
            if (b.hit) continue
            b.life -= dt
            if (b.life <= 0f) continue
            b.x += b.vx * dt
            if (solveSolidX(b.x, b.y, b.w, b.h, b.vx > 0f) { b.x = it }) {
                emit(GameEvent.Hit(b.x + b.w / 2f, b.y + b.h / 2f))
                continue
            }
            var hitEnemy = false
            for (e in enemies) {
                if (e.gone || e.dying) continue
                if (!aabb(b.x, b.y, b.w, b.h, e.x, e.y, e.w, e.h)) continue
                e.hp -= SPELL_DMG
                e.vx += sign(b.vx) * 48f
                if (e.kind != "bat") e.vy = -36f
                hitstop = HITSTOP
                emit(GameEvent.Hit(e.x + e.w / 2f, e.y + e.h / 2f))
                if (e.hp <= 0) killEnemy(e)
                hitEnemy = true
                break
            }
            if (!hitEnemy) keep.add(b)
        }
        projectiles.clear()
        projectiles.addAll(keep)
    }

    private fun stepPickups() {
        val p = player
        for (item in pickups) {
            if (item.taken) continue
            item.bobT += 0.08f
            if (!aabb(p.x, p.y, p.w, p.h, item.x, item.y, item.w, item.h)) continue
            item.taken = true
            val cx = item.x + item.w / 2f
            val cy = item.y
            when (item.kind) {
                "heal" -> {
                    p.hp = PLAYER_MAX_HP
                    floaters.add(Floater().also { it.text = "HP restored"; it.x = cx; it.y = cy; it.t = 1.2f })
                    emit(GameEvent.Heal(cx, cy))
                }
                "mana" -> {
                    p.mana = PLAYER_MAX_MANA
                    floaters.add(Floater().also { it.text = "Mana restored"; it.x = cx; it.y = cy; it.t = 1.2f })
                    emit(GameEvent.Mana(cx, cy))
                }
                else -> {
                    coins += 1
                    floaters.add(Floater().also { it.text = "+1"; it.x = cx; it.y = cy; it.t = 0.9f })
                    emit(GameEvent.Coin(cx, cy))
                }
            }
        }
    }

    private fun stepFloaters(dt: Float) {
        val keep = mutableListOf<Floater>()
        for (f in floaters) {
            f.t -= dt
            f.y -= 18f * dt
            if (f.t > 0f) keep.add(f)
        }
        floaters.clear()
        floaters.addAll(keep)
    }
}

fun lerpCam(from: Float, to: Float, dt: Float): Float {
    val k = 1f - exp(-7f * dt)
    return from + (to - from) * k
}
