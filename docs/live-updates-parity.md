# Android Live Updates parity

This file tracks public Android Live Updates requirements and how the SDK maps them.

## Public Android requirements

Source:

- https://developer.android.com/develop/ui/views/notifications/live-update
- https://developer.android.com/develop/ui/views/notifications/metric-style
- https://github.com/android/platform-samples/tree/main/samples/user-interface/live-updates
- https://forum.developer.samsung.com/t/now-bar/38681
- https://www.samsung.com/us/support/answer/ANS10004605/

As of the 2026-05-18 check, Samsung's public developer site does not expose a
dedicated third-party Now Bar SDK. The public Samsung forum thread above records
that no public Now Bar SDK was available and points third-party apps toward Android
Live Updates on newer One UI builds. The SDK therefore keeps the normal path aligned
with public Android Live Updates and exposes Samsung extras / hidden-style builders
as best-effort compatibility layers backed by APK and `dumpsys` evidence.
Samsung's public support article documents Now Bar as a live-notifications surface
for sports, Maps, active media, timers, and related categories, but it does not
publish a developer API contract.

| Requirement | SDK coverage |
| --- | --- |
| `POST_PROMOTED_NOTIFICATIONS` manifest permission | `nowbar/src/main/AndroidManifest.xml`, `demo/src/main/AndroidManifest.xml`, `examples/AndroidManifest.snippet.xml` |
| `NotificationCompat.Builder#setRequestPromotedOngoing(true)` | `LiveUpdateBuilder.apply(...)` when `NowBarConfig.requestPromotedOngoing` is true |
| Ongoing notification | `NowBarNotificationBuilder` and fallback adapter call `setOngoing(true)` |
| Content title | All card types provide `toPrimaryInfo()`; builder always sets title |
| Header subtext/context | `NowBarCard.toSubText()` maps to `NotificationCompat.Builder#setSubText(...)`; delivery cards use the merchant name and metric cards use `secondaryText` |
| No custom content view for Live Updates | Normal SDK path does not use `RemoteViews`; Samsung dump-parity helpers are separate and OEM-specific |
| Not a group summary | Normal SDK path does not set group summary; dump-parity helper is separate |
| Not colorized | Normal SDK path sets color but does not call `setColorized(true)` |
| Channel importance above `IMPORTANCE_MIN` | `NowBarConfig.channelImportance` defaults to `IMPORTANCE_LOW`; diagnostics reports channel blockers |
| BigTextStyle for long status details | `CustomCard.bigText(...)` maps to AndroidX `NotificationCompat.BigTextStyle` |
| CallStyle for call Live Updates | `CallCard` uses AndroidX `NotificationCompat.CallStyle` for incoming, ongoing, and screening calls when the required intents are present |
| ProgressStyle segments/points/tracker icons | `ProgressStyleAdapter` and `LiveUpdateBuilder` |
| App-defined custom ProgressStyle journeys | `CustomCard` exposes custom segments, milestone points, tracker/start/end icons, and `styledByProgress`; explicit custom segments and tracker icons are mirrored to Samsung progress-segment extras |
| Android 17 MetricStyle metrics | `MetricCard` + `LiveUpdateMetricStyle`; `PlatformMetricNotificationBuilder` applies the platform template reflectively on API 37+ and maps `secondaryText` to `setSubText(...)` for the Live Update header/context line |
| Maximum 3 action buttons | `NowBarActionLimits.MAX_ACTIONS`; notification builders keep the first three `ActionConfig` entries |
| Samsung extras path | Now Bar feature flag or Samsung manufacturer/brand detection; ignored extras remain harmless on older One UI builds; `NowBarConfig.aodRemoteApp` mirrors the AOD identity keys seen in Samsung dumps |
| Status chip text / chronometer | `toShortCriticalText()` maps to `Notification.Builder#setShortCriticalText`; `TimerCard` maps countdown and stopwatch state to `setWhen(...)` chronometer chips; delivery, custom, and metric cards can expose `toChipWhenTimeMillis()` for countdown chips |
| Tap handling | `NowBarCard.tapAction` maps to `setContentIntent(...)`; diagnostics and evidence reports expose whether the built notification carries one |
| Dismiss handling | `NowBarCard.deleteIntent` maps to `setDeleteIntent(...)`; diagnostics and evidence reports expose whether the built notification carries one |
| Unpin/demote handling | `ActionSemantic.UNPIN` marks actions that should stop showing the enhanced live surface while the underlying event continues; `NowBarSession.unpin()` and the demo action rebuild the current card with `STANDARD_NOTIFICATION` and `requestPromotedOngoing = false` |
| Text-only / disabled actions | `ActionConfig.textOnly(...)`, `ActionConfig.disabled(...)`, and `ActionConfig.NO_ICON` mirror Android's official sample pattern of action buttons without icons or tap intents |
| Action metadata | `NowBarActionExtras` stores `ActionConfig.id` and `ActionConfig.semantic` into `Notification.Action.extras`, and diagnostics/evidence reports expose the ids and semantics after notification build |
| Rich visual identity | `NowBarCard.largeIcon` maps to `setLargeIcon(...)`; media album art remains supported |
| Android 17 semantic title annotations | `LiveUpdateSemanticStyle` and `LiveUpdateTextStyler` use the platform API via reflection when present |
| Runtime eligibility reporting | `LiveUpdateDiagnostics.inspect(...)` reports platform support, manifest permission, settings availability, request state, user setting, promotable characteristics, Android progress value/max/indeterminate state, ProgressStyle segments/points/icons, styled-by-progress flag, content intent, delete intent, action count/titles/ids/semantics/text-only/disabled state, subtext, status chip text/time/chronometer, compact-chip text fit guidance, too-soon/past-when/expired countdown advisories, missing delete-intent and over-limit action advisories, group/custom/color/channel blockers |
| Device capability reporting | `NowBarDiagnostics.inspect(...)` reports Samsung manufacturer fallback, Now Bar feature flag, hidden Samsung style availability, Android 16 Live Updates support, Now Bar / app / promoted settings shortcuts, and ambient feature flags |
| Built notification evidence reporting | `NowBarNotificationEvidence.inspect(...)` classifies Samsung extras, exposes structured first-class Samsung Now Bar text/visual/action/progress/chronometer/capsule state, dump extras, native template, promoted ongoing, content title, content/delete intents, action count/titles/ids/semantics/text-only/disabled state, custom-view/colorized blockers, Android progress value/max/indeterminate state, ProgressStyle segments/points/icons, styled-by-progress flag, subtext, status chip text/time/chronometer, BigTextStyle/CallStyle/ProgressStyle/MetricStyle templates, structured RemoteViews, AOD remote-app name/icon/pending intent, Samsung dump text/visual/chronometer state, capsule, and missing core hints |
| One-shot readiness preflight | `NowBarReadiness.inspect(...)` / `NowBarManager.inspectReadiness(...)` builds the SDK notification locally and combines device capability, Live Updates eligibility, notification evidence, fallback state, action truncation advisories, and blocking reasons without posting |
| Settings shortcuts | `NowBarDiagnostics.createNowBarSettingsIntent(...)` opens the documented Samsung Settings area for Now bar app toggles; `LiveUpdateDiagnostics.createManageAppPromotedNotificationsIntent(...)` mirrors the current Android docs name and delegates to the platform `android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS` action |
| Samsung hidden OngoingActivityStyle | `SamsungOngoingActivityStyleBuilder` can create `android.app.Notification$OngoingActivityStyle` reflectively when the runtime exposes it and `buildWithReport()` records applied, missing, and failed hidden methods |

