# Dungeons in the Night

Нативная Android-игра (Kotlin + Canvas). Интернет не нужен. Все спрайты, земля, музыка и уровни лежат внутри APK.

## Store-картинки (скачай из корня репо)

- [`icon-512.png`](icon-512.png) — иконка 512×512 (она же иконка приложения на телефоне)
- [`banner-1024x500.png`](banner-1024x500.png) — баннер 1024×500 (feature graphic)

## Что это

Рыцарь, слаймы, летучие мыши и большой дракон. Три зала. Следующий уровень открывается только после прохождения предыдущего.

Языки в **Settings → Language** (без перезапуска): English, Deutsch, Français, 中文, 日本語.

## Как открыть в Android Studio

1. **Clone Repository**
2. Вставь: `https://github.com/Art010102/dungeons-in-the-night.git`
3. **не жми New Project**
4. Trust / SDK 36 — согласись, дождись Sync
5. Подключи телефон, **Run**

Если репо уже клонировано: **Git → Pull**, потом Run.

Собрать APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**

Package: `com.dungeonsnight.game`  
compileSdk / targetSdk: **36**
