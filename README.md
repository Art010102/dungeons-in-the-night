# Dungeons in the Night

Нативная Android-игра (Kotlin + Canvas). Интернет не нужен. Все спрайты, земля, музыка и уровни лежат внутри APK.

## Store-картинки (скачай из корня репо)

- [`icon-512.png`](icon-512.png) — иконка 512×512
- [`banner-1024x500.png`](banner-1024x500.png) — баннер 1024×500 (feature graphic)

## Что это

Тот же рыцарь, слаймы, летучие мыши и **большой дракон** (в 2.5 раза крупнее). Три зала:

1. **Ember Halls** — исходный уровень
2. **Forked Dark** — развилка вверх/вниз
3. **Night's Crown** — вертикальный лабиринт

Следующий уровень открывается только после прохождения предыдущего. Пол — земля с травой до самого низа экрана.

## Как открыть в Android Studio

1. **Clone Repository**
2. Вставь: `https://github.com/Art010102/dungeons-in-the-night.git`
3. **не жми New Project**
4. Trust / SDK 36 — согласись, дождись Sync
5. Подключи телефон, **Run**

Если репо уже клонировано: **Git → Pull**, потом Run. Не открывай старую папку — она может быть без дракона.

Собрать APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**

Package: `com.dungeonsnight.game`  
compileSdk / targetSdk: **36**