`MetricCard` intentionally limits metric count to the public Android maximum of three.
`CallCard` keeps plain action-button fallback when the card does not provide the
system intents required by `CallStyle`. When the intents are present, incoming calls use
`forIncomingCall(...)`, active calls use `forOngoingCall(...)`, and screened calls use
`forScreeningCall(...)`, which keeps the notification inside the public Live Updates
allowed-style set.
`NowBarNotificationEvidence` also reports AndroidX CallStyle extras such as
`android.callType`, `android.callIsVideo`, `android.callPerson`, answer/decline/hangup
intents, answer/decline color hints, `android.verificationText`, and
`android.verificationIcon`.
For Samsung dump-style cards, the same report keeps the concrete AOD remote-app name,
icon, and pending intent,
individual `nowbarRemoteView` / `expandedRemoteView` / chip/custom RemoteViews flags plus a structured `samsungViews` object,
a total Samsung RemoteViews count, `samsungDumpShow`, `samsungReducedImages`,
primary action, expandable type, visual icon flags, action-background color count,
optional Samsung dump text/visual/chronometer state, and optional PDE telemetry state so captured `dumpsys` records can be checked
against the Google Sports / Finance topology.
`CustomCard.bigText(...)` covers the public Android `BigTextStyle` allowed style
for status details that need more room than `contentText` while still avoiding
custom `RemoteViews`.

The builders also cap notification actions at three buttons, matching Live Updates /
MetricStyle display constraints. `NowBarCard.toSubText()` is the generic header/context
hook for the public Live Updates anatomy: delivery cards write the merchant name, and
metric cards write `secondaryText`. On Android 17+, metric values are built with platform
classes while preserving the same subtext header. On lower SDKs the same card remains a
standard ongoing notification because the platform template does not exist.

