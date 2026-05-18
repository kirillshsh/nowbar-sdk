# Real Samsung device testing

This repository includes an installable `:demo` app for manual Now Bar and Live Update validation.

## Build

```bash
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
ANDROID_SDK_ROOT=/opt/homebrew/share/android-commandlinetools \
./gradlew test build :nowbar:connectedDebugAndroidTest
```

Debug APK:

```text
demo/build/outputs/apk/debug/demo-debug.apk
```

## Device checklist

```bash
adb devices -l
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell pm list features | grep -i 'samsung.*nowbar\|nowbar'
```

## Samsung Remote Test Lab

Samsung's Remote Test Lab is an official way to test on a real Galaxy device through
a web browser: https://developer.samsung.com/remote-test-lab
Samsung's Remote Debug Bridge flow lets that real remote phone appear in local ADB:
https://developer.samsung.com/sdp/blog/en/2021/06/04/using-remote-test-lab-with-android-studio

Use it when no local USB Samsung device is available:

1. Sign in to Samsung Developer and start a phone running One UI 7/8.
2. In the Remote Test Lab client, right-click the device and open
   `Test > Remote Debug Bridge`.
3. Run the displayed command, usually shaped like `adb connect localhost:<port>`,
   and accept the RSA debugging prompt on the remote device.
4. Run `adb devices -l` locally. The smoke scripts can also run the connection step
   for you with `ADB_CONNECT=localhost:<port>`.
5. Install `demo/build/outputs/apk/debug/demo-debug.apk` through ADB or the Remote Test Lab client.
6. Grant notification permission in the device UI.
7. Open the demo app and run every button: timer, workout, navigation, delivery, metrics,
   media, call, custom actions, Samsung dump style, and Samsung native style.
8. Capture lock-screen/AOD/notification screenshots for each scenario.

If the Remote Test Lab session exposes ADB locally, run the automated smoke directly:

```bash
ADB_CONNECT=localhost:<port> WAIT_FOR_DEVICE_SECONDS=30 scripts/real_samsung_smoke.sh
ADB_CONNECT=localhost:<port> WAIT_FOR_DEVICE_SECONDS=30 scripts/run_real_samsung_e2e.sh
```

Set `SERIAL=<serial>` only when more than one device is connected. Otherwise use the
manual button flow above.

Install and grant notifications:

```bash
adb install -r -d -g demo/build/outputs/apk/debug/demo-debug.apk
adb shell pm grant com.nowbar.demo android.permission.POST_NOTIFICATIONS
adb shell monkey -p com.nowbar.demo 1
```

`POST_PROMOTED_NOTIFICATIONS` is a non-runtime manifest permission. The demo declares it,
and `LiveUpdateDiagnostics.declaresPostPromotedNotifications` verifies that the installed
package exposes it before interpreting promoted ongoing failures.
`LiveUpdateDiagnostics.canOpenPromotionSettings` also verifies whether the device exposes
the official promoted-notification settings activity before the demo opens it.

Run the complete automated smoke:

```bash
scripts/real_samsung_smoke.sh
```

The script installs `demo-debug.apk`, grants `POST_NOTIFICATIONS`, runs every demo action,
executes `:demo:connectedDebugAndroidTest`, captures demo APK manifest metadata,
installed package state, appops, full device feature list, full `getprop`, One UI
build properties, notification/AOD/ambient-related settings, `cmd notification --help`,
ADB connect/device/mDNS preflight files, Samsung package inventory,
`dumpsys notification --noredact`, wide per-action package
slices, filtered Now Bar logs, `NowBarDiagnostics` capability reports, app screenshots,
notification shade screenshots, and device properties under
`artifacts/real-samsung-smoke/<timestamp>/`.

