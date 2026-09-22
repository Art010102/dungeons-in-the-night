# Dungeons in the Night

Нативная Android-игра (Kotlin + Canvas). Интернет не нужен. Все спрайты, земля, музыка и уровни лежат внутри APK.

Стек: **AGP 9.4.1 · Gradle 9.7.1 · Kotlin 2.4.10 · compileSdk 36**

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
4. Trust / SDK 36 — согласись, дождись Sync (Gradle 9.7.1 скачается сам)
5. Подключи телефон, **Run**

Если репо уже клонировано: **Git → Pull**, потом **File → Sync Project with Gradle Files**, потом Run.

## Как собрать APK, который ставится на телефон

В верхней полоске Mac: **Build → Build Bundle(s) / APK(s) → Build APK(s)**

Когда всплывёт **APK(s) generated successfully** — жми **locate**.

Нужный файл:

`app/build/outputs/apk/debug/app-debug.apk`

Не бери `app-release-unsigned.apk` — телефон напишет «пакет повреждён». Debug-apk уже подписан и ставится.

Если игра уже стояла со старого билда — сначала удали её с телефона, потом ставь новый apk.

Package: `com.dungeonsnight.game`  
compileSdk / targetSdk: **36**
