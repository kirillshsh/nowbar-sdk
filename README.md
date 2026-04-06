# NowBar SDK

> Android library module for Samsung Now Bar and Android 16 Live Updates.

<p align="center">
  <a href="#english">🇬🇧 English</a>
  ·
  <a href="#russian">🇷🇺 Русский</a>
</p>

---

<a id="english"></a>

## 🇬🇧 English

`nowbar-sdk` is a portable Android library module built around the `com.nowbar.api` package. It gives you one API for creating, updating, dismissing, and stopping ongoing Now Bar / Live Update style surfaces.

### Highlights

| Capability | Details |
| --- | --- |
| Native surfaces | Samsung Now Bar and Android 16 Live Updates |
| Fallback | Standard ongoing notification via `FallbackStrategy` |
| Integration style | Copy the [`nowbar/`](./nowbar) module into your project |
| Entry points | Direct manager API, session API, foreground service helper |

### Repository Layout

- [`nowbar/`](./nowbar) - Android library module
- [`examples/`](./examples) - ready-to-copy usage examples
- [`examples/TimerSessionExample.kt`](./examples/TimerSessionExample.kt) - session API example
- [`examples/TimerNowBarService.kt`](./examples/TimerNowBarService.kt) - foreground service example
- [`examples/WorkoutNowBarService.kt`](./examples/WorkoutNowBarService.kt) - workout example
- [`examples/AndroidManifest.snippet.xml`](./examples/AndroidManifest.snippet.xml) - manifest snippet

### Core API