Every action writes a row to `action-summary.tsv` and the same table is embedded in
`summary.md`. The counters show whether the posted notification still contains the
demo package, Android promoted ongoing hints, Samsung `android.ongoingActivityNoti.*`
extras, Android/Samsung progress references, metric/native-style references,
hidden native-style build report refs, BigTextStyle/CallStyle references,
Android action button refs, text-only/disabled action refs, action id/semantic metadata refs, status-chip time/text
references, MetricStyle subtext/header references, AOD remote-app identity, Samsung
RemoteViews topology, Samsung dump chronometer refs, Unpin action refs, and capsule keys. The smoke
also invokes the standalone `UNPIN` action after the custom controls flow, so the same
device run proves the notification remains posted after demotion and no longer carries
Android promoted refs, Samsung ongoing-activity extras, or Samsung native-style refs. That makes it possible to
distinguish "button launched" from "button posted a Now Bar-compatible notification"
before doing the visual notification shade, lock-screen, and AOD review.

The smoke also runs `scripts/verify_real_samsung_smoke_artifacts.sh` and stores its
output in `artifact-verification.txt`. The verifier requires every expected demo action
row, clean `am start` output, captured app and notification shade screenshots,
package-owned `dumpsys` evidence for active actions, Samsung or Android enhanced-surface
hints for normal card actions, Samsung dump/native evidence for `SHOW_DUMP`, and
promoted/native evidence for `SHOW_NATIVE_STYLE`. It also requires explicit BigTextStyle
refs for `SHOW_BIG_TEXT` and explicit CallStyle refs for `SHOW_CALL` and
`SHOW_CALL_SCREENING`. When the action summary includes the newer columns, `SHOW_DELIVERY`
and `SHOW_METRICS` must expose Live Update subtext/header refs, every active action
must expose content-intent and delete-intent refs, and `SHOW_DUMP` also must expose
AOD remote-app refs, Samsung RemoteViews refs, visual dump refs, and Samsung dump
chronometer refs. Normal card actions must also expose AOD remote-app refs whenever
Samsung ongoing-activity extras are present. Timer, delivery, metric, and custom-control actions must expose
status-chip refs. `SHOW_NATIVE_STYLE` must also expose hidden style build report refs
when that column is present. `SHOW_DELIVERY` must expose action-button refs plus the
text-only/disabled `Tip` action refs when those columns are present. `SHOW_DELIVERY`,
`SHOW_METRICS`, `SHOW_BIG_TEXT`, and `SHOW_CUSTOM_ACTIONS` must expose action metadata
refs when that column is present. `SHOW_METRICS`, `SHOW_BIG_TEXT`, and
`SHOW_CUSTOM_ACTIONS` must expose action-button refs when the column is present. `SHOW_CUSTOM_ACTIONS`
must expose Unpin action refs, and `UNPIN` must remove enhanced-surface refs while keeping
the package-owned ongoing notification, so the demo covers the official Live Updates
demotion flow.

The per-action package slice intentionally keeps a wide context around `com.nowbar.demo`
matches because Android and One UI notification dumps can put template/extras lines far
from the package name within the same notification record.

Re-run artifact verification manually:

```bash
scripts/verify_real_samsung_smoke_artifacts.sh artifacts/real-samsung-smoke/<timestamp>
ALLOW_MISSING_SCREENSHOTS=1 scripts/verify_real_samsung_smoke_artifacts.sh artifacts/real-samsung-smoke/<timestamp>
REQUIRE_LOCKSCREEN_SCREENSHOTS=1 scripts/verify_real_samsung_smoke_artifacts.sh artifacts/real-samsung-smoke/<timestamp>
ALLOW_MISSING_SHADE_SCREENSHOTS=1 scripts/verify_real_samsung_smoke_artifacts.sh artifacts/real-samsung-smoke/<timestamp>
VERIFY_SCREENSHOT_FILES=0 scripts/verify_real_samsung_smoke_artifacts.sh artifacts/real-samsung-smoke/<timestamp>
```

Notification shade screenshots are captured by default as `screen-<ACTION>-shade.png`.
Disable them only for devices where `cmd statusbar expand-notifications` is blocked:

