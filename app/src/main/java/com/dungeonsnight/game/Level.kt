package com.dungeonsnight.game

const val TILE = 16f
const val T_EMPTY: Byte = 0
const val T_SOLID: Byte = 1
const val T_ONEWAY: Byte = 2
const val LEVEL_COUNT = 3

val LEVEL_NAMES = arrayOf("Ember Halls", "Forked Dark", "Night's Crown")

data class EnemySpawn(val kind: String, val x: Float, val y: Float)
data class Torch(val x: Float, val y: Float)

class LevelData(
    val id: Int,
    val name: String,
    val cols: Int,
    val rows: Int,
    val tiles: ByteArray,
    val spawnX: Float,
    val spawnY: Float,
    val flagX: Float,
    val flagY: Float,
    val enemies: List<EnemySpawn>,
    val torches: List<Torch>,
) {
    val worldW: Float get() = cols * TILE
    val worldH: Float get() = rows * TILE

    fun tileAt(c: Int, r: Int): Byte {
        if (c < 0 || c >= cols || r < 0) return T_SOLID
        if (r >= rows) return T_EMPTY
        return tiles[r * cols + c]
    }
}

private fun idx(cols: Int, c: Int, r: Int) = r * cols + c

private fun fill(t: ByteArray, cols: Int, rows: Int, c0: Int, r0: Int, c1: Int, r1: Int, v: Byte) {
    for (r in r0..r1) for (c in c0..c1) {
        if (c in 0 until cols && r in 0 until rows) t[idx(cols, c, r)] = v
    }
}

private fun fillEarth(t: ByteArray, cols: Int, rows: Int) {
    for (c in 0 until cols) {
        var lowest = -1
        for (r in 2 until rows) if (t[idx(cols, c, r)] == T_SOLID) lowest = r
        if (lowest >= 2) for (r in lowest + 1 until rows) t[idx(cols, c, r)] = T_SOLID
    }
}

private fun shell(t: ByteArray, cols: Int, rows: Int) {
    fill(t, cols, rows, 0, 0, cols - 1, 1, T_SOLID)
    fill(t, cols, rows, 0, 1, 1, rows - 1, T_SOLID)
    fill(t, cols, rows, cols - 2, 1, cols - 1, rows - 1, T_SOLID)
}

private fun ground(t: ByteArray, cols: Int, rows: Int, c0: Int, r: Int, c1: Int) =
    fill(t, cols, rows, c0, r, c1, r, T_SOLID)

private fun ledge(t: ByteArray, cols: Int, rows: Int, c0: Int, r: Int, c1: Int) =
    fill(t, cols, rows, c0, r, c1, r, T_ONEWAY)

private fun slime(c: Int, floor: Int) = EnemySpawn("slime", c * TILE, floor * TILE - 10f)
private fun bat(c: Int, r: Int, extra: Float = 0f) = EnemySpawn("bat", c * TILE, r * TILE + extra)
private fun torches(cols: IntArray, yRow: Int) = cols.map { Torch(it * TILE + 8f, yRow * TILE) }

private fun level1(): LevelData {
    val cols = 132
    val rows = 16
    val t = ByteArray(cols * rows)
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
    fill(t, cols, rows, 118, 11, 129, 12, T_SOLID)
    fill(t, cols, rows, 121, 10, 127, 10, T_SOLID)
    fillEarth(t, cols, rows)
    return LevelData(
        1, LEVEL_NAMES[0], cols, rows, t,
        3 * TILE + 3f, 13 * TILE - 14f,
        123 * TILE + 2f, 10 * TILE - 26f,
        listOf(slime(34, 13), slime(76, 11), slime(108, 13), bat(56, 4), bat(84, 3), bat(101, 3, 8f)),
        torches(intArrayOf(12, 36, 55, 76, 102, 124), 6),
    )
}