| API | Purpose |
| --- | --- |
| [`NowBarManager`](./nowbar/src/main/kotlin/com/nowbar/api/NowBarManager.kt) | Feature detection, channel creation, build/post/cancel |
| [`NowBarConfig`](./nowbar/src/main/kotlin/com/nowbar/api/NowBarConfig.kt) | Notification channel, notification id, fallback, Samsung surface options |
| [`NowBarSession`](./nowbar/src/main/kotlin/com/nowbar/api/NowBarSession.kt) | `start()`, `update()`, `dismiss()`, `stop()` |
| [`NowBarForegroundService`](./nowbar/src/main/kotlin/com/nowbar/api/service/NowBarForegroundService.kt) | Helper base class for long-running foreground-service flows |
| [`NowBarCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/NowBarCard.kt) | Base model for card content |

### Card Models

[`TimerCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/TimerCard.kt),
[`WorkoutCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/WorkoutCard.kt),
[`MediaCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/MediaCard.kt),
[`CallCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/CallCard.kt),
[`NavigationCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/NavigationCard.kt),
[`CustomCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/CustomCard.kt)

### Platform Support

| Platform | Behavior |
| --- | --- |
| Samsung devices with Now Bar support | Native Samsung ongoing-activity extras path |
| Android 16+ | Native Live Updates / promoted ongoing notification path |
| Unsupported devices | Plain ongoing notification for `AUTO` / `STANDARD_NOTIFICATION`, no SDK-managed posting for `NONE` |

Use `NowBarManager.isSupported(context)` to check whether a native enhanced surface is available, and `NowBarManager.getSupportedPlatform(context)` to inspect the detected platform.

### Requirements

- `minSdk = 26`
- `compileSdk = 36` or newer
- Java / Kotlin target `17`

### Integration

1. Copy the [`nowbar/`](./nowbar) folder into your Android project.
2. Register the module in your root `settings.gradle.kts`:

```kotlin
include(":nowbar")
```

3. Add the dependency in your app module:

```kotlin
dependencies {
    implementation(project(":nowbar"))
}
```

4. Merge the required manifest entries from [`nowbar/src/main/AndroidManifest.xml`](./nowbar/src/main/AndroidManifest.xml) or [`examples/AndroidManifest.snippet.xml`](./examples/AndroidManifest.snippet.xml):

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<application>
    <meta-data
        android:name="com.samsung.android.support.ongoing_activity"
        android:value="true" />
</application>
```

5. Request `POST_NOTIFICATIONS` at runtime on Android 13+.

`FOREGROUND_SERVICE_*` permissions depend on your service type. For example, the timer service uses `specialUse`, while the workout example uses `health|location`, so [`examples/AndroidManifest.snippet.xml`](./examples/AndroidManifest.snippet.xml) also includes `FOREGROUND_SERVICE_HEALTH`, `FOREGROUND_SERVICE_LOCATION`, and `android:foregroundServiceType="health|location"` for that service.

### Quick Start

```kotlin
val config = NowBarConfig(
    channelId = "timer",
    channelName = "Timer"
)

NowBarManager.createNotificationChannel(context, config)

val session = NowBarManager.createSession(context, config)

session.start(
    TimerCard(
        title = "Tea timer",
        icon = IconCompat.createWithResource(context, R.drawable.ic_timer),
        totalDuration = 5.minutes,
        remainingDuration = 5.minutes,
        isCountDown = true,
        accentColor = 0xFFFF9800.toInt()
    )
)

// update later
session.update(
    TimerCard(
        title = "Tea timer",
        icon = IconCompat.createWithResource(context, R.drawable.ic_timer),
        totalDuration = 5.minutes,
        remainingDuration = 3.minutes,
        isCountDown = true,
        accentColor = 0xFFFF9800.toInt()
    )
)

// keep the notification, hide promoted / Samsung surface
session.dismiss()

// cancel everything
session.stop()
```

### Notes

- If you do not need session state, you can build or post notifications directly through [`NowBarManager`](./nowbar/src/main/kotlin/com/nowbar/api/NowBarManager.kt).
- For long-running flows, use [`NowBarForegroundService`](./nowbar/src/main/kotlin/com/nowbar/api/service/NowBarForegroundService.kt).
- SDK-managed `notify` / `session` posting is skipped when the app cannot post notifications, for example when `POST_NOTIFICATIONS` is denied on Android 13+.
- `FallbackStrategy.AUTO` uses Samsung / Android 16 enhancements when available, otherwise keeps a plain ongoing notification.
- `FallbackStrategy.STANDARD_NOTIFICATION` always keeps a plain ongoing notification and never requests native enhancements.
- `FallbackStrategy.NONE` posts only when a native Samsung / Android 16 surface is available.
- `NowBarForegroundService` still has to keep a foreground notification because Android requires it, so `FallbackStrategy` there affects rendering, not the fact that `startForeground()` is used.

---

<a id="russian"></a>

## 🇷🇺 Русский

`nowbar-sdk` — это Android-библиотека вокруг пакета `com.nowbar.api`. Она даёт единый API для создания, обновления, скрытия и остановки карточек и ongoing-уведомлений в стиле Samsung Now Bar и Android 16 Live Updates.

### Что внутри

| Возможность | Что это значит |
| --- | --- |
| Нативные поверхности | Samsung Now Bar и Android 16 Live Updates |
| Фоллбек | Обычное ongoing-уведомление через `FallbackStrategy` |
| Формат интеграции | Папка [`nowbar/`](./nowbar) копируется прямо в ваш проект |
| Точки входа | Прямой manager API, session API и helper для foreground service |

### Структура репозитория

- [`nowbar/`](./nowbar) - Android-библиотека
- [`examples/`](./examples) - готовые примеры интеграции
- [`examples/TimerSessionExample.kt`](./examples/TimerSessionExample.kt) - пример через session API
- [`examples/TimerNowBarService.kt`](./examples/TimerNowBarService.kt) - пример через foreground service
- [`examples/WorkoutNowBarService.kt`](./examples/WorkoutNowBarService.kt) - пример для workout-сценария
- [`examples/AndroidManifest.snippet.xml`](./examples/AndroidManifest.snippet.xml) - фрагмент manifest

### Основное API

| API | Для чего нужно |
| --- | --- |
| [`NowBarManager`](./nowbar/src/main/kotlin/com/nowbar/api/NowBarManager.kt) | Определение поддержки, создание channel, build/post/cancel уведомлений |
| [`NowBarConfig`](./nowbar/src/main/kotlin/com/nowbar/api/NowBarConfig.kt) | Channel, notification id, fallback и параметры Samsung surface |
| [`NowBarSession`](./nowbar/src/main/kotlin/com/nowbar/api/NowBarSession.kt) | `start()`, `update()`, `dismiss()`, `stop()` |
| [`NowBarForegroundService`](./nowbar/src/main/kotlin/com/nowbar/api/service/NowBarForegroundService.kt) | Базовый helper для долгоживущих foreground service сценариев |
| [`NowBarCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/NowBarCard.kt) | Базовая модель карточки |

### Доступные карточки

[`TimerCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/TimerCard.kt),
[`WorkoutCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/WorkoutCard.kt),
[`MediaCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/MediaCard.kt),
[`CallCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/CallCard.kt),
[`NavigationCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/NavigationCard.kt),
[`CustomCard`](./nowbar/src/main/kotlin/com/nowbar/api/cards/CustomCard.kt)

### Поддержка платформ

| Платформа | Поведение |
| --- | --- |
| Samsung-устройства с Now Bar | Нативный путь через Samsung ongoing-activity extras |
| Android 16+ | Нативный путь через Live Updates / promoted ongoing notification |
| Остальные устройства | Обычное ongoing-уведомление для `AUTO` / `STANDARD_NOTIFICATION`, без SDK-posting для `NONE` |

Проверить наличие нативной поверхности можно через `NowBarManager.isSupported(context)`, а посмотреть определённую платформу — через `NowBarManager.getSupportedPlatform(context)`.

### Требования

- `minSdk = 26`
- `compileSdk = 36` или новее
- Java / Kotlin target `17`

### Подключение

1. Скопируйте папку [`nowbar/`](./nowbar) в свой Android-проект.
2. Добавьте модуль в корневой `settings.gradle.kts`:

```kotlin
include(":nowbar")
```

3. Подключите модуль в app-модуле:

```kotlin
dependencies {
    implementation(project(":nowbar"))
}
```

4. Добавьте нужные записи в manifest, взяв их из [`nowbar/src/main/AndroidManifest.xml`](./nowbar/src/main/AndroidManifest.xml) или [`examples/AndroidManifest.snippet.xml`](./examples/AndroidManifest.snippet.xml):

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<application>
    <meta-data
        android:name="com.samsung.android.support.ongoing_activity"
        android:value="true" />
</application>
```

5. На Android 13+ запросите `POST_NOTIFICATIONS` как runtime permission.

`FOREGROUND_SERVICE_*` permissions зависят от типа сервиса. Например, timer-сервис использует `specialUse`, а workout-пример использует `health|location`, поэтому в [`examples/AndroidManifest.snippet.xml`](./examples/AndroidManifest.snippet.xml) дополнительно есть `FOREGROUND_SERVICE_HEALTH`, `FOREGROUND_SERVICE_LOCATION` и `android:foregroundServiceType="health|location"` для этого сервиса.

### Быстрый старт

```kotlin
val config = NowBarConfig(
    channelId = "timer",
    channelName = "Timer"
)

NowBarManager.createNotificationChannel(context, config)

val session = NowBarManager.createSession(context, config)

session.start(
    TimerCard(
        title = "Таймер чая",
        icon = IconCompat.createWithResource(context, R.drawable.ic_timer),
        totalDuration = 5.minutes,
        remainingDuration = 5.minutes,
        isCountDown = true,
        accentColor = 0xFFFF9800.toInt()
    )
)

// потом обновляем
session.update(
    TimerCard(
        title = "Таймер чая",
        icon = IconCompat.createWithResource(context, R.drawable.ic_timer),
        totalDuration = 5.minutes,
        remainingDuration = 3.minutes,
        isCountDown = true,
        accentColor = 0xFFFF9800.toInt()
    )
)

// оставляем уведомление, но скрываем promoted / Samsung surface
session.dismiss()

// полностью останавливаем
session.stop()
```

### Примечания

- Если session API не нужен, можно собирать и отправлять уведомления напрямую через [`NowBarManager`](./nowbar/src/main/kotlin/com/nowbar/api/NowBarManager.kt).
- Для долгоживущих сценариев есть [`NowBarForegroundService`](./nowbar/src/main/kotlin/com/nowbar/api/service/NowBarForegroundService.kt).
- SDK-прослойка для `notify` / `session` не постит уведомление, если приложению сейчас нельзя их показывать, например когда на Android 13+ не выдан `POST_NOTIFICATIONS`.
- `FallbackStrategy.AUTO` включает Samsung / Android 16 enhancements, когда они доступны, и оставляет обычное ongoing-уведомление в остальных случаях.
- `FallbackStrategy.STANDARD_NOTIFICATION` всегда оставляет обычное ongoing-уведомление и не запрашивает нативные enhancements.
- `FallbackStrategy.NONE` постит только при наличии нативной Samsung / Android 16 поверхности.
- `NowBarForegroundService` всё равно обязан держать foreground-уведомление, потому что этого требует Android, поэтому там `FallbackStrategy` влияет на рендеринг, а не отменяет сам `startForeground()`.