```bash
CAPTURE_NOTIFICATION_SHADE=0 scripts/real_samsung_smoke.sh
```

When shade capture is disabled, the smoke automatically relaxes only the shade-screenshot
assertion; normal app screenshots and `dumpsys` evidence are still required.

Optional lock-screen screenshots:

```bash
CAPTURE_LOCKSCREEN=1 scripts/real_samsung_smoke.sh
```

This sends sleep/wake key events after each action and stores
`screen-<ACTION>-lock.png`. Use it only on an unlocked local/Remote Test Lab device
where wake/sleep control is acceptable.

Run the full real-device evidence bundle:

```bash
scripts/run_real_samsung_e2e.sh
ADB_CONNECT=localhost:<port> WAIT_FOR_DEVICE_SECONDS=30 scripts/run_real_samsung_e2e.sh
CAPTURE_NOTIFICATION_SHADE=0 scripts/run_real_samsung_e2e.sh
CAPTURE_LOCKSCREEN=1 scripts/run_real_samsung_e2e.sh
REQUIRE_LOCKSCREEN_SCREENSHOTS=1 CAPTURE_LOCKSCREEN=1 scripts/run_real_samsung_e2e.sh
```

The E2E runner writes `artifacts/real-samsung-e2e/<timestamp>/summary.md` and combines:

- host ADB/tool snapshot;
- complete `real_samsung_smoke.sh` output;
- smoke artifact verification;
- Samsung APK pull/decompile from the same device;
- APK key coverage report.

Useful options:

```bash
RUN_APK_RESEARCH=0 scripts/run_real_samsung_e2e.sh
APK_RESEARCH_DECOMPILE=0 scripts/run_real_samsung_e2e.sh
CONTINUE_AFTER_SMOKE_FAILURE=1 scripts/run_real_samsung_e2e.sh
WAIT_FOR_DEVICE_SECONDS=30 scripts/run_real_samsung_e2e.sh
```

Run only the instrumentation smoke:

```bash
ANDROID_SERIAL=<serial> ./gradlew :demo:connectedDebugAndroidTest
```

Run one scenario manually through the exported activity trampoline:

```bash
adb shell am start -W -n com.nowbar.demo/.MainActivity -a com.nowbar.demo.SHOW_TIMER
adb shell am start -W -n com.nowbar.demo/.MainActivity -a com.nowbar.demo.SHOW_WORKOUT
adb shell am start -W -n com.nowbar.demo/.MainActivity -a com.nowbar.demo.SHOW_NAVIGATION
adb shell am start -W -n com.nowbar.demo/.MainActivity -a com.nowbar.demo.SHOW_DELIVERY
adb shell am start -W -n com.nowbar.demo/.MainActivity -a com.nowbar.demo.SHOW_METRICS
adb shell am start -W -n com.nowbar.demo/.MainActivity -a com.nowbar.demo.SHOW_NATIVE_STYLE
```

Inspect posted notification extras:

```bash
adb shell dumpsys notification --noredact | grep -A120 'com.nowbar.demo'
adb shell logcat -d | grep NowBarDemo
```

Pull and decompile Samsung APKs from the same device:

```bash
scripts/pull_decompile_samsung_apks.sh
ADB_CONNECT=localhost:<port> WAIT_FOR_DEVICE_SECONDS=30 scripts/pull_decompile_samsung_apks.sh
```

By default it pulls SystemUI, Settings, Launcher, Clock, Samsung Health, Voice Recorder,
Modes/Routines, Game Tools, AOD service, Google Maps, and Google App when installed.
It also auto-discovers up to 60 installed Samsung/SystemUI/AOD/launcher/health/routine
candidate packages unless `DISCOVER_SAMSUNG_PACKAGES=0` is set. Every base/split APK
path reported by `pm path` is pulled. The script captures `dumpsys package`, manifest
metadata through `apkanalyzer`, runs `apktool` and `jadx` per APK when available,
then writes a combined Now Bar scan to
`artifacts/samsung-apk-research/<timestamp>/nowbar-scan-all.txt`.