private fun level2(): LevelData {
    val cols = 148
    val rows = 26
    val t = ByteArray(cols * rows)
    shell(t, cols, rows)
    ground(t, cols, rows, 2, 16, 22)
    fill(t, cols, rows, 16, 15, 22, 15, T_SOLID)
    ledge(t, cols, rows, 8, 12, 15)
    ground(t, cols, rows, 23, 22, 39)
    ground(t, cols, rows, 46, 22, 62)
    ledge(t, cols, rows, 39, 18, 47)
    ledge(t, cols, rows, 18, 13, 26)
    ledge(t, cols, rows, 24, 10, 34)
    ledge(t, cols, rows, 32, 7, 58)
    ledge(t, cols, rows, 50, 5, 64)
    ledge(t, cols, rows, 54, 19, 59)
    ledge(t, cols, rows, 57, 15, 63)
    ledge(t, cols, rows, 60, 11, 66)
    ground(t, cols, rows, 63, 20, 84)
    ledge(t, cols, rows, 72, 14, 92)
    ledge(t, cols, rows, 86, 9, 102)
    ground(t, cols, rows, 96, 23, 118)
    ledge(t, cols, rows, 92, 17, 99)
    ledge(t, cols, rows, 112, 19, 120)
    ledge(t, cols, rows, 118, 15, 128)
    ledge(t, cols, rows, 124, 11, 134)
    ledge(t, cols, rows, 130, 8, 140)
    ground(t, cols, rows, 134, 12, cols - 1)
    fill(t, cols, rows, 138, 11, 145, 11, T_SOLID)
    fill(t, cols, rows, 140, 10, 146, 10, T_SOLID)
    fillEarth(t, cols, rows)
    return LevelData(
        2, LEVEL_NAMES[1], cols, rows, t,
        4 * TILE + 3f, 16 * TILE - 14f,
        141 * TILE + 2f, 10 * TILE - 26f,
        listOf(
            slime(18, 16), slime(30, 22), slime(52, 22), slime(74, 20), slime(108, 23),
            bat(40, 5), bat(56, 4), bat(88, 6), bat(126, 6),
        ),
        torches(intArrayOf(10, 28, 50), 6) + torches(intArrayOf(36, 70), 14) + torches(intArrayOf(104, 122, 140), 7),
    )
}

private fun level3(): LevelData {
    val cols = 160
    val rows = 28
    val t = ByteArray(cols * rows)
    shell(t, cols, rows)
    ground(t, cols, rows, 2, 24, 16)
    ledge(t, cols, rows, 6, 20, 12)
    ledge(t, cols, rows, 14, 21, 18)
    ledge(t, cols, rows, 17, 17, 22)
    ledge(t, cols, rows, 14, 13, 19)
    ledge(t, cols, rows, 18, 9, 25)
    ledge(t, cols, rows, 22, 6, 30)
    ledge(t, cols, rows, 28, 6, 38)
    ledge(t, cols, rows, 36, 9, 46)
    ledge(t, cols, rows, 44, 6, 54)
    ledge(t, cols, rows, 52, 4, 60)
    ground(t, cols, rows, 58, 23, 78)
    ledge(t, cols, rows, 56, 14, 62)
    ledge(t, cols, rows, 60, 18, 68)
    ground(t, cols, rows, 80, 20, 96)
    ledge(t, cols, rows, 90, 15, 104)
    ground(t, cols, rows, 102, 22, 118)
    ledge(t, cols, rows, 112, 12, 126)
    ledge(t, cols, rows, 118, 8, 130)
    ledge(t, cols, rows, 124, 20, 130)
    ledge(t, cols, rows, 128, 16, 136)
    ledge(t, cols, rows, 132, 12, 140)
    ledge(t, cols, rows, 126, 8, 132)
    ground(t, cols, rows, 136, 18, 148)
    ground(t, cols, rows, 146, 10, cols - 1)
    fill(t, cols, rows, 150, 9, 157, 9, T_SOLID)
    fill(t, cols, rows, 152, 8, 158, 8, T_SOLID)
    ledge(t, cols, rows, 142, 13, 148)
    fillEarth(t, cols, rows)
    return LevelData(
        3, LEVEL_NAMES[2], cols, rows, t,
        4 * TILE + 3f, 24 * TILE - 14f,
        153 * TILE + 2f, 8 * TILE - 26f,
        listOf(
            slime(10, 24), slime(66, 23), slime(88, 20), slime(110, 22), slime(140, 18),
            bat(20, 5), bat(40, 3), bat(50, 2), bat(96, 8), bat(122, 5), bat(134, 6),
        ),
        torches(intArrayOf(8, 24, 48), 4) + torches(intArrayOf(66, 86, 110), 12) + torches(intArrayOf(130, 152), 5),
    )
}

fun buildLevel(id: Int): LevelData = when (id.coerceIn(1, LEVEL_COUNT)) {
    2 -> level2()
    3 -> level3()
    else -> level1()
}
