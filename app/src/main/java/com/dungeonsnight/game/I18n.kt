package com.dungeonsnight.game

object I18n {
    val codes = arrayOf("en", "de", "fr", "zh", "ja")
    val nativeNames = arrayOf("English", "Deutsch", "Français", "中文", "日本語")

    var lang: String = "en"
        private set

    fun parse(raw: String?): String {
        val v = raw ?: return "en"
        return if (codes.contains(v)) v else "en"
    }

    fun set(code: String): String {
        lang = parse(code)
        return lang
    }

    fun indexOf(code: String): Int = codes.indexOf(parse(code)).coerceAtLeast(0)

    fun t(key: String): String = table[lang]?.get(key) ?: table["en"]?.get(key) ?: key

    fun levelTitle(id: Int): String = t("level$id")

    fun fill(key: String, vars: Map<String, String>): String {
        var s = t(key)
        for ((k, v) in vars) s = s.replace("{$k}", v)
        return s
    }

    private val en = mapOf(
        "eyebrow" to "A torchlit descent",
        "title1" to "Dungeons",
        "title2" to "in the Night",
        "blurb" to "Three halls. Slimes, bats, a red banner at the far end. The next door opens only after the last.",
        "start" to "Start Game",
        "selectLevel" to "Select Level",
        "settings" to "Settings",
        "volume" to "Volume",
        "language" to "Language",
        "back" to "Back",
        "paused" to "Paused",
        "resume" to "Resume",
        "menu" to "Menu",
        "gameOver" to "Game Over",
        "torchOut" to "The torch goes out",
        "restart" to "Restart",
        "youWin" to "You Win",
        "bannerYours" to "The banner is yours",
        "nextYes" to "Yes — next level",
        "nextNo" to "No — menu",
        "nightYours" to "The night is yours. All three halls are quiet.",
        "nextHall" to "Next hall: {name}. Continue?",
        "open" to "Open",
        "locked" to "Locked",
        "hpRestored" to "HP restored",
        "manaRestored" to "Mana restored",
        "level1" to "Ember Halls",
        "level2" to "Forked Dark",
        "level3" to "Night's Crown",
    )

    private val de = mapOf(
        "eyebrow" to "Ein fackelheller Abstieg",
        "title1" to "Dungeons",
        "title2" to "in der Nacht",
        "blurb" to "Drei Hallen. Schleime, Fledermäuse, ein rotes Banner am Ende. Die nächste Tür öffnet sich erst nach der letzten.",
        "start" to "Spiel starten",
        "selectLevel" to "Level wählen",
        "settings" to "Einstellungen",
        "volume" to "Lautstärke",
        "language" to "Sprache",
        "back" to "Zurück",
        "paused" to "Pause",
        "resume" to "Weiter",
        "menu" to "Menü",
        "gameOver" to "Spiel vorbei",
        "torchOut" to "Die Fackel erlischt",
        "restart" to "Nochmal",
        "youWin" to "Sieg",
        "bannerYours" to "Das Banner ist dein",
        "nextYes" to "Ja — nächstes Level",
        "nextNo" to "Nein — Menü",
        "nightYours" to "Die Nacht gehört dir. Alle drei Hallen sind still.",
        "nextHall" to "Nächste Halle: {name}. Weiter?",
        "open" to "Offen",
        "locked" to "Gesperrt",
        "hpRestored" to "LP wiederhergestellt",
        "manaRestored" to "Mana wiederhergestellt",
        "level1" to "Gluthallen",
        "level2" to "Gegabelte Dunkelheit",
        "level3" to "Krone der Nacht",
    )

    private val fr = mapOf(
        "eyebrow" to "Une descente aux flambeaux",
        "title1" to "Dungeons",
        "title2" to "dans la Nuit",
        "blurb" to "Trois salles. Slimes, chauves-souris, une bannière rouge au fond. La porte suivante ne s’ouvre qu’après la dernière.",
        "start" to "Nouvelle partie",
        "selectLevel" to "Choisir le niveau",
        "settings" to "Réglages",
        "volume" to "Volume",
        "language" to "Langue",
        "back" to "Retour",
        "paused" to "Pause",
        "resume" to "Reprendre",
        "menu" to "Menu",
        "gameOver" to "Partie terminée",
        "torchOut" to "La torche s’éteint",
        "restart" to "Recommencer",
        "youWin" to "Victoire",
        "bannerYours" to "La bannière est à toi",
        "nextYes" to "Oui — niveau suivant",
        "nextNo" to "Non — menu",
        "nightYours" to "La nuit t’appartient. Les trois salles sont silencieuses.",
        "nextHall" to "Salle suivante : {name}. Continuer ?",
        "open" to "Ouvert",
        "locked" to "Verrouillé",
        "hpRestored" to "PV restaurés",
        "manaRestored" to "Mana restauré",
        "level1" to "Salles de Braise",
        "level2" to "Ténèbres fourchues",
        "level3" to "Couronne de la Nuit",
    )

    private val zh = mapOf(
        "eyebrow" to "火把照亮的下潜",
        "title1" to "暗夜",
        "title2" to "地牢",
        "blurb" to "三座大厅。史莱姆、蝙蝠，尽头一面红旗。只有通关上一关，下一扇门才会打开。",
        "start" to "开始游戏",
        "selectLevel" to "选择关卡",
        "settings" to "设置",
        "volume" to "音量",
        "language" to "语言",
        "back" to "返回",
        "paused" to "暂停",
        "resume" to "继续",
        "menu" to "菜单",
        "gameOver" to "游戏结束",
        "torchOut" to "火把熄灭了",
        "restart" to "再来一次",
        "youWin" to "胜利",
        "bannerYours" to "旗帜属于你",
        "nextYes" to "是 — 下一关",
        "nextNo" to "否 — 菜单",
        "nightYours" to "黑夜属于你。三座大厅都已沉寂。",
        "nextHall" to "下一关：{name}。继续？",
        "open" to "已解锁",
        "locked" to "未解锁",
        "hpRestored" to "生命已恢复",
        "manaRestored" to "魔力已恢复",
        "level1" to "余烬大厅",
        "level2" to "分叉暗道",
        "level3" to "夜之王冠",
    )

    private val ja = mapOf(
        "eyebrow" to "松明に照らされた降下",
        "title1" to "ダンジョン",
        "title2" to "イン・ザ・ナイト",
        "blurb" to "三つの広間。スライム、コウモリ、奥には赤い旗。前の階をクリアしないと、次の扉は開かない。",
        "start" to "ゲームスタート",
        "selectLevel" to "レベル選択",
        "settings" to "設定",
        "volume" to "音量",
        "language" to "言語",
        "back" to "戻る",
        "paused" to "一時停止",
        "resume" to "再開",
        "menu" to "メニュー",
        "gameOver" to "ゲームオーバー",
        "torchOut" to "松明が消えた",
        "restart" to "やり直す",
        "youWin" to "クリア",
        "bannerYours" to "旗はお前のもの",
        "nextYes" to "はい — 次のレベル",
        "nextNo" to "いいえ — メニュー",
        "nightYours" to "夜はお前のもの。三つの広間は静まり返った。",
        "nextHall" to "次の広間：{name}。続ける？",
        "open" to "開放",
        "locked" to "未開放",
        "hpRestored" to "HP回復",
        "manaRestored" to "マナ回復",
        "level1" to "残り火の広間",
        "level2" to "分かれ道の闇",
        "level3" to "夜の王冠",
    )

    private val table = mapOf(
        "en" to en,
        "de" to de,
        "fr" to fr,
        "zh" to zh,
        "ja" to ja,
    )
}
