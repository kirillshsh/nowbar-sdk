# Samsung Voice Recorder Now Bar extract

This note records local APK evidence for the Samsung native ongoing-activity style
and cover capsule behavior. The SDK only records names, method shapes, and extra
keys needed for interoperability; it does not copy Samsung implementation code.

## Artifact

- Download tool: `apkeep 0.18.0`
- APK command:

```bash
apkeep -a com.sec.android.app.voicenote -d apk-pure artifacts/external-apks
OUT_DIR=artifacts/local-apk-nowbar-scan/current-samsung-clock-voice \
  scripts/scan_local_apks_for_nowbar.sh \
  artifacts/external-apks/com.sec.android.app.clockpackage.apk \
  artifacts/external-apks/com.sec.android.app.voicenote.apk
```

- Scan output: `artifacts/local-apk-nowbar-scan/current-samsung-clock-voice/`
- Package: `com.sec.android.app.voicenote`
- Version: `21.5.42.06`
- Version code: `2021542060`
- Target SDK: `34`
- Debuggable: `false`

## Manifest signals

The APK declares normal notification and foreground-service permissions, plus Samsung
surfaces that explain why Voice Recorder can render richer system UI:

- `android.permission.POST_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MICROPHONE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `com.samsung.android.app.aodservice.permission.SERVICEBOX_REMOTEVIEWS`
- `com.samsung.systemui.permission.FACE_WIDGET`

## Hidden native style

`com.sec.android.app.voicenote.ui.remote.OngoingActivityStyleRef` reflects:

```text
android.app.Notification$OngoingActivityStyle
```

Confirmed methods from that wrapper:

| Samsung method | SDK mapping |
| --- | --- |
| `setChipIcon(Icon)` | `SamsungOngoingActivityStyleBuilder.chipIcon(...)` |
| `setChipBackground(Int)` | `SamsungOngoingActivityStyleBuilder.chipBackgroundColor(...)` |
| `setCardIcon(Icon)` | `SamsungOngoingActivityStyleBuilder.cardIcon(...)` |
| `setBadge(Icon)` | `SamsungOngoingActivityStyleBuilder.badgeIcon(...)` |
| `setCardBackground(Int)` | `SamsungOngoingActivityStyleBuilder.cardBackgroundColor(...)` |
| `setPrimaryInfo(CharSequence)` | `SamsungOngoingActivityStyleBuilder.primaryInfo(...)` |
| `setSecondaryInfo(CharSequence)` | `SamsungOngoingActivityStyleBuilder.secondaryInfo(...)` |
| `setMoreInfo(CharSequence)` | `SamsungOngoingActivityStyleBuilder.moreInfo(...)` |
| `setExpandedChipView(RemoteViews)` | `SamsungOngoingActivityStyleBuilder.expandedChipView(...)` |
| `setCustomCardViewCenterUI(RemoteViews)` | `SamsungOngoingActivityStyleBuilder.customCardViewCenterUi(...)` |
| `setCustomExpandedCardView(RemoteViews)` | `SamsungOngoingActivityStyleBuilder.customExpandedCardView(...)` |
| `addAction(Notification.Action)` | `SamsungOngoingActivityStyleBuilder.action(...)` |

Voice Recorder creates the style instance, applies chip/card colors and icons, adds
actions, then passes it to `Notification.Builder.setStyle(...)`. The SDK exposes the
same shape as an optional reflection builder. It returns `null` when the hidden Samsung
class is absent and ignores missing methods on older or newer One UI builds.

## Capsule keys

`NotiRemoteViewManager` confirms the cover/capsule bundle keys already exposed through
`CapsuleConfig` and `OngoingExtrasBuilder`:

| Key | SDK mapping |
| --- | --- |
| `isCapsule` | `CapsuleConfig` |
| `capsule_layout` | `CapsuleConfig.layout` |
| `capsule_action` | `CapsuleConfig.action` |
| `bg_startColor` | `CapsuleConfig.bgStartColor` |
| `bg_endColor` | `CapsuleConfig.bgEndColor` |
| `capsule_priority` | `CapsuleConfig.priority` |

Voice Recorder uses `capsule_priority=normal` for active playback/recording and
`capsule_priority=low` for paused states.

## API consequences

- `SamsungOngoingActivityStyleBuilder` is a best-effort Samsung-only API for apps that
  want to build directly on `Notification.Builder` when One UI exposes the hidden style.
  `buildWithReport()` returns the style plus an applied/missing/failed method report for
  real-device drift checks across One UI versions.
- The default SDK manager still uses public Android notifications plus Samsung extras,
  because that path stays portable and testable on non-Samsung devices.
- Real rendering behavior still requires a Samsung device. Static APK evidence can prove
  method names and keys, not package allow-listing or SystemUI/AOD ranking behavior.
