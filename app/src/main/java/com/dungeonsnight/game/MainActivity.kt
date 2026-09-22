package com.dungeonsnight.game

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), GameView.Listener {

    private lateinit var game: GameView
    private lateinit var save: Save
    private lateinit var audio: AudioHub

    private lateinit var menu: View
    private lateinit var levels: View
    private lateinit var settings: View
    private lateinit var pause: View
    private lateinit var win: View
    private lateinit var dead: View
    private lateinit var hud: View
    private lateinit var touch: View
    private lateinit var btnPause: View
    private lateinit var hpRow: LinearLayout
    private lateinit var hudLevel: TextView
    private lateinit var xpLabel: TextView
    private lateinit var winXp: TextView
    private lateinit var winHint: TextView
    private lateinit var btnNextYes: Button
    private lateinit var volumeBar: SeekBar
    private lateinit var volumeValue: TextView
    private lateinit var btnL1: Button
    private lateinit var btnL2: Button
    private lateinit var btnL3: Button
    private var dpadJump = false
    private var faceJump = false

    private fun applyJump() {
        game.engine.input.jumpHeld = dpadJump || faceJump
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        save = Save(this)
        audio = AudioHub(this)
        audio.setVolume(save.volume)

        game = findViewById(R.id.gameView)
        game.listener = this
        menu = findViewById(R.id.menuOverlay)
        levels = findViewById(R.id.levelsOverlay)
        settings = findViewById(R.id.settingsOverlay)
        pause = findViewById(R.id.pauseOverlay)
        win = findViewById(R.id.winOverlay)
        dead = findViewById(R.id.deadOverlay)
        hud = findViewById(R.id.hud)
        touch = findViewById(R.id.touchBar)
        btnPause = findViewById(R.id.btnPause)
        hpRow = findViewById(R.id.hpRow)
        hudLevel = findViewById(R.id.hudLevel)
        xpLabel = findViewById(R.id.xpLabel)
        winXp = findViewById(R.id.winXp)
        winHint = findViewById(R.id.winHint)
        btnNextYes = findViewById(R.id.btnNextYes)
        volumeBar = findViewById(R.id.volumeBar)
        volumeValue = findViewById(R.id.volumeValue)
        btnL1 = findViewById(R.id.btnL1)
        btnL2 = findViewById(R.id.btnL2)
        btnL3 = findViewById(R.id.btnL3)

        findViewById<Button>(R.id.btnStart).setOnClickListener { startLevel(1) }
        findViewById<Button>(R.id.btnLevels).setOnClickListener { showLevels() }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { showSettings() }
        findViewById<Button>(R.id.btnExit).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnLevelsBack).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.btnSettingsBack).setOnClickListener { showMenu() }
        findViewById<Button>(R.id.btnResume).setOnClickListener {
            game.engine.paused = false
            pause.visibility = View.GONE
        }
        findViewById<Button>(R.id.btnPauseExit).setOnClickListener { toMenuFromPlay() }
        findViewById<Button>(R.id.btnRestart).setOnClickListener {
            dead.visibility = View.GONE
            startLevel(game.engine.levelId)
        }
        findViewById<Button>(R.id.btnDeadExit).setOnClickListener { toMenuFromPlay() }
        btnNextYes.setOnClickListener {
            val next = game.engine.levelId + 1
            win.visibility = View.GONE
            if (next <= LEVEL_COUNT) startLevel(next) else toMenuFromPlay()
        }
        findViewById<Button>(R.id.btnNextNo).setOnClickListener { toMenuFromPlay() }
        btnPause.setOnClickListener {
            game.engine.togglePause()
            pause.visibility = if (game.engine.paused) View.VISIBLE else View.GONE
        }

        hold(R.id.btnLeft) { game.engine.input.moveX = if (it) -1f else if (game.engine.input.moveX < 0f) 0f else game.engine.input.moveX }
        hold(R.id.btnRight) { game.engine.input.moveX = if (it) 1f else if (game.engine.input.moveX > 0f) 0f else game.engine.input.moveX }
        hold(R.id.btnJump) { faceJump = it; applyJump() }
        hold(R.id.btnJumpDpad) { dpadJump = it; applyJump() }
        hold(R.id.btnDrop) { game.engine.input.dropHeld = it }
        hold(R.id.btnAttack) { game.engine.input.attackHeld = it }
        hold(R.id.btnSpell) { game.engine.input.spellHeld = it }

        btnL1.setOnClickListener { startLevel(1) }
        btnL2.setOnClickListener { startLevel(2) }
        btnL3.setOnClickListener { startLevel(3) }

        volumeBar.progress = (save.volume * 100).toInt()
        volumeValue.text = volumeBar.progress.toString()
        volumeBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val v = progress / 100f
                save.volume = v
                audio.setVolume(v)
                volumeValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        showMenu()
        buildHp(PLAYER_MAX_HP)
    }

    private fun hold(id: Int, fn: (Boolean) -> Unit) {
        findViewById<View>(id).setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { v.performClick(); fn(true) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> fn(false)
            }
            true
        }
    }

    private fun startLevel(id: Int) {
        if (id > save.unlocked) return
        game.startLevel(id)
        audio.start()
        audio.startMusic()
        hideAll()
        hud.visibility = View.VISIBLE
        touch.visibility = View.VISIBLE
        btnPause.visibility = View.VISIBLE
        hudLevel.text = game.engine.level.name
    }

    private fun toMenuFromPlay() {
        audio.stopMusic()
        game.engine.reset(1)
        game.engine.paused = false
        dpadJump = false
        faceJump = false
        game.engine.input.jumpHeld = false
        showMenu()
    }

    private fun showMenu() {
        hideAll()
        menu.visibility = View.VISIBLE
        xpLabel.text = "XP  ${save.xp}"
        audio.stopMusic()
    }

    private fun showLevels() {
        hideAll()
        levels.visibility = View.VISIBLE
        btnL1.isEnabled = save.unlocked >= 1
        btnL2.isEnabled = save.unlocked >= 2
        btnL3.isEnabled = save.unlocked >= 3
        btnL2.alpha = if (save.unlocked >= 2) 1f else 0.4f
        btnL3.alpha = if (save.unlocked >= 3) 1f else 0.4f
    }

    private fun showSettings() {
        hideAll()
        settings.visibility = View.VISIBLE
        volumeBar.progress = (save.volume * 100).toInt()
    }

    private fun hideAll() {
        menu.visibility = View.GONE
        levels.visibility = View.GONE
        settings.visibility = View.GONE
        pause.visibility = View.GONE
        win.visibility = View.GONE
        dead.visibility = View.GONE
        hud.visibility = View.GONE
        touch.visibility = View.GONE
        btnPause.visibility = View.GONE
    }

    private fun buildHp(max: Int) {
        hpRow.removeAllViews()
        repeat(max) {
            val v = View(this)
            val lp = LinearLayout.LayoutParams(14, 14)
            lp.marginEnd = 4
            v.layoutParams = lp
            val d = GradientDrawable()
            d.setColor(Color.parseColor("#2A2118"))
            d.cornerRadius = 2f
            v.background = d
            hpRow.addView(v)
        }
    }

    override fun onHud(hp: Int, mana: Int, coins: Int, phase: Phase, paused: Boolean, levelName: String) {
        hudLevel.text = levelName
        for (i in 0 until hpRow.childCount) {
            val d = GradientDrawable()
            d.setColor(if (i < hp) Color.parseColor("#C45A48") else Color.parseColor("#2A2118"))
            d.cornerRadius = 2f
            hpRow.getChildAt(i).background = d
        }
        val manaCell = findViewById<View>(R.id.manaCell)
        val md = GradientDrawable()
        md.setColor(if (mana > 0) Color.parseColor("#C8C2B4") else Color.parseColor("#2A2118"))
        md.cornerRadius = 2f
        manaCell.background = md
        findViewById<TextView>(R.id.hudCoins).text = "$coins/3"
    }

    override fun onWin() {
        audio.stopMusic()
        audio.win()
        save.addXp(WIN_XP)
        save.unlock(game.engine.levelId + 1)
        winXp.text = "+$WIN_XP XP    total ${save.xp}"
        val hasNext = game.engine.levelId < LEVEL_COUNT
        winHint.text = if (hasNext) {
            "Next hall: ${LEVEL_NAMES[game.engine.levelId]}. Continue?"
        } else {
            getString(R.string.night_yours)
        }
        btnNextYes.visibility = if (hasNext) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnNextNo).text = if (hasNext) getString(R.string.next_no) else getString(R.string.exit)
        win.visibility = View.VISIBLE
    }

    override fun onDead() {
        audio.stopMusic()
        audio.death()
        dead.visibility = View.VISIBLE
    }

    override fun onEvent(ev: GameEvent) {
        when (ev) {
            GameEvent.Jump -> audio.jump()
            is GameEvent.Land -> audio.land()
            GameEvent.Swing -> audio.swing()
            GameEvent.Spell -> audio.swing()
            is GameEvent.Heal -> audio.win()
            is GameEvent.Mana -> audio.win()
            is GameEvent.Coin -> {
                audio.hit()
                save.addCoins(1)
            }
            is GameEvent.Hit -> {
                audio.hit()
                game.trauma = minOf(1f, game.trauma + 0.35f)
            }
            is GameEvent.Hurt -> {
                audio.hurt()
                game.trauma = minOf(1f, game.trauma + 0.55f)
            }
            is GameEvent.EnemyDie -> audio.death()
            else -> {}
        }
    }

    override fun onPause() {
        super.onPause()
        if (hud.visibility == View.VISIBLE && game.engine.phase == Phase.PLAY) {
            game.engine.paused = true
            pause.visibility = View.VISIBLE
        }
        audio.stopMusic()
    }

    override fun onResume() {
        super.onResume()
        if (hud.visibility == View.VISIBLE && game.engine.phase == Phase.PLAY && !game.engine.paused) {
            audio.startMusic()
        }
    }

    override fun onDestroy() {
        audio.release()
        super.onDestroy()
    }
}