Expected constraints:

- One UI 7 Samsung Now Bar can still be package allow-list gated even when Samsung extras are correct.
- Android 16 / One UI 8 Live Updates require promoted ongoing eligibility and user/OEM settings.
- The demo exposes separate settings shortcuts for Samsung Now Bar app toggles and Android
  promoted notifications. SDK callers can use `NowBarDiagnostics.createNowBarSettingsIntent`
  for the documented Samsung Settings -> Lock screen and AOD -> Now bar path, or
  `LiveUpdateDiagnostics.createManageAppPromotedNotificationsIntent`, which matches the
  current Android Live Updates docs naming and uses the platform promotion-settings action.

## Local APK artifacts

If a Samsung APK is already available on disk, scan it without ADB:

```bash
scripts/scan_local_apks_for_nowbar.sh /path/to/samsung.apk
scripts/scan_local_apks_for_nowbar.sh /path/to/samsung.xapk
scripts/scan_local_apks_for_nowbar.sh /path/to/samsung.apks
scripts/scan_local_apks_for_nowbar.sh /path/to/apk-directory
```

The local scanner unpacks APK bundles, captures manifest metadata through `apkanalyzer`,
and uses the same Now Bar / Live Updates patterns as the device pull script, including
Samsung ongoing-activity extras, hidden `OngoingActivityStyle`, capsule/cover keys,
AOD RemoteViews keys, Android progress extras, PDE telemetry keys, promoted ongoing APIs,
semantic title annotations, delete intents, and large-icon usage. Set `DECOMPILE=0` for a fast raw APK scan. Each APK scan also
runs `scripts/check_nowbar_key_coverage.sh` and writes a nested `key-coverage/report.md`
that compares observed Now Bar-related keys against the SDK constants.

Run the coverage check directly against existing scan artifacts:

```bash
scripts/check_nowbar_key_coverage.sh artifacts/local-apk-nowbar-scan/current-samsung-extra-apps/nowbar-scan-all.txt
STRICT=1 scripts/check_nowbar_key_coverage.sh artifacts/local-apk-nowbar-scan/current-samsung-extra-apps/nowbar-scan-all.txt
```

`STRICT=1` exits non-zero when a scan contains a stable Samsung/Android Now Bar key that
is not represented by the SDK. Resource names such as `capsule_background` are ignored;
only contract-shaped keys such as `android.ongoingActivityNoti.*`, `android.ongoingActivity*`,
`android.progress*`, `android.progressSegments`, `android.progressPoints`,
`android.progressTrackerIcon`, `android.progressStartIcon`, `android.progressEndIcon`,
`android.styledByProgress`, `pde_*`, `isCapsule`, `capsule_layout`, `capsule_action`, `capsule_priority`,
`bg_startColor`, `bg_endColor`, promoted-notification permissions, and feature flags are compared.

When no Samsung device is connected, a public Samsung APK can still be used for static
evidence. Example with `apkeep`:

```bash
apkeep -a com.sec.android.app.shealth -d apk-pure artifacts/external-apks
scripts/scan_local_apks_for_nowbar.sh artifacts/external-apks/com.sec.android.app.shealth.apk
```

Samsung Voice Recorder is useful for the hidden `Notification.OngoingActivityStyle`
path and capsule keys:

```bash
apkeep -a com.sec.android.app.voicenote -d apk-pure artifacts/external-apks
OUT_DIR=artifacts/local-apk-nowbar-scan/current-samsung-voice \
  scripts/scan_local_apks_for_nowbar.sh artifacts/external-apks/com.sec.android.app.voicenote.apk
```

This does not replace real-device Now Bar rendering checks, but it does validate the
manifest strings, proprietary Samsung extras, and decompiled helper names used by the
SDK compatibility layer.
