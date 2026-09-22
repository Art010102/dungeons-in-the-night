package com.dungeonsnight.game

import android.content.Context

class Save(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("dungeons_in_the_night", Context.MODE_PRIVATE)

    var xp: Int
        get() = prefs.getInt("xp", 0)
        set(v) { prefs.edit().putInt("xp", v).apply() }

    var unlocked: Int
        get() = prefs.getInt("unlocked", 1).coerceIn(1, LEVEL_COUNT)
        set(v) { prefs.edit().putInt("unlocked", v.coerceIn(1, LEVEL_COUNT)).apply() }

    var volume: Float
        get() = prefs.getFloat("volume", 0.8f).coerceIn(0f, 1f)
        set(v) { prefs.edit().putFloat("volume", v.coerceIn(0f, 1f)).apply() }

    var coins: Int
        get() = prefs.getInt("coins", 0)
        set(v) { prefs.edit().putInt("coins", v).apply() }

    fun addCoins(amount: Int): Int {
        coins += amount
        return coins
    }

    fun addXp(amount: Int): Int {
        xp += amount
        return xp
    }

    fun unlock(id: Int): Int {
        unlocked = maxOf(unlocked, id.coerceAtMost(LEVEL_COUNT))
        return unlocked
    }
}
