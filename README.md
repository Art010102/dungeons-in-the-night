# Dungeons in the Night

Нативная Android-игра (Kotlin + Canvas). Интернет не нужен. Все спрайты, земля, музыка и уровни лежат внутри APK.

## Что это

Тот же рыцарь, слаймы, летучие мыши и красный флаг. Три зала:

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

Собрать APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**

Package: `com.dungeonsnight.game`  
compileSdk / targetSdk: **36**