The official Android Live Updates sample uses `setShortCriticalText(...)` for status
chips such as delivery state or a compact ETA. `CustomCard`, `DeliveryCard`, and
`MetricCard` expose `shortCriticalText(...)`; other cards keep `chipText(...)` as the
default chip text source. `LiveUpdateDiagnostics` and `NowBarNotificationEvidence`
also report the `android.shortCriticalText` extra when it is present.
`TimerCard` now exposes the public status-chip chronometer contract directly: countdown
timers use a future `when` timestamp with countdown enabled, while stopwatch-style
timers use an elapsed base timestamp with countdown disabled. Text and chronometer
status-chip mapping is also applied on the standard notification fallback path, so
demoted or non-promoted cards keep compact status context instead of becoming static
plain notifications.
`LiveUpdateDiagnostics` also exposes status-chip advisories instead of treating them as
eligibility blockers: long short-critical text gets `status-chip-text-may-truncate`,
`when` values less than two minutes in the future get `status-chip-when-too-soon`,
non-chronometer past `when` gets `status-chip-when-in-past`, and expired countdown
chronometers get `status-chip-countdown-expired`. `NowBarReadiness` prefixes those as
`live-update:*` advisories so preflight output can separate hard blockers from UX risks.

For user-initiated monitoring flows that offer an Unpin action, the SDK keeps this
as an action semantic plus an explicit `NowBarSession.unpin()` operation instead of a
magic auto-dismiss policy. The session API and demo Unpin button repost the same current
card through `FallbackStrategy.STANDARD_NOTIFICATION` with `requestPromotedOngoing = false`,
so Android/Samsung enhanced-surface hints disappear while the foreground-service
notification remains visible.

Samsung extras also expose separate `android.ongoingActivityNoti.progressSegments.*`
keys for `progressColor`, `segmentColor`, `segmentStart`, and `icon`. Samsung Health
6.32.0.001 confirms this shape in `SportOngoingNotificationHelper`; see
`docs/samsung-health-nowbar-extract.md`. The SDK writes the tracker icon automatically
for workout, navigation, and delivery cards, mirrors delivery's four-step journey to
Samsung segment starts, and exposes `OngoingExtrasBuilder` plus `ProgressSegment` for
custom dump-parity payloads.
For `CustomCard`, custom AndroidX `ProgressStyle` segment lengths define the effective
progress maximum, matching Android's `ProgressStyle` contract. The same explicit custom
segments are converted to Samsung `progressSegments.segmentStart` positions by cumulative
length, and `progressTrackerIcon(...)` is also written as Samsung's progress segment icon.
`LiveUpdateDiagnostics` and `NowBarNotificationEvidence` report the AndroidX compat
extras too: `android.progressSegments`, `android.progressPoints`,
`android.progressTrackerIcon`, `android.progressStartIcon`, `android.progressEndIcon`,
and `android.styledByProgress`.

`LiveUpdateDiagnostics` also mirrors the Android action-button evidence exposed by
`NowBarNotificationEvidence`, including title lists, text-only actions, disabled
actions, and over-limit action advisories. The same report surfaces a
`missing-delete-intent` advisory so callers can catch dismissals with `setDeleteIntent`
before deciding whether to repost a Live Update.
`NowBarReadiness` additionally compares the original card action count with the built
notification action count and emits `notification:action-buttons-truncated` when SDK
limits trimmed the posted notification to the Android Live Updates maximum.

Samsung Voice Recorder 21.5.42.06 also confirms a native hidden style path:
`android.app.Notification$OngoingActivityStyle`. That path is exposed separately through
`SamsungOngoingActivityStyleBuilder`; see `docs/samsung-voice-recorder-nowbar-extract.md`.
It is intentionally not the default manager path because it depends on OEM runtime
classes and cannot be proven without a Samsung build that ships the class.

Extra APK sweeps across Samsung Music, SmartThings, Samsung Find, Samsung Notes,
Samsung Messages, and Samsung Dialer did not surface new Now Bar keys or
promoted-notification APIs; see `docs/samsung-extra-apk-scan.md`.

## Compile SDK note

The project currently compiles against Android 36 and uses reflection for Android 17-only semantic title APIs:

- `Notification.SEMANTIC_STYLE_*`
- `Notification.createSemanticStyleAnnotation(...)`
- `Notification.isRequestPromotedOngoing()`
- `Notification.Builder#setShortCriticalText(...)`
- `Notification.MetricStyle` / `Notification.Metric`

This keeps the SDK consumable without requiring the Android 37.0 platform package while still using the new APIs on devices where they exist.

## Real-device status

Local build/unit/lint gates can prove API shape and notification construction. Local
Samsung Health and Voice Recorder APK extracts can prove observed extras, hidden style
method names, and manifest signals. They cannot prove Samsung Now Bar rendering, package
allow-list behavior, One UI settings behavior, or OEM ranking behavior. Those still
require `scripts/real_samsung_smoke.sh` on a connected Samsung device and
`scripts/pull_decompile_samsung_apks.sh` for device APK evidence.
Samsung's public setup path for Now bar app toggles is Settings -> Lock screen and AOD
-> Now bar -> View more; `NowBarDiagnostics.resolveNowBarSettingsIntent(...)` is only
a best-effort shortcut to that settings area because Samsung does not publish a per-app
Now bar settings intent.
